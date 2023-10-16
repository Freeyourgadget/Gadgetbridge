/*  Copyright (C) 2023 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiHealthService;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiActivityFileFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiActivityFileFetcher.class);

    private final XiaomiHealthService mHealthService;

    private final Queue<List<XiaomiActivityFileId>> mFetchQueue = new LinkedList<>();
    private ByteArrayOutputStream mBuffer = null;
    private Set<XiaomiActivityFileId> pendingFiles = new HashSet<>();
    private boolean isFetching = false;

    public XiaomiActivityFileFetcher(final XiaomiHealthService healthService) {
        this.mHealthService = healthService;
    }

    public void addChunk(final byte[] chunk) {
        final int total = BLETypeConversions.toUint16(chunk, 0);
        final int num = BLETypeConversions.toUint16(chunk, 2);

        LOG.debug("Got activity chunk {}/{}", num, total);

        if (num == 1) {
            if (mBuffer == null) {
                mBuffer = new ByteArrayOutputStream();
            }
            mBuffer.reset();
            mBuffer.write(chunk, 4, chunk.length - 4);
        }

        if (num == total) {
            final byte[] data = mBuffer.toByteArray();
            mBuffer.reset();
            mBuffer = null;

            if (data.length < 13) {
                LOG.warn("Activity data length of {} is too short", data.length);
                // FIXME this may mess up the order.. maybe we should just abort
                triggerNextFetch();
                return;
            }

            if (!validChecksum(data)) {
                LOG.warn("Invalid activity data checksum");
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
            final byte[] activityData = Arrays.copyOfRange(data, 8, data.length - 4);
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

            final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
            if (activityParser == null) {
                LOG.warn("Failed to find activity parser for {}", fileId);
                triggerNextFetch();
                return;
            }

            if (activityParser.parse(fileId, activityData)) {
                LOG.debug("Acking recorded data {}", fileId);
                //mHealthService.ackRecordedData(fileId);
            }

            // FIXME only after receiving everything triggerNextFetch();
        }
    }

    public void fetch(final List<XiaomiActivityFileId> fileIds) {
        mFetchQueue.add(fileIds);
        if (!isFetching) {
            // Currently not fetching anything, fetch the next
            final XiaomiSupport support = mHealthService.getSupport();
            final Context context = support.getContext();
            GB.updateTransferNotification(context.getString(R.string.busy_task_fetch_activity_data),"", true, 0, context);
            support.getDevice().setBusyTask(context.getString(R.string.busy_task_fetch_activity_data));
            triggerNextFetch();
        }
    }

    private void triggerNextFetch() {
        final List<XiaomiActivityFileId> fileIds = mFetchQueue.poll();

        if (fileIds == null || fileIds.isEmpty()) {
            mHealthService.getSupport().getDevice().unsetBusyTask();
            GB.updateTransferNotification(null, "", false, 100, mHealthService.getSupport().getContext());
            return;
        }

        mHealthService.requestRecordedData(fileIds);
    }

    public boolean validChecksum(final byte[] arr) {
        final int arrCrc32 = CheckSums.getCRC32(arr, 0, arr.length - 4);
        final int expectedCrc32 = BLETypeConversions.toUint32(arr, arr.length - 4);

        return arrCrc32 == expectedCrc32;
    }
}
