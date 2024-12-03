/*  Copyright (C) 2023-2024 José Rebelo, Yoran Vulker

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiActivityFileFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiActivityFileFetcher.class);

    private final XiaomiHealthService mHealthService;

    private final Queue<XiaomiActivityFileId> mFetchQueue = new PriorityQueue<>();
    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();
    private boolean isFetching = false;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    public XiaomiActivityFileFetcher(final XiaomiHealthService healthService) {
        this.mHealthService = healthService;
    }

    public void dispose() {
        clearTimeout();
    }

    private void clearTimeout() {
        this.timeoutHandler.removeCallbacksAndMessages(null);
    }

    private void setTimeout() {
        // #4305 - Set the timeout in case the watch does not send the file
        this.timeoutHandler.postDelayed(() -> {
            LOG.warn("Timed out waiting for activity file with {} bytes in the buffer", mBuffer.size());
            triggerNextFetch();
        }, 5000L);
    }

    public void addChunk(final byte[] chunk) {
        clearTimeout();

        final int total = BLETypeConversions.toUint16(chunk, 0);
        final int num = BLETypeConversions.toUint16(chunk, 2);

        if (num == 1) {
            // reset buffer
            mBuffer = new ByteArrayOutputStream();
        }

        LOG.debug("Got activity chunk {}/{}", num, total);

        mBuffer.write(chunk, 4, chunk.length - 4);

        if (num != total) {
            setTimeout();
            return;
        }

        final byte[] data = mBuffer.toByteArray();
        mBuffer = new ByteArrayOutputStream();

        if (data.length < 13) {
            LOG.warn("Activity data length of {} is too short", data.length);
            // FIXME this may mess up the order.. maybe we should just abort
            triggerNextFetch();
            return;
        }

        final int arrCrc32 = CheckSums.getCRC32(data, 0, data.length - 4);
        final int expectedCrc32 = BLETypeConversions.toUint32(data, data.length - 4);

        if (arrCrc32 != expectedCrc32) {
            LOG.warn(
                    "Invalid activity data checksum: got {}, expected {}",
                    String.format("%08X", arrCrc32),
                    String.format("%08X", expectedCrc32)
            );
            // FIXME this may mess up the order.. maybe we should just abort
            triggerNextFetch();
            return;
        }

        if (data[7] != 0) {
            LOG.warn(
                    "Unexpected activity payload byte {} at position 7 - parsing might fail",
                    String.format("0x%02X", data[7])
            );
        }

        final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

        dumpBytesToExternalStorage(fileId, data);

        if (!XiaomiPreferences.keepActivityDataOnDevice(mHealthService.getSupport().getDevice())) {
            LOG.debug("Acking recorded data {}", fileId);
            // TODO is this too early?
            mHealthService.ackRecordedData(fileId);
        }

        final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
        if (activityParser == null) {
            LOG.warn("Failed to find parser for {}", fileId);
            triggerNextFetch();
            return;
        }

        try {
            if (activityParser.parse(mHealthService.getSupport(), fileId, data)) {
                LOG.info("Successfully parsed {}", fileId);
            } else {
                LOG.warn("Failed to parse {}", fileId);
            }
        } catch (final Exception ex) {
            LOG.error("Exception while parsing {}", fileId, ex);
        }

        triggerNextFetch();
    }

    public void fetch(final List<XiaomiActivityFileId> fileIds) {
        // #4305 - ensure unique files
        for (final XiaomiActivityFileId fileId : fileIds) {
            if (!mFetchQueue.contains(fileId)) {
                mFetchQueue.add(fileId);
            } else {
                LOG.warn("Ignoring duplicated file {}", fileId);
            }
        }

        if (!isFetching) {
            // Currently not fetching anything, fetch the next
            isFetching = true;
            final XiaomiSupport support = mHealthService.getSupport();
            final Context context = support.getContext();
            GB.updateTransferNotification(context.getString(R.string.busy_task_fetch_activity_data), "", true, 0, context);
            support.getDevice().setBusyTask(context.getString(R.string.busy_task_fetch_activity_data));
            support.getDevice().sendDeviceUpdateIntent(support.getContext());
            triggerNextFetch();
        }
    }

    private void triggerNextFetch() {
        clearTimeout();
        mBuffer = new ByteArrayOutputStream();

        final XiaomiActivityFileId fileId = mFetchQueue.poll();

        if (fileId == null) {
            LOG.debug("Nothing more to fetch");
            isFetching = false;
            mHealthService.getSupport().getDevice().unsetBusyTask();
            GB.signalActivityDataFinish(mHealthService.getSupport().getDevice());
            GB.updateTransferNotification(null, "", false, 100, mHealthService.getSupport().getContext());
            mHealthService.getSupport().getDevice().sendDeviceUpdateIntent(mHealthService.getSupport().getContext());
            return;
        }

        LOG.debug("Triggering next fetch for: {}", fileId);

        setTimeout();

        mHealthService.requestRecordedData(fileId);
    }

    protected void dumpBytesToExternalStorage(final XiaomiActivityFileId fileId, final byte[] bytes) {
        try {
            final GBDevice device = mHealthService.getSupport().getDevice();
            final File exportDirectory = device.getDeviceCoordinator().getWritableExportDirectory(device);
            final File targetDir = new File(exportDirectory, "rawFetchOperations");
            targetDir.mkdirs();

            final File outputFile = new File(targetDir, fileId.getFilename());

            final OutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(bytes);
            outputStream.close();
        } catch (final Exception e) {
            LOG.error("Failed to dump bytes to storage", e);
        }
    }
}
