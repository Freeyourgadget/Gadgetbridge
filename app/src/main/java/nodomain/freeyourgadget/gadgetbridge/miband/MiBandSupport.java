package nodomain.freeyourgadget.gadgetbridge.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.btle.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_GENERIC;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_K9MAIL;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.ORIGIN_SMS;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.getNotificationPrefIntValue;

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBandSupport.class);
    private static final Logger ACTIVITYLOG = LoggerFactory.getLogger("activity");

    //temporary buffer, size is 60 because we want to store complete minutes (1 minute = 3 bytes)
    private static final int activityDataHolderSize = 60;
    private byte[] activityDataHolder = new byte[activityDataHolderSize];
    //index of the buffer above
    private int activityDataHolderProgress = 0;
    //number of bytes we will get in a single data transfer, used as counter
    private int activityDataRemainingBytes = 0;
    //same as above, but remains untouched for the ack message
    private int activityDataUntilNextHeader = 0;
    //timestamp of the single data transfer, incremented to store each minute's data
    private GregorianCalendar activityDataTimestampProgress = null;
    //same as above, but remains untouched for the ack message
    private GregorianCalendar activityDataTimestampToAck = null;


    public MiBandSupport() {
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZING, getContext()));
        pair(builder)
                .sendUserInfo(builder)
                .enableNotifications(builder, true)
                .setCurrentTime(builder)
                .requestBatteryInfo(builder)
                .setInitialized(builder);

        return builder;
    }

    /**
     * Last action of initialization sequence. Sets the device to initialized.
     * It is only invoked if all other actions were successfully run, so the device
     * must be initialized, then.
     * @param builder
     */
    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZED, getContext()));
    }

    // TODO: tear down the notifications on quit
    private MiBandSupport enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_NOTIFICATION), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);

        return this;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void pair() {
        for (int i = 0; i < 5; i++) {
            if (connect()) {
                return;
            }
        }
    }

    private byte[] getDefaultNotification() {
        final int vibrateTimes = 1;
        final long vibrateDuration = 250l;
        final int flashTimes = 1;
        final int flashColour = 0xFFFFFFFF;
        final int originalColour = 0xFFFFFFFF;
        final long flashDuration = 250l;

        return getNotification(vibrateDuration, vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);
    }

    private void sendDefaultNotification(TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        LOG.info("Sending notification to MiBand: " + characteristic);
        builder.write(characteristic, getDefaultNotification()).queue(getQueue());
    }

    private void sendCustomNotification(int vibrateDuration, int vibrateTimes, int pause, int flashTimes, int flashColour, int originalColour, long flashDuration, TransactionBuilder builder) {
        BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        int vDuration = Math.min(500, vibrateDuration); // longer than 500ms is not possible
        for (int i = 0; i < vibrateTimes; i++) {
            builder.write(controlPoint, startVibrate);
            builder.wait(vDuration);
            builder.write(controlPoint, stopVibrate);
            if (pause > 0) {
                builder.wait(pause);
            }
        }

        LOG.info("Sending notification to MiBand: " + controlPoint);
        builder.queue(getQueue());
    }

    private static final byte[] startVibrate = new byte[] { MiBandService.COMMAND_SEND_NOTIFICATION, 1 };
    private static final byte[] stopVibrate = new byte[] { MiBandService.COMMAND_STOP_MOTOR_VIBRATE };
    private static final byte[] reboot = new byte[]{ MiBandService.COMMAND_REBOOT };
    private static final byte[] fetch = new byte[]{ MiBandService.COMMAND_FETCH_DATA };

    private byte[] getNotification(long vibrateDuration, int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration) {
        byte[] vibrate = new byte[]{MiBandService.COMMAND_SEND_NOTIFICATION, (byte) 1};
        byte r = 6;
        byte g = 0;
        byte b = 6;
        boolean display = true;
        //      byte[] flashColor = new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 };
        return vibrate;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBandSupport sendUserInfo(TransactionBuilder builder) {
        LOG.debug("Writing User Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_USER_INFO);
        builder.write(characteristic, MiBandCoordinator.getAnyUserInfo(getDevice().getAddress()).getData());
        return this;
    }

    private MiBandSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY);
        builder.read(characteristic);
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */
    private MiBandSupport pair(TransactionBuilder transaction) {
        LOG.info("Attempting to pair MI device...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_PAIR);
        if (characteristic != null) {
            transaction.write(characteristic, new byte[]{2});
        } else {
            LOG.info("Unable to pair MI device -- characteristic not available");
        }
        return this;
    }

    private void performDefaultNotification(String task) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder);
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

//    private void performCustomNotification(String task, int vibrateDuration, int vibrateTimes, int pause, int flashTimes, int flashColour, int originalColour, long flashDuration) {
//        try {
//            TransactionBuilder builder = performInitialized(task);
//            sendCustomNotification(vibrateDuration, vibrateTimes, pause, flashTimes, flashColour, originalColour, flashDuration, builder);
//        } catch (IOException ex) {
//            LOG.error("Unable to send notification to MI device", ex);
//        }
//    }

    private void performPreferredNotification(String task, String notificationOrigin) {
        try {
            TransactionBuilder builder = performInitialized(task);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int vibrateDuration = getPreferredVibrateDuration(notificationOrigin, prefs);
            int vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            int vibratePause = getPreferredVibratePause(notificationOrigin, prefs);

            int flashTimes = getPreferredFlashCount(notificationOrigin, prefs);
            int flashColour = getPreferredFlashColour(notificationOrigin, prefs);
            int originalColour = getPreferredOriginalColour(notificationOrigin, prefs);
            int flashDuration = getPreferredFlashDuration(notificationOrigin, prefs);

            sendCustomNotification(vibrateDuration, vibrateTimes, vibratePause, flashTimes, flashColour, originalColour, flashDuration, builder);
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

    private int getPreferredFlashDuration(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_DURATION);
    }

    private int getPreferredOriginalColour(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_ORIGINAL_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR);
    }

    private int getPreferredFlashColour(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COLOUR);
    }

    private int getPreferredFlashCount(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(FLASH_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COUNT);
    }

    private int getPreferredVibratePause(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_PAUSE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PAUSE);
    }

    private int getPreferredVibrateCount(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_COUNT);
    }

    private int getPreferredVibrateDuration(String notificationOrigin, SharedPreferences prefs) {
        return getNotificationPrefIntValue(VIBRATION_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_DURATION);
    }

    @Override
    public void onSMS(String from, String body) {
//        performCustomNotification("sms received", 500, 3, 2000, 0, 0, 0, 0);
        performPreferredNotification("sms received", ORIGIN_SMS);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        performPreferredNotification("email received", ORIGIN_K9MAIL);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        performPreferredNotification("generic notification received", ORIGIN_GENERIC);
    }

    @Override
    public void onSetTime(long ts) {
        try {
            TransactionBuilder builder = performInitialized("Set date and time");
            setCurrentTime(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time on MI device", ex);
        }
    }

    /**
     * Sets the current time to the Mi device using the given builder.
     *
     * @param builder
     */
    private MiBandSupport setCurrentTime(TransactionBuilder builder) {
        Calendar now = GregorianCalendar.getInstance();
        byte[] time = new byte[]{
                (byte) (now.get(Calendar.YEAR) - 2000),
                (byte) now.get(Calendar.MONTH),
                (byte) now.get(Calendar.DATE),
                (byte) now.get(Calendar.HOUR_OF_DAY),
                (byte) now.get(Calendar.MINUTE),
                (byte) now.get(Calendar.SECOND),
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f,
                (byte) 0x0f
        };
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DATE_TIME);
        if (characteristic != null) {
            builder.write(characteristic, time);
        } else {
            LOG.info("Unable to set time -- characteristic not available");
        }
        return this;
    }

    @Override
    public void onSetCallState(String number, String name, GBCommand command) {
        if (GBCommand.CALL_INCOMING.equals(command)) {
            performDefaultNotification("incoming call");
        }
    }

    @Override
    public void onSetMusicInfo(String artist, String album, String track) {
        // not supported
    }

    @Override
    public void onFirmwareVersionReq() {
        try {
            TransactionBuilder builder = performInitialized("Get MI Band device info");
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO);
            builder.read(characteristic).queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read device info from MI", ex);
        }
    }

    @Override
    public void onBatteryInfoReq() {
        try {
            TransactionBuilder builder = performInitialized("Get MI Band battery info");
            requestBatteryInfo(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read battery info from MI", ex);
        }
    }

    @Override
    public void onReboot() {
        try {
            TransactionBuilder builder = performInitialized("Reboot");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), reboot);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to reboot MI", ex);
        }
    }

    @Override
    public void onFetchActivityData() {
        try {
            TransactionBuilder builder = performInitialized("fetch activity data");
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fetch);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to fetch MI activity data", ex);
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        // not supported
    }

    @Override
    public void onAppInfoReq() {
        // not supported
    }

    @Override
    public void onAppStart(UUID uuid) {
        // not supported
    }

    @Override
    public void onAppDelete(UUID uuid) {
        // not supported
    }

    @Override
    public void onPhoneVersion(byte os) {
        // not supported
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO.equals(characteristicUUID)) {
            handleDeviceInfo(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_PAIR.equals(characteristicUUID)) {
            handlePairResult(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_USER_INFO.equals(characteristicUUID)) {
            handleUserInfoResult(characteristic.getValue(), status);
        } else if (MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT.equals(characteristicUUID)) {
            handleControlPointResult(characteristic.getValue(), status);
        }
    }

    private void handleDeviceInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            DeviceInfo info = new DeviceInfo(value);
            getDevice().setFirmwareVersion(info.getFirmwareVersion());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    private void handleActivityNotif(byte[] value) {
        LOG.info("handleActivityNotif GOT " + value.length + " BYTES.");
        if (value.length == 11 ) {
            // byte 0 is the data type: 1 means that each minute is represented by a triplet of bytes
            int dataType = value[0];
            // byte 1 to 6 represent a timestamp
            GregorianCalendar timestamp = new GregorianCalendar(value[1]+2000,
                    value[2],
                    value[3],
                    value[4],
                    value[5],
                    value[6]);

            // counter of all data held by the band
            int totalDataToRead = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
            totalDataToRead *= (dataType == 1) ? 3 : 1;


            // counter of this data block
            int dataUntilNextHeader = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
            dataUntilNextHeader *= (dataType ==1) ? 3 : 1;

            // there is a total of totalDataToRead that will come in chunks (3 bytes per minute if dataType == 1),
            // these chunks are usually 20 bytes long and grouped in blocks
            // after dataUntilNextHeader bytes we will get a new packet of 11 bytes that should be parsed
            // as we just did

            LOG.info("total data to read: "+  totalDataToRead   +" len: " + (totalDataToRead / 3) + " minute(s)");
            LOG.info("data to read until next header: "+  dataUntilNextHeader   +" len: " + (dataUntilNextHeader / 3) + " minute(s)");
            LOG.info("TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()).toString() + " magic byte: " + dataUntilNextHeader);

            this.activityDataRemainingBytes = this.activityDataUntilNextHeader = dataUntilNextHeader;
            this.activityDataTimestampProgress = this.activityDataTimestampToAck = timestamp;

        } else {
            bufferActivityData(value);
        }
        if (this.activityDataRemainingBytes == 0) {
            sendAckDataTransfer(this.activityDataTimestampToAck, this.activityDataUntilNextHeader);
            flushActivityDataHolder();
        }
    }

    private void bufferActivityData(byte[] value) {

        if (this.activityDataRemainingBytes >= value.length) {
            //I don't like this clause, but until we figure out why we get different data sometimes this should work
            if (value.length == 20 || value.length == this.activityDataRemainingBytes) {
                System.arraycopy(value, 0, this.activityDataHolder, this.activityDataHolderProgress, value.length);
                this.activityDataHolderProgress +=value.length;
                this.activityDataRemainingBytes -= value.length;

                if (this.activityDataHolderSize == this.activityDataHolderProgress) {
                    flushActivityDataHolder();
                }
            } else {
                // the length of the chunk is not what we expect. We need to make sense of this data
                LOG.warn("GOT UNEXPECTED ACTIVITY DATA WITH LENGTH: " + value.length + ", EXPECTED LENGTH: " + this.activityDataRemainingBytes);
                for (byte b: value){
                    LOG.warn("DATA: " + String.format("0x%8x", b));
                }
            }
        }
    }

    private void flushActivityDataHolder() {
        GregorianCalendar timestamp = this.activityDataTimestampProgress;
        byte category, intensity, steps;

        ActivityDatabaseHandler dbHandler = GBApplication.getActivityDatabaseHandler();
        try (SQLiteDatabase db = dbHandler.getWritableDatabase()) { // explicitly keep the db open while looping over the samples
            for (int i=0; i<this.activityDataHolderProgress; i+=3) { //TODO: check if multiple of 3, if not something is wrong
                category = this.activityDataHolder[i];
                intensity = this.activityDataHolder[i+1];
                steps = this.activityDataHolder[i+2];

                dbHandler.addGBActivitySample(
                        (int) (timestamp.getTimeInMillis() / 1000),
                        GBActivitySample.PROVIDER_MIBAND,
                        intensity,
                        steps,
                        category);

                ACTIVITYLOG.info(
                        " timestamp:"+DateFormat.getDateTimeInstance().format(timestamp.getTime()).toString() +
                                " category:"+ category+
                                " intensity:"+intensity+
                                " steps:"+steps
                );
                timestamp.add(Calendar.MINUTE, 1);
            }
        }

        this.activityDataHolderProgress = 0;
        this.activityDataTimestampProgress = timestamp;
    }

    private void handleControlPointResult(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Could not write to the control point.");
        }
        LOG.info("handleControlPoint got status:" + status);

        if (getDevice().isBusy()) {
            if (isActivityDataSyncFinished(value)) {
                unsetBusy();
            }
        }
        for (byte b: value){
            LOG.info("handleControlPoint GOT DATA:" + String.format("0x%8x", b));
        }
    }

    private boolean isActivityDataSyncFinished(byte[] value) {
        if (value.length == 9) {
            if (value[0] == 0xa && value[1] == 0xf && value[2] == 5 && value[7] == 0 && value[8] == 0) {
                return true;
            }
        }
        return false;
    }

    private void unsetBusy() {
        getDevice().unsetBusyTask();
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private void sendAckDataTransfer(Calendar time, int bytesTransferred) {
        byte[] ack = new byte[]{
                MiBandService.COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE,
                (byte) (time.get(Calendar.YEAR) - 2000),
                (byte) time.get(Calendar.MONTH),
                (byte) time.get(Calendar.DATE),
                (byte) time.get(Calendar.HOUR_OF_DAY),
                (byte) time.get(Calendar.MINUTE),
                (byte) time.get(Calendar.SECOND),
                (byte) (bytesTransferred & 0xff),
                (byte) (0xff & (bytesTransferred >> 8))
        };
        try {
            TransactionBuilder builder = performInitialized("send acknowledge");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), ack);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send ack to MI", ex);
        }
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BatteryInfo info = new BatteryInfo(value);
            getDevice().setBatteryLevel((short) info.getLevelInPercent());
            getDevice().setBatteryState(info.getStatus());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    private void handleUserInfoResult(byte[] value, int status) {
        // successfully transfered user info means we're initialized
        if (status == BluetoothGatt.GATT_SUCCESS) {
            setConnectionState(State.INITIALIZED);
        }
    }

    private void setConnectionState(State newState) {
        getDevice().setState(newState);
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private void handlePairResult(byte[] pairResult, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.info("Pairing MI device failed: " + status);
            return;
        }

        String value = null;
        if (pairResult != null) {
            if (pairResult.length == 1) {
                try {
                    if (pairResult[0] == 2) {
                        LOG.info("Successfully paired  MI device");
                        return;
                    }
                } catch (Exception ex) {
                    LOG.warn("Error identifying pairing result", ex);
                    return;
                }
            }
            value = Arrays.toString(pairResult);
        }
        LOG.info("MI Band pairing result: " + value);
    }

    @Override
    protected TransactionBuilder createTransactionBuilder(String taskName) {
        return new MiBandTransactionBuilder(taskName);
    }
}