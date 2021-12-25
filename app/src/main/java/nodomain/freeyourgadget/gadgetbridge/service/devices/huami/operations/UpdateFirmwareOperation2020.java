/*  Copyright (C) 2020-2021 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo.UIHH_HEADER;

public class UpdateFirmwareOperation2020 extends UpdateFirmwareOperation {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation2020.class);

    public UpdateFirmwareOperation2020(Uri uri, HuamiSupport support) {
        super(uri, support);
    }

    private final byte COMMAND_REQUEST_PARAMETERS = (byte) 0xd0;
    private final byte COMMAND_SEND_FIRMWARE_INFO = (byte) 0xd2;
    private final byte COMMAND_START_TRANSFER = (byte) 0xd3;
    private final byte REPLY_UPDATE_PROGRESS = (byte) 0xd4;
    private final byte COMMAND_COMPLETE_TRANSFER = (byte) 0xd5;
    private final byte COMMAND_FINALIZE_UPDATE = (byte) 0xd6;

    private int mChunkLength = -1;

    @Override
    protected void doPerform() throws IOException {
        firmwareInfo = createFwInfo(uri, getContext());
        if (!firmwareInfo.isGenerallyCompatibleWith(getDevice())) {
            throw new IOException("Firmware is not compatible with the given device: " + getDevice().getAddress());
        }

        if (!requestParameters()) {
            displayMessage(getContext(), "Error requesting parameters, aborting.", Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
    }


    @Override
    protected void handleNotificationNotif(byte[] value) {
        if (value.length != 3 && value.length != 6 && value.length != 11) {
            LOG.error("Notifications should be 3, 6  or 11 bytes long.");
            getSupport().logMessageContent(value);
            return;
        }
        boolean success = (value[2] == HuamiService.SUCCESS) || ((value[1] == REPLY_UPDATE_PROGRESS) && value.length == 6); // ugly

        if (value[0] == HuamiService.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case COMMAND_REQUEST_PARAMETERS: {
                        mChunkLength = (value[4] & 0xff) | ((value[5] & 0xff) << 8);
                        LOG.info("got chunk length of " + mChunkLength);
                        sendFwInfo();
                        break;
                    }
                    case COMMAND_SEND_FIRMWARE_INFO:
                        sendTransferStart();
                        break;
                    case COMMAND_START_TRANSFER:
                        sendFirmwareDataChunk(getFirmwareInfo(), 0);
                        break;
                    case HuamiService.COMMAND_FIRMWARE_START_DATA:
                        sendChecksum(getFirmwareInfo());
                        break;
                    case REPLY_UPDATE_PROGRESS:
                        int offset = (value[2] & 0xff) | ((value[3] & 0xff) << 8) | ((value[4] & 0xff) << 16) | ((value[5] & 0xff) << 24);
                        LOG.info("update progress " + offset + " bytes");
                        sendFirmwareDataChunk(getFirmwareInfo(), offset);
                        break;
                    case COMMAND_COMPLETE_TRANSFER:
                        sendFinalize();
                        break;
                    case COMMAND_FINALIZE_UPDATE: {
                        if (getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.FIRMWARE) {
                            TransactionBuilder builder = performInitialized("reboot");
                            getSupport().sendReboot(builder);
                            builder.queue(getQueue());
                        } else {
                            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                            done();
                        }
                        break;
                    }
                    case HuamiService.COMMAND_FIRMWARE_REBOOT: {
                        LOG.info("Reboot command successfully sent.");
                        GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                        done();
                        break;
                    }
                    default: {
                        LOG.error("Unexpected response during firmware update: ");
                        getSupport().logMessageContent(value);
                        operationFailed();
                        displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                        done();
                    }
                }
            } catch (Exception ex) {
                displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                done();
            }
        } else {
            LOG.error("Unexpected notification during firmware update: ");
            operationFailed();
            getSupport().logMessageContent(value);
            displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_metadata_updateproblem), Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
    }


    public boolean sendFwInfo() {
        try {
            TransactionBuilder builder = performInitialized("send firmware info");
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.updating_firmware), getContext()));
            int fwSize = getFirmwareInfo().getSize();
            byte[] sizeBytes = BLETypeConversions.fromUint32(fwSize);
            int crc32 = firmwareInfo.getCrc32();
            byte[] chunkSizeBytes = BLETypeConversions.fromUint16(mChunkLength);
            byte[] crcBytes = BLETypeConversions.fromUint32(crc32);
            byte[] bytes = new byte[]{
                    COMMAND_SEND_FIRMWARE_INFO,
                    getFirmwareInfo().getFirmwareType().getValue(),
                    sizeBytes[0],
                    sizeBytes[1],
                    sizeBytes[2],
                    sizeBytes[3],
                    crcBytes[0],
                    crcBytes[1],
                    crcBytes[2],
                    crcBytes[3],
                    chunkSizeBytes[0],
                    chunkSizeBytes[1],
                    0, // ??
                    0, // index
                    1, // count
                    sizeBytes[0], // total size? right now it is equal to the size above
                    sizeBytes[1],
                    sizeBytes[2],
                    sizeBytes[3]
            };

            if (getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.WATCHFACE) {
                byte[] fwBytes = firmwareInfo.getBytes();
                if (ArrayUtils.startsWith(fwBytes, UIHH_HEADER)) {
                    getSupport().writeToConfiguration(builder,
                            new byte[]{0x39, 0x00,
                                    sizeBytes[0],
                                    sizeBytes[1],
                                    sizeBytes[2],
                                    sizeBytes[3],
                                    fwBytes[18],
                                    fwBytes[19],
                                    fwBytes[20],
                                    fwBytes[21]
                            });
                }
            }
            builder.write(fwCControlChar, bytes);
            builder.queue(getQueue());
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }


    public boolean requestParameters() {
        try {
            TransactionBuilder builder = performInitialized("get update capabilities");
            byte[] bytes = new byte[]{COMMAND_REQUEST_PARAMETERS};
            builder.write(fwCControlChar, bytes);
            builder.queue(getQueue());
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }


    private boolean sendFirmwareDataChunk(HuamiFirmwareInfo info, int offset) {
        byte[] fwbytes = info.getBytes();
        int len = fwbytes.length;
        int remaining = len - offset;
        final int packetLength = getSupport().getMTU() - 3;

        int chunkLength = mChunkLength;
        if (remaining < mChunkLength) {
            chunkLength = remaining;
        }

        int packets = chunkLength / packetLength;
        int chunkProgress = 0;

        try {
            if (remaining <= 0) {
                sendTransferComplete();
                return true;
            }

            TransactionBuilder builder = performInitialized("send firmware packets");

            for (int i = 0; i < packets; i++) {
                byte[] fwChunk = Arrays.copyOfRange(fwbytes, offset + i * packetLength, offset + i * packetLength + packetLength);

                builder.write(fwCDataChar, fwChunk);
                chunkProgress += packetLength;
            }

            if (chunkProgress < chunkLength) {
                byte[] lastChunk = Arrays.copyOfRange(fwbytes, offset + packets * packetLength, offset + packets * packetLength + (chunkLength - chunkProgress));
                builder.write(fwCDataChar, lastChunk);
            }

            int progressPercent = (int) ((((float) (offset + chunkLength)) / len) * 100);

            builder.add(new SetProgressAction(getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, getContext()));

            builder.queue(getQueue());

        } catch (IOException ex) {
            LOG.error("Unable to send fw to device", ex);
            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_firmware_not_sent), false, 0, getContext());
            return false;
        }
        return true;
    }


    protected void sendTransferStart() throws IOException {
        TransactionBuilder builder = performInitialized("trasfer complete");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_START_TRANSFER, 1,
        });
        builder.queue(getQueue());
    }

    protected void sendTransferComplete() throws IOException {
        TransactionBuilder builder = performInitialized("trasfer complete");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_COMPLETE_TRANSFER,
        });
        builder.queue(getQueue());
    }

    protected void sendFinalize() throws IOException {
        TransactionBuilder builder = performInitialized("finalize firmware");
        builder.write(fwCControlChar, new byte[]{
                COMMAND_FINALIZE_UPDATE,
        });
        builder.queue(getQueue());
    }
}