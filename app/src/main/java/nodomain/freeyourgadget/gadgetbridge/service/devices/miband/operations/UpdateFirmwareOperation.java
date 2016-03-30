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
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.PlainAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.AbstractMiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class UpdateFirmwareOperation extends AbstractMiBandOperation {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFirmwareOperation.class);

    private final Uri uri;
    private boolean firmwareInfoSent = false;
    private UpdateCoordinator updateCoordinator;

    public UpdateFirmwareOperation(Uri uri, MiBandSupport support) {
        super(support);
        this.uri = uri;
    }

    @Override
    protected void doPerform() throws IOException {
        MiBandFWHelper mFwHelper = new MiBandFWHelper(uri, getContext());

        AbstractMiFirmwareInfo firmwareInfo = mFwHelper.getFirmwareInfo();
        if (!firmwareInfo.isGenerallyCompatibleWith(getDevice())) {
            throw new IOException("Firmware is not compatible with the given device: " + getDevice().getAddress());
        }

        if (getSupport().supportsHeartRate()) {
            updateCoordinator = prepareFirmwareInfo1S(firmwareInfo);
        } else {
            updateCoordinator = prepareFirmwareInfo(mFwHelper.getFw(), mFwHelper.getFirmwareVersion());
        }

        updateCoordinator.initNextOperation();
        if (!updateCoordinator.sendFwInfo()) {
            GB.toast(getContext(), "Error sending firmware info, aborting.", Toast.LENGTH_LONG, GB.ERROR);
            done();
        }
        //the firmware will be sent by the notification listener if the band confirms that the metadata are ok.
    }

    private void done() {
        LOG.info("Operation done.");
        updateCoordinator = null;
        operationFinished();
        unsetBusy();
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
        if (value.length != 1) {
            LOG.error("Notifications should be 1 byte long.");
            getSupport().logMessageContent(value);
            return;
        }
        if (updateCoordinator == null) {
            LOG.error("received notification when updateCoordinator is null, ignoring!");
            return;
        }

        switch (value[0]) {
            case MiBandService.NOTIFY_FW_CHECK_SUCCESS:
                if (firmwareInfoSent) {
                    GB.toast(getContext(), "Firmware metadata successfully sent.", Toast.LENGTH_LONG, GB.INFO);
                    if (!updateCoordinator.sendFwData()) {
                        GB.toast(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                        done();
                    }
                    firmwareInfoSent = false;
                } else {
                    LOG.warn("firmwareInfoSent is false -- not sending firmware data even though we got meta data success notification");
                }
                break;
            case MiBandService.NOTIFY_FW_CHECK_FAILED:
                GB.toast(getContext().getString(R.string.updatefirmwareoperation_metadata_updateproblem), Toast.LENGTH_LONG, GB.ERROR);
                firmwareInfoSent = false;
                done();
                break;
            case MiBandService.NOTIFY_FIRMWARE_UPDATE_SUCCESS:
                if (updateCoordinator.initNextOperation()) {
                    GB.toast(getContext(), "Heart Rate Firmware successfully updated, now updating Mi Band Firmware", Toast.LENGTH_LONG, GB.INFO);
                    if (!updateCoordinator.sendFwInfo()) {
                        GB.toast(getContext(), "Error sending firmware info, aborting.", Toast.LENGTH_LONG, GB.ERROR);
                        done();
                    }
                    break;
                } else if (updateCoordinator.needsReboot()) {
                    GB.toast(getContext(), getContext().getString(R.string.updatefirmwareoperation_update_complete_rebooting), Toast.LENGTH_LONG, GB.INFO);
                    GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_update_complete), false, 100, getContext());
                    getSupport().onReboot();
                } else {
                    LOG.error("BUG: Successful firmware update without reboot???");
                }
                done();
                break;
            case MiBandService.NOTIFY_FIRMWARE_UPDATE_FAILED:
                //TODO: the firmware transfer failed, but the miband should be still functional with the old firmware. What should we do?
                GB.toast(getContext().getString(R.string.updatefirmwareoperation_updateproblem_do_not_reboot), Toast.LENGTH_LONG, GB.ERROR);
                GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_write_failed), false, 0, getContext());
                done();
                break;

            default:
                getSupport().logMessageContent(value);
                break;
        }
    }

    /**
     * Prepare the MiBand to receive the new firmware data.
     * Some information about the new firmware version have to be pushed to the MiBand before sending
     * the actual firmare.
     * <p/>
     * The Mi Band will send a notification after receiving these data to confirm if the metadata looks good to it.
     *
     * @param newFwVersion
     * @see MiBandSupport#handleNotificationNotif
     */
    private UpdateCoordinator prepareFirmwareInfo(byte[] fwBytes, int newFwVersion) throws IOException {
        int newFwSize = fwBytes.length;
        String mMac = getDevice().getAddress();
        String[] mMacOctets = mMac.split(":");
        int currentFwVersion = getSupport().getDeviceInfo().getFirmwareVersion();
        int checksum = (Integer.decode("0x" + mMacOctets[4]) << 8 | Integer.decode("0x" + mMacOctets[5])) ^ CheckSums.getCRC16(fwBytes);

        byte[] fwInfo = prepareFirmwareUpdateA(currentFwVersion, newFwVersion, newFwSize, checksum);
        return new SingleUpdateCoordinator(fwInfo, fwBytes, true);
    }

    private UpdateCoordinator prepareFirmwareInfo1S(AbstractMiFirmwareInfo info) {
        if (info.isSingleMiBandFirmware()) {
            throw new IllegalArgumentException("preparing single fw not allowed for 1S");
        }
        int fw2Version = info.getSecond().getFirmwareVersion();
        int fw1Version = info.getFirst().getFirmwareVersion();

        String[] mMacOctets = getDevice().getAddress().split(":");
        int encodedMac = (Integer.decode("0x" + mMacOctets[4]) << 8 | Integer.decode("0x" + mMacOctets[5]));

        byte[] fw2Bytes = info.getSecond().getFirmwareBytes();
        int fw2Checksum = CheckSums.getCRC16(fw2Bytes) ^ encodedMac;

        byte[] fw1Bytes = info.getFirst().getFirmwareBytes();
        int fw1Checksum = encodedMac ^ CheckSums.getCRC16(fw1Bytes);

        // check firmware validity?

        int fw1OldVersion = getSupport().getDeviceInfo().getFirmwareVersion();
        int fw2OldVersion = getSupport().getDeviceInfo().getHeartrateFirmwareVersion();

        boolean rebootWhenFinished = true;
        if (info.isSingleMiBandFirmware()) {
            LOG.info("is single Mi Band firmware");
            byte[] fw1Info = prepareFirmwareInfo(fw1Bytes, fw1OldVersion, fw1Version, fw1Checksum, 0, rebootWhenFinished /*, progress monitor */);
            return new SingleUpdateCoordinator(fw1Info, fw1Bytes, rebootWhenFinished);
        } else {
            LOG.info("is multi Mi Band firmware, sending fw2 (hr) first");
            byte[] fw2Info = prepareFirmwareInfo(fw2Bytes, fw2OldVersion, fw2Version, fw2Checksum, 1, rebootWhenFinished /*, progress monitor */);
            byte[] fw1Info = prepareFirmwareInfo(fw1Bytes, fw1OldVersion, fw1Version, fw1Checksum, 0, rebootWhenFinished /*, progress monitor */);
            return new DoubleUpdateCoordinator(fw1Info, fw1Bytes, fw2Info, fw2Bytes, rebootWhenFinished);
        }
    }

    private byte[] prepareFirmwareInfo(byte[] fwBytes, int currentFwVersion, int newFwVersion, int checksum, int something, boolean reboot) {
        byte[] fwInfo;
        switch (something) {
            case -1:
                fwInfo = prepareFirmwareUpdateA(currentFwVersion, newFwVersion, fwBytes.length, checksum);
                break;
            case -2:
                fwInfo = prepareFirmwareUpdateB(currentFwVersion, newFwVersion, fwBytes.length, checksum, 0);
                break;
            default:
                fwInfo = prepareFirmwareUpdateB(currentFwVersion, newFwVersion, fwBytes.length, checksum, something);
        }
        return fwInfo;
    }

    private byte[] prepareFirmwareUpdateA(int currentFwVersion, int newFwVersion, int newFwSize, int checksum) {
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
        return fwInfo;
    }

    private byte[] prepareFirmwareUpdateB(int currentFwVersion, int newFwVersion, int newFwSize, int checksum, int something) {
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
                (byte) (checksum >> 8),
                (byte) something
        };
        return fwInfo;
    }


    /**
     * Method that uploads a firmware (fwbytes) to the Mi Band.
     * The firmware has to be split into chunks of 20 bytes each, and periodically a COMMAND_SYNC command has to be issued to the Mi Band.
     * <p/>
     * The Mi Band will send a notification after receiving this data to confirm if the firmware looks good to it.
     *
     * @param fwbytes
     * @return whether the transfer succeeded or not. Only a BT layer exception will cause the transmission to fail.
     * @see MiBandSupport#handleNotificationNotif
     */
    private boolean sendFirmwareData(byte[] fwbytes) {
        int len = fwbytes.length;
        final int packetLength = 20;
        int packets = len / packetLength;

        // going from 0 to len
        int firmwareProgress = 0;

        try {
            TransactionBuilder builder = performInitialized("send firmware packet");
            getSupport().setLowLatency(builder);
            for (int i = 0; i < packets; i++) {
                byte[] fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);

                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_FIRMWARE_DATA), fwChunk);
                firmwareProgress += packetLength;

                int progressPercent = (int) (((float) firmwareProgress) / len) * 100;
                if ((i > 0) && (i % 50 == 0)) {
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), new byte[]{MiBandService.COMMAND_SYNC});
                    builder.add(new SetProgressAction(getContext().getString(R.string.updatefirmwareoperation_update_in_progress), true, progressPercent, getContext()));
                }
            }

            if (firmwareProgress < len) {
                byte[] lastChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_FIRMWARE_DATA), lastChunk);
                firmwareProgress = len;
            }

            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), new byte[]{MiBandService.COMMAND_SYNC});
            builder.queue(getQueue());

        } catch (IOException ex) {
            LOG.error("Unable to send fw to MI", ex);
            GB.updateInstallNotification(getContext().getString(R.string.updatefirmwareoperation_firmware_not_sent), false, 0, getContext());
            return false;
        }
        return true;
    }

    private abstract class UpdateCoordinator {
        private final boolean reboot;

        public UpdateCoordinator(boolean needsReboot) {
            this.reboot = needsReboot;
        }

        abstract byte[] getFirmwareInfo();

        public abstract byte[] getFirmwareBytes();

        public abstract boolean initNextOperation();

        public boolean sendFwInfo() {
            try {
                TransactionBuilder builder = performInitialized("send firmware info");
                getSupport().setLowLatency(builder);
                builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.updating_firmware), getContext()));
                builder.add(new FirmwareInfoSentAction()); // Note: *before* actually sending the info, otherwise it's too late!
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), getFirmwareInfo());
                builder.queue(getQueue());
                return true;
            } catch (IOException e) {
                LOG.error("Error sending firmware info: " + e.getLocalizedMessage(), e);
                return false;
            }
        }

        public boolean sendFwData() {
//            if (true) {
//            return true; // FIXME: temporarily disabled firmware sending
//            }
            return sendFirmwareData(getFirmwareBytes());
        }

        public boolean needsReboot() {
            return reboot;
        }
    }

    private class SingleUpdateCoordinator extends UpdateCoordinator {

        private final byte[] fwInfo;
        private final byte[] fwData;

        public SingleUpdateCoordinator(byte[] fwInfo, byte[] fwData, boolean reboot) {
            super(reboot);
            this.fwInfo = fwInfo;
            this.fwData = fwData;
        }

        @Override
        public byte[] getFirmwareInfo() {
            return fwInfo;
        }

        @Override
        public byte[] getFirmwareBytes() {
            return fwData;
        }

        @Override
        public boolean initNextOperation() {
            return false;
        }
    }

    enum State {
        INITIAL,
        SEND_FW2,
        SEND_FW1,
        FINISHED,
        UNKNOWN
    }

    private class DoubleUpdateCoordinator extends UpdateCoordinator {

        private final byte[] fw1Info;
        private final byte[] fw1Data;

        private final byte[] fw2Info;
        private final byte[] fw2Data;

        private byte[] currentFwInfo;
        private byte[] currentFwData;

        private State state = State.INITIAL;

        public DoubleUpdateCoordinator(byte[] fw1Info, byte[] fw1Data, byte[] fw2Info, byte[] fw2Data, boolean reboot) {
            super(reboot);
            this.fw1Info = fw1Info;
            this.fw1Data = fw1Data;
            this.fw2Info = fw2Info;
            this.fw2Data = fw2Data;

            // start with fw2 (heart rate)
            currentFwInfo = fw2Info;
            currentFwData = fw2Data;
        }

        @Override
        public byte[] getFirmwareInfo() {
            return currentFwInfo;
        }

        @Override
        public byte[] getFirmwareBytes() {
            return currentFwData;
        }

        @Override
        public boolean initNextOperation() {
            switch (state) {
                case INITIAL:
                    currentFwInfo = fw2Info;
                    currentFwData = fw2Data;
                    state = State.SEND_FW2;
                    return true;
                case SEND_FW2:
                    currentFwInfo = fw1Info;
                    currentFwData = fw1Data;
                    state = State.SEND_FW1;
                    return fw1Info != null && fw1Data != null;
                case SEND_FW1:
                    currentFwInfo = null;
                    currentFwData = null;
                    state = State.FINISHED;
                    return false; // we're done
                default:
                    state = State.UNKNOWN;
            }
            return false;
        }
    }

    private class FirmwareInfoSentAction extends PlainAction {
        @Override
        public boolean run(BluetoothGatt gatt) {
            if (isOperationRunning()) {
                firmwareInfoSent = true;
            }
            return true;
        }
    }
}
