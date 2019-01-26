/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer

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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class UpdateFirmwareOperation extends AbstractHuamiOperation {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation.class);

    protected final Uri uri;
    protected final BluetoothGattCharacteristic fwCControlChar;
    protected final BluetoothGattCharacteristic fwCDataChar;
    protected final Prefs prefs = GBApplication.getPrefs();
    protected HuamiFirmwareInfo firmwareInfo;

    public UpdateFirmwareOperation(Uri uri, HuamiSupport support) {
        super(support);
        this.uri = uri;
        fwCControlChar = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE);
        fwCDataChar = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_DATA);
    }

    @Override
    protected void enableNeededNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(fwCControlChar, enable);
    }

    @Override
    protected void doPerform() throws IOException {
        firmwareInfo = createFwInfo(uri, getContext());
        if (!firmwareInfo.isGenerallyCompatibleWith(getDevice())) {
            throw new IOException("Firmware is not compatible with the given device: " + getDevice().getAddress());
        }

        if (!sendFwInfo()) {
            displayMessage(getContext(), "Error sending firmware info, aborting.", Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
        //the firmware will be sent by the notification listener if the band confirms that the metadata are ok.
    }

    protected HuamiFirmwareInfo createFwInfo(Uri uri, Context context) throws IOException {
        HuamiFWHelper fwHelper = getSupport().createFWHelper(uri, context);
        return fwHelper.getFirmwareInfo();
    }

    protected void done() {
        LOG.info("Operation done.");
        operationFinished();
        unsetBusy();
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            operationFailed();
        }
        return super.onCharacteristicWrite(gatt, characteristic, status);
    }

    private void operationFailed() {
        GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_write_failed), false, 0, getContext());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (fwCControlChar.getUuid().equals(characteristicUUID)) {
            handleNotificationNotif(characteristic.getValue());
            return true; // don't let anyone else handle it
        } else {
            super.onCharacteristicChanged(gatt, characteristic);
        }
        return false;
    }

    /**
     * React to messages sent by the Mi Band to the MiBandService.UUID_CHARACTERISTIC_NOTIFICATION
     * characteristic,
     * These messages appear to be always 1 byte long, with values that are listed in MiBandService.
     * It is not excluded that there are further values which are still unknown.
     * <p/>
     * Upon receiving known values that request further action by GB, the appropriate method is called.
     *
     * @param value
     */
    private void handleNotificationNotif(byte[] value) {
        if (value.length != 3) {
            LOG.error("Notifications should be 3 bytes long.");
            getSupport().logMessageContent(value);
            return;
        }
        boolean success = value[2] == HuamiService.SUCCESS;

        if (value[0] == HuamiService.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case HuamiService.COMMAND_FIRMWARE_INIT: {
                        sendFirmwareData(getFirmwareInfo());
                        break;
                    }
                    case HuamiService.COMMAND_FIRMWARE_START_DATA: {
                        sendChecksum(getFirmwareInfo());
                        break;
                    }
                    case HuamiService.COMMAND_FIRMWARE_CHECKSUM: {
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
//                    getSupport().onReboot();
                        done();
                        break;
                    }
                    default: {
                        LOG.error("Unexpected response during firmware update: ");
                        getSupport().logMessageContent(value);
                        operationFailed();
                        displayMessage(getContext(), getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                        done();
                        return;
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
    protected void displayMessage(Context context, String message, int duration, int severity) {
        getSupport().handleGBDeviceEvent(new GBDeviceEventDisplayMessage(message, duration, severity));
    }

    public boolean sendFwInfo() {
        try {
            TransactionBuilder builder = performInitialized("send firmware info");
//                getSupport().setLowLatency(builder);
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.updating_firmware), getContext()));
            int fwSize = getFirmwareInfo().getSize();
            byte[] sizeBytes = BLETypeConversions.fromUint24(fwSize);
            int arraySize = 4;
            boolean isFirmwareCode = getFirmwareInfo().getFirmwareType() == HuamiFirmwareType.FIRMWARE;
            if (!isFirmwareCode) {
                arraySize++;
            }
            byte[] bytes = new byte[arraySize];
            int i = 0;
            bytes[i++] = HuamiService.COMMAND_FIRMWARE_INIT;
            bytes[i++] = sizeBytes[0];
            bytes[i++] = sizeBytes[1];
            bytes[i++] = sizeBytes[2];
            if (!isFirmwareCode) {
                bytes[i++] = getFirmwareInfo().getFirmwareType().getValue();
            }

            builder.write(fwCControlChar, bytes);
            builder.queue(getQueue());
            return true;
        } catch (IOException e) {
            LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
            return false;
        }
    }

    /**
     * Method that uploads a firmware (fwbytes) to the Mi Band.
     * The firmware has to be split into chunks of 20 bytes each, and periodically a COMMAND_SYNC command has to be issued to the Mi Band.
     * <p/>
     * The Mi Band will send a notification after receiving this data to confirm if the firmware looks good to it.
     *
     * @param info
     * @return whether the transfer succeeded or not. Only a BT layer exception will cause the transmission to fail.
     * @see #handleNotificationNotif
     */
    private boolean sendFirmwareData(HuamiFirmwareInfo info) {
        byte[] fwbytes = info.getBytes();
        int len = fwbytes.length;
        final int packetLength = 20;
        int packets = len / packetLength;

        try {
            // going from 0 to len
            int firmwareProgress = 0;

            TransactionBuilder builder = performInitialized("send firmware packet");
            if (prefs.getBoolean("mi_low_latency_fw_update", true)) {
                getSupport().setLowLatency(builder);
            }
            builder.write(fwCControlChar, new byte[] { HuamiService.COMMAND_FIRMWARE_START_DATA });

            for (int i = 0; i < packets; i++) {
                byte[] fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);

                builder.write(fwCDataChar, fwChunk);
                firmwareProgress += packetLength;

                int progressPercent = (int) ((((float) firmwareProgress) / len) * 100);
                if ((i > 0) && (i % 100 == 0)) {
                    builder.write(fwCControlChar, new byte[]{HuamiService.COMMAND_FIRMWARE_UPDATE_SYNC});
                    builder.add(new SetProgressAction(getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, getContext()));
                }
            }

            if (firmwareProgress < len) {
                byte[] lastChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
                builder.write(fwCDataChar, lastChunk);
                firmwareProgress = len;
            }

            builder.write(fwCControlChar, new byte[]{HuamiService.COMMAND_FIRMWARE_UPDATE_SYNC});
            builder.queue(getQueue());

        } catch (IOException ex) {
            LOG.error("Unable to send fw to MI 2", ex);
            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_firmware_not_sent), false, 0, getContext());
            return false;
        }
        return true;
    }


    private void sendChecksum(HuamiFirmwareInfo firmwareInfo) throws IOException {
        TransactionBuilder builder = performInitialized("send firmware checksum");
        int crc16 = firmwareInfo.getCrc16();
        byte[] bytes = BLETypeConversions.fromUint16(crc16);
        builder.write(fwCControlChar, new byte[] {
                HuamiService.COMMAND_FIRMWARE_CHECKSUM,
                bytes[0],
                bytes[1],
        });
        builder.queue(getQueue());
    }

    private HuamiFirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }
}
