/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.net.Uri;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;

public class CmfDataUploader implements CmfCharacteristic.Handler {
    private static final Logger LOG = LoggerFactory.getLogger(CmfWatchProSupport.class);

    private final CmfWatchProSupport mSupport;

    private CmfFwHelper fwHelper;

    public CmfDataUploader(final CmfWatchProSupport support) {
        this.mSupport = support;
    }

    @Override
    public void onCommand(final CmfCommand cmd, final byte[] payload) {
        switch (cmd) {
            case DATA_TRANSFER_WATCHFACE_INIT_1_REPLY:
                if (payload[0] != 0x01) {
                    LOG.warn("Got unexpected transfer init 1 reply {}", payload[0]);
                    fwHelper = null;
                    return;
                }

                final ByteBuffer buf = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN);
                buf.put((byte) (0xa5));
                buf.putInt(fwHelper.getBytes().length);
                buf.putInt(new Random().nextInt()); // FIXME watchface ID?

                mSupport.sendData(
                        "transfer watchface init 2 request",
                        CmfCommand.DATA_TRANSFER_WATCHFACE_INIT_2_REQUEST,
                        buf.array()
                );
                return;
            case DATA_TRANSFER_AGPS_INIT_REPLY:
            case DATA_TRANSFER_WATCHFACE_INIT_2_REPLY:
                if (payload[0] != 0x01) {
                    LOG.warn("Got unexpected transfer 2 init reply {}", payload[0]);
                    fwHelper = null;
                    return;
                }

                setDeviceBusy();
                updateProgress(0, true);

                return;
            case DATA_TRANSFER_WATCHFACE_FINISH_ACK_1:
                handleAck1(CmfCommand.DATA_TRANSFER_WATCHFACE_FINISH_ACK_2, payload);
                return;
            case DATA_TRANSFER_AGPS_FINISH_ACK_1:
                handleAck1(CmfCommand.DATA_TRANSFER_AGPS_FINISH_ACK_2, payload);
                return;
            case DATA_CHUNK_REQUEST_AGPS:
                if (fwHelper == null || !fwHelper.isAgps()) {
                    LOG.warn("We are not sending AGPS - refusing request");
                    return;
                }
                handleChunkRequest(CmfCommand.DATA_CHUNK_REQUEST_AGPS, payload);
                return;
            case DATA_CHUNK_REQUEST_WATCHFACE:
                if (fwHelper == null || !fwHelper.isWatchface()) {
                    LOG.warn("We are not sending a watchface - refusing request");
                    return;
                }
                handleChunkRequest(CmfCommand.DATA_CHUNK_WRITE_WATCHFACE, payload);
                return;
        }

        LOG.warn("Got unknown data command {}", cmd);
    }

    public void onInstallApp(final Uri uri) {
        if (fwHelper != null) {
            LOG.warn("Already installing {}", fwHelper.getUri());
            return;
        }

        fwHelper = new CmfFwHelper(uri, mSupport.getContext());
        if (!fwHelper.isValid()) {
            LOG.warn("Uri {} is not valid", uri);
            fwHelper = null;
            return;
        }

        if (fwHelper.isWatchface()) {
            mSupport.sendData(
                    "transfer watchface init request",
                    CmfCommand.DATA_TRANSFER_WATCHFACE_INIT_1_REQUEST,
                    (byte) 0xa5
            );

            return;
        }

        LOG.warn("Unsupported fwHelper for {}", fwHelper.getUri());
        fwHelper = null;
    }

    private void handleChunkRequest(final CmfCommand commandReply, final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        final int offset = buf.getInt();
        final int length = buf.getInt();
        final int progress = buf.get();

        LOG.debug("Got chunk request: offset={}, length={}, progress={}", offset, length, progress);

        final TransactionBuilder builder = mSupport.createTransactionBuilder("send chunk offset " + offset);
        updateProgress(builder, progress, true);
        mSupport.sendData(
                "transfer watchface init request",
                commandReply,
                ArrayUtils.subarray(fwHelper.getBytes(), offset, offset + length)
        );
    }

    private void handleAck1(final CmfCommand commandReply, final byte[] payload) {
        if (payload[0] != 0x01) {
            LOG.warn("Got unexpected transfer finish reply {}", payload[0]);
            fwHelper = null;
        }

        LOG.debug("Got transfer finish ack 1");

        unsetDeviceBusy();
        updateProgress(100, false);
        mSupport.sendData("transfer finish", commandReply, (byte) 0xa5);
}

    private void updateProgress(final int progressPercent, boolean ongoing) {
        final TransactionBuilder builder = mSupport.createTransactionBuilder("update data upload progress to " + progressPercent);
        updateProgress(builder, progressPercent, ongoing);
        builder.queue(mSupport.getQueue());
    }

    private void updateProgress(final TransactionBuilder builder, final int progressPercent, boolean ongoing) {
        final int uploadMessage;
        if (fwHelper != null && fwHelper.isWatchface()) {
            uploadMessage = R.string.uploading_watchface;
        } else {
            uploadMessage = R.string.updating_firmware;
        }

        builder.add(new SetProgressAction(
                mSupport.getContext().getString(uploadMessage),
                ongoing,
                progressPercent,
                mSupport.getContext()
        ));
    }

    private void setDeviceBusy() {
        final GBDevice device = mSupport.getDevice();
        device.setBusyTask(mSupport.getContext().getString(R.string.updating_firmware));
        device.sendDeviceUpdateIntent(mSupport.getContext());
    }

    private void unsetDeviceBusy() {
        final GBDevice device = mSupport.getDevice();
        if (device != null && device.isConnected()) {
            if (device.isBusy()) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(mSupport.getContext());
            }
            device.sendDeviceUpdateIntent(mSupport.getContext());
        }
    }
}
