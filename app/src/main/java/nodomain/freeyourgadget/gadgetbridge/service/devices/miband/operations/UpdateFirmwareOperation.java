package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class UpdateFirmwareOperation extends AbstractBTLEOperation<MiBandSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation.class);

    private final Uri uri;
    private boolean firmwareInfoSent = false;
    private byte[] newFirmware;
    private boolean rebootWhenBandReady = false;

    public UpdateFirmwareOperation(Uri uri, MiBandSupport support) {
        super(support);
        this.uri = uri;
    }

    @Override
    public void perform() throws IOException {
        MiBandFWHelper mFwHelper = new MiBandFWHelper(uri, getContext());
        String mMac = getDevice().getAddress();
        String[] mMacOctets = mMac.split(":");

        int newFwVersion = mFwHelper.getFirmwareVersion();
        int oldFwVersion = getSupport().getDeviceInfo().getFirmwareVersion();
        int checksum = (Integer.decode("0x" + mMacOctets[4]) << 8 | Integer.decode("0x" + mMacOctets[5])) ^ CheckSums.getCRC16(mFwHelper.getFw());

        sendFirmwareInfo(oldFwVersion, newFwVersion, mFwHelper.getFw().length, checksum);
        firmwareInfoSent = true;
        newFirmware = mFwHelper.getFw();
        //the firmware will be sent by the notification listener if the band confirms that the metadata are ok.
    }

    private void done() {
        getDevice().unsetBusyTask();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_NOTIFICATION.equals(characteristicUUID)) {
            handleNotificationNotif(characteristic.getValue());
        } else {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    /**
     * React to unsolicited messages sent by the Mi Band to the MiBandService.UUID_CHARACTERISTIC_NOTIFICATION
     * characteristic,
     * These messages appear to be always 1 byte long, with values that are listed in MiBandService.
     * It is not excluded that there are further values which are still unknown.
     * <p/>
     * Upon receiving known values that request further action by GB, the appropriate method is called.
     *
     * @param value
     */
    private void handleNotificationNotif(byte[] value) {
        if (value.length != 1) {
            LOG.error("Notifications should be 1 byte long.");
            getSupport().logMessageContent(value);
            return;
        }
        switch (value[0]) {
            case MiBandService.NOTIFY_FW_CHECK_SUCCESS:
                if (firmwareInfoSent && newFirmware != null) {
                    if (sendFirmwareData(newFirmware)) {
                        rebootWhenBandReady = true;
                    } else {
                        //TODO: the firmware transfer failed, but the miband should be still functional with the old firmware. What should we do?
                        GB.toast(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                        done();
                    }
                    firmwareInfoSent = false;
                    newFirmware = null;
                }
                break;
            case MiBandService.NOTIFY_FW_CHECK_FAILED:
                GB.toast(getContext().getString(R.string.updatefirmwareoperation_metadata_updateproblem), Toast.LENGTH_LONG, GB.ERROR);
                firmwareInfoSent = false;
                newFirmware = null;
                done();
                break;
            case MiBandService.NOTIFY_FIRMWARE_UPDATE_SUCCESS:
                if (rebootWhenBandReady) {
                    GB.toast(getContext(), getContext().getString(R.string.updatefirmwareoperation_update_complete_rebooting), Toast.LENGTH_LONG, GB.INFO);
                    GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                    getSupport().onReboot();
                    rebootWhenBandReady = false;
                }
                done();
                break;
            case MiBandService.NOTIFY_FIRMWARE_UPDATE_FAILED:
                //TODO: the firmware transfer failed, but the miband should be still functional with the old firmware. What should we do?
                GB.toast(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_write_failed), false, 0, getContext());
                rebootWhenBandReady = false;
                done();
                break;

            default:
                for (byte b : value) {
                    LOG.warn("DATA: " + String.format("0x%2x", b));
                }
        }
    }

    /**
     * Prepare the MiBand to receive the new firmware data.
     * Some information about the new firmware version have to be pushed to the MiBand before sending
     * the actual firmare.
     * <p/>
     * The Mi Band will send a notification after receiving these data to confirm if the metadata looks good to it.
     *
     * @param currentFwVersion
     * @param newFwVersion
     * @param newFwSize
     * @param checksum
     * @see MiBandSupport#handleNotificationNotif
     */
    private void sendFirmwareInfo(int currentFwVersion, int newFwVersion, int newFwSize, int checksum) throws IOException {
        byte[] fwInfo = new byte[]{
                MiBandService.COMMAND_SEND_FIRMWARE_INFO,
                (byte) currentFwVersion,
                (byte) (currentFwVersion >> 8),
                (byte) (currentFwVersion >> 16),
                (byte) (currentFwVersion >> 24),
                (byte) newFwVersion,
                (byte) (newFwVersion >> 8),
                (byte) (newFwVersion >> 16),
                (byte) (newFwVersion >> 24),
                (byte) newFwSize,
                (byte) (newFwSize >> 8),
                (byte) checksum,
                (byte) (checksum >> 8)
        };
        TransactionBuilder builder = performInitialized("send firmware info");
        builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.updating_firmware), getContext()));
        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fwInfo);
        builder.queue(getQueue());
    }

    /**
     * Method that uploads a firmware (fwbytes) to the MiBand.
     * The firmware has to be splitted into chunks of 20 bytes each, and periodically a COMMAND_SYNC comand has to be issued to the MiBand.
     * <p/>
     * The Mi Band will send a notification after receiving these data to confirm if the firmware looks good to it.
     *
     * @param fwbytes
     * @return whether the transfer succeeded or not. Only a BT layer exception will cause the transmission to fail.
     * @see MiBandSupport#handleNotificationNotif
     */
    private boolean sendFirmwareData(byte fwbytes[]) {
        int len = fwbytes.length;
        final int packetLength = 20;
        int packets = len / packetLength;
        byte fwChunk[] = new byte[packetLength];

        int firmwareProgress = 0;

        try {
            TransactionBuilder builder = performInitialized("send firmware packet");
            for (int i = 0; i < packets; i++) {
                fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);

                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_FIRMWARE_DATA), fwChunk);
                firmwareProgress += packetLength;

                if ((i > 0) && (i % 50 == 0)) {
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), new byte[]{MiBandService.COMMAND_SYNC});
                    builder.add(new SetProgressAction("Firmware update in progress", true, (int)(((float) firmwareProgress) / len * 100), getContext()));
                }

                LOG.info("Firmware update progress:" + firmwareProgress + " total len:" + len + " progress:" + (int)(((float) firmwareProgress) / len * 100));
            }

            if (!(len % packetLength == 0)) {
                byte lastChunk[] = new byte[len % packetLength];
                lastChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_FIRMWARE_DATA), lastChunk);
                firmwareProgress += len % packetLength;
            }

            LOG.info("Firmware update progress:" + firmwareProgress + " total len:" + len + " progress:" + (firmwareProgress / len));
            if (firmwareProgress >= len) {
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), new byte[]{MiBandService.COMMAND_SYNC});
            } else {
                GB.updateInstallNotification("Firmware write failed", false, 0, getContext());
            }

            builder.queue(getQueue());

        } catch (IOException ex) {
            LOG.error("Unable to send fw to MI", ex);
            GB.updateInstallNotification("Firmware write failed", false, 0, getContext());
            return false;
        }
        return true;
    }
}
