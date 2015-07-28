package nodomain.freeyourgadget.gadgetbridge.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GB;
import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.btle.AbortTransactionAction;
import nodomain.freeyourgadget.gadgetbridge.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.btle.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.btle.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PROFILE;
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
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.getNotificationPrefIntValue;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.getNotificationPrefStringValue;

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    //temporary buffer, size is a multiple of 60 because we want to store complete minutes (1 minute = 3 bytes)
    private static final int activityDataHolderSize = 3 * 60 * 4; // 8h

    private static class ActivityStruct {
        public byte[] activityDataHolder = new byte[activityDataHolderSize];
        //index of the buffer above
        public int activityDataHolderProgress = 0;
        //number of bytes we will get in a single data transfer, used as counter
        public int activityDataRemainingBytes = 0;
        //same as above, but remains untouched for the ack message
        public int activityDataUntilNextHeader = 0;
        //timestamp of the single data transfer, incremented to store each minute's data
        public GregorianCalendar activityDataTimestampProgress = null;
        //same as above, but remains untouched for the ack message
        public GregorianCalendar activityDataTimestampToAck = null;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MiBandSupport.class);
    private volatile boolean telephoneRinging;
    private volatile boolean isLocatingDevice;

    private ActivityStruct activityStruct;

    private DeviceInfo mDeviceInfo;

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
     *
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

    private void sendDefaultNotification(TransactionBuilder builder, short repeat, BtLEAction extraAction) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        LOG.info("Sending notification to MiBand: " + characteristic + " (" + repeat + " times)");
        byte[] defaultNotification = getDefaultNotification();
        for (short i = 0; i < repeat; i++) {
            builder.write(characteristic, defaultNotification);
            builder.add(extraAction);
        }
        builder.queue(getQueue());
    }

    /**
     * Sends a custom notification to the Mi Band.
     *
     * @param vibrationProfile specifies how and how often the Band shall vibrate.
     * @param flashTimes
     * @param flashColour
     * @param originalColour
     * @param flashDuration
     * @param extraAction      an extra action to be executed after every vibration and flash sequence. Allows to abort the repetition, for example.
     * @param builder
     */
    private void sendCustomNotification(VibrationProfile vibrationProfile, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        for (short i = 0; i < vibrationProfile.getRepeat(); i++) {
            int[] onOffSequence = vibrationProfile.getOnOffSequence();
            for (int j = 0; j < onOffSequence.length; j++) {
                int on = onOffSequence[j];
                on = Math.min(500, on); // longer than 500ms is not possible
                builder.write(controlPoint, startVibrate);
                builder.wait(on);
                builder.write(controlPoint, stopVibrate);

                if (++j < onOffSequence.length) {
                    int off = Math.max(onOffSequence[j], 25); // wait at least 25ms
                    builder.wait(off);
                }

                if (extraAction != null) {
                    builder.add(extraAction);
                }
            }
        }

        LOG.info("Sending notification to MiBand: " + controlPoint);
        builder.queue(getQueue());
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

    private static final byte[] startVibrate = new byte[]{MiBandService.COMMAND_SEND_NOTIFICATION, 1};
    private static final byte[] stopVibrate = new byte[]{MiBandService.COMMAND_STOP_MOTOR_VIBRATE};
    private static final byte[] reboot = new byte[]{MiBandService.COMMAND_REBOOT};
    private static final byte[] fetch = new byte[]{MiBandService.COMMAND_FETCH_DATA};

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

    private void performDefaultNotification(String task, short repeat, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder, repeat, extraAction);
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

    private void performPreferredNotification(String task, String notificationOrigin, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int vibrateDuration = getPreferredVibrateDuration(notificationOrigin, prefs);
            int vibratePause = getPreferredVibratePause(notificationOrigin, prefs);
            int vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            VibrationProfile profile = getPreferredVibrateProfile(notificationOrigin, prefs, vibrateTimes);

            int flashTimes = getPreferredFlashCount(notificationOrigin, prefs);
            int flashColour = getPreferredFlashColour(notificationOrigin, prefs);
            int originalColour = getPreferredOriginalColour(notificationOrigin, prefs);
            int flashDuration = getPreferredFlashDuration(notificationOrigin, prefs);

            sendCustomNotification(profile, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
//            sendCustomNotification(vibrateDuration, vibrateTimes, vibratePause, flashTimes, flashColour, originalColour, flashDuration, builder);
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

    private VibrationProfile getPreferredVibrateProfile(String notificationOrigin, SharedPreferences prefs, int repeat) {
        String profileId = getNotificationPrefStringValue(VIBRATION_PROFILE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PROFILE);
        return VibrationProfile.getProfile(profileId, (byte) (repeat & 0xfff));
    }

    @Override
    public void onSetAlarms(ArrayList<GBAlarm> alarms) {
        try {
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
            TransactionBuilder builder = performInitialized("Set alarm");
            for (GBAlarm alarm : alarms) {
                queueAlarm(alarm, builder, characteristic);
            }
            builder.queue(getQueue());
            Toast.makeText(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT).show();
        } catch (IOException ex) {
            Toast.makeText(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG).show();
            LOG.error("Unable to set alarms on MI device", ex);
        }
    }

    @Override
    public void onSMS(String from, String body) {
        performPreferredNotification("sms received", ORIGIN_SMS, null);
    }

    @Override
    public void onEmail(String from, String subject, String body) {
        performPreferredNotification("email received", ORIGIN_K9MAIL, null);
    }

    @Override
    public void onGenericNotification(String title, String details) {
        performPreferredNotification("generic notification received", ORIGIN_GENERIC, null);
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
            telephoneRinging = true;
            AbortTransactionAction abortAction = new AbortTransactionAction() {
                @Override
                protected boolean shouldAbort() {
                    return !isTelephoneRinging();
                }
            };
            performPreferredNotification("incoming call", MiBandConst.ORIGIN_INCOMING_CALL, abortAction);
        } else if (GBCommand.CALL_START.equals(command) || GBCommand.CALL_END.equals(command)) {
            telephoneRinging = false;
        }
    }

    private boolean isTelephoneRinging() {
        // don't synchronize, this is not really important
        return telephoneRinging;
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
    public void onFindDevice(boolean start) {
        isLocatingDevice = start;

        if (start) {
            AbortTransactionAction abortAction = new AbortTransactionAction() {
                @Override
                protected boolean shouldAbort() {
                    return !isLocatingDevice;
                }
            };
            performDefaultNotification("locating device", (short) 255, abortAction);
        }
    }

    @Override
    public void onFetchActivityData() {
        try {
            TransactionBuilder builder = performInitialized("fetch activity data");
//            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getLowLatency());
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fetch);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to fetch MI activity data", ex);
        }
    }

    private byte[] getHighLatency() {
        int minConnectionInterval = 460;
        int maxConnectionInterval = 500;
        int latency = 0;
        int timeout = 500;
        int advertisementInterval = 0;

        return getLatency(minConnectionInterval, maxConnectionInterval, latency, timeout, advertisementInterval);
    }

    private byte[] getLatency(int minConnectionInterval, int maxConnectionInterval, int latency, int timeout, int advertisementInterval) {
        byte result[] = new byte[12];
        result[0] = (byte) (minConnectionInterval & 0xff);
        result[1] = (byte) (0xff & minConnectionInterval >> 8);
        result[2] = (byte) (maxConnectionInterval & 0xff);
        result[3] = (byte) (0xff & maxConnectionInterval >> 8);
        result[4] = (byte) (latency & 0xff);
        result[5] = (byte) (0xff & latency >> 8);
        result[6] = (byte) (timeout & 0xff);
        result[7] = (byte) (0xff & timeout >> 8);
        result[8] = 0;
        result[9] = 0;
        result[10] = (byte) (advertisementInterval & 0xff);
        result[11] = (byte) (0xff & advertisementInterval >> 8);

        return result;
    }

    private byte[] getLowLatency() {
        int minConnectionInterval = 39;
        int maxConnectionInterval = 49;
        int latency = 0;
        int timeout = 500;
        int advertisementInterval = 0;

        return getLatency(minConnectionInterval, maxConnectionInterval, latency, timeout, advertisementInterval);
    }

    @Override
    public void onInstallApp(Uri uri) {
        MiBandFWHelper mFwHelper = new MiBandFWHelper(uri, getContext());
        String mMac = getDevice().getAddress();
        String[] mMacOctets = mMac.split(":");

        int newFwVersion = mFwHelper.getFirmwareVersion();
        int oldFwVersion = mDeviceInfo.getFirmwareVersion();
        int checksum = (Integer.decode("0x" + mMacOctets[4]) << 8 | Integer.decode("0x" + mMacOctets[5])) ^ mFwHelper.getCRC16(mFwHelper.getFw());

        if (sendFirmwareInfo(oldFwVersion, newFwVersion, mFwHelper.getFw().length, checksum)) {
            //metadata were sent correctly, send the actual firmware
            if(sendFirmwareData(mFwHelper.getFw())) {
                //firmware was sent correctly, reboot
                onReboot();
            } else {
                //TODO: the firmware transfer failed, but the miband should be still functional with the old firmware. What should we do?
            }
        }

        return;
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
    public void onScreenshotReq() {
        // not supported
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
        } else if (MiBandService.UUID_CHARACTERISTIC_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
        } else if (MiBandService.UUID_CHARACTERISTIC_NOTIFICATION.equals(characteristicUUID)) {
            // device somehow changed, should we update e.g. battery level?
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
            mDeviceInfo = new DeviceInfo(value);
            getDevice().setFirmwareVersion(mDeviceInfo.getHumanFirmwareVersion());
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    private void queueAlarm(GBAlarm alarm, TransactionBuilder builder, BluetoothGattCharacteristic characteristic) {
        Calendar alarmCal = alarm.getAlarmCal();
        byte[] alarmMessage = new byte[]{
                (byte) MiBandService.COMMAND_SET_TIMER,
                (byte) alarm.getIndex(),
                (byte) (alarm.isEnabled() ? 1 : 0),
                (byte) (alarmCal.get(Calendar.YEAR) - 2000),
                (byte) alarmCal.get(Calendar.MONTH),
                (byte) alarmCal.get(Calendar.DATE),
                (byte) alarmCal.get(Calendar.HOUR_OF_DAY),
                (byte) alarmCal.get(Calendar.MINUTE),
                (byte) alarmCal.get(Calendar.SECOND),
                (byte) (alarm.isSmartWakeup() ? 30 : 0),
                (byte) alarm.getRepetitionMask()
        };
        builder.write(characteristic, alarmMessage);
    }

    private void handleActivityNotif(byte[] value) {
        boolean firstChunk = activityStruct == null;
        if (firstChunk) {
            activityStruct = new ActivityStruct();
        }

        if (value.length == 11) {
            // byte 0 is the data type: 1 means that each minute is represented by a triplet of bytes
            int dataType = value[0];
            // byte 1 to 6 represent a timestamp
            GregorianCalendar timestamp = parseTimestamp(value, 1);

            // counter of all data held by the band
            int totalDataToRead = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
            totalDataToRead *= (dataType == 1) ? 3 : 1;


            // counter of this data block
            int dataUntilNextHeader = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
            dataUntilNextHeader *= (dataType == 1) ? 3 : 1;

            // there is a total of totalDataToRead that will come in chunks (3 bytes per minute if dataType == 1),
            // these chunks are usually 20 bytes long and grouped in blocks
            // after dataUntilNextHeader bytes we will get a new packet of 11 bytes that should be parsed
            // as we just did

            if (firstChunk && dataUntilNextHeader != 0) {
                GB.toast(getContext().getString(R.string.user_feedback_miband_activity_data_transfer,
                        GB.formatDurationHoursMinutes((totalDataToRead / 3), TimeUnit.MINUTES),
                        DateFormat.getDateTimeInstance().format(timestamp.getTime())), Toast.LENGTH_LONG, GB.INFO);
            }
            LOG.info("total data to read: " + totalDataToRead + " len: " + (totalDataToRead / 3) + " minute(s)");
            LOG.info("data to read until next header: " + dataUntilNextHeader + " len: " + (dataUntilNextHeader / 3) + " minute(s)");
            LOG.info("TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()).toString() + " magic byte: " + dataUntilNextHeader);

            activityStruct.activityDataRemainingBytes = activityStruct.activityDataUntilNextHeader = dataUntilNextHeader;
            activityStruct.activityDataTimestampToAck = (GregorianCalendar) timestamp.clone();
            activityStruct.activityDataTimestampProgress = timestamp;

        } else {
            bufferActivityData(value);
        }
        LOG.debug("activity data: length: " + value.length + ", remaining bytes: " + activityStruct.activityDataRemainingBytes);

        if (activityStruct.activityDataRemainingBytes == 0) {
            sendAckDataTransfer(activityStruct.activityDataTimestampToAck, activityStruct.activityDataUntilNextHeader);
        }
    }

    private GregorianCalendar parseTimestamp(byte[] value, int offset) {
        GregorianCalendar timestamp = new GregorianCalendar(
                value[offset] + 2000,
                value[offset + 1],
                value[offset + 2],
                value[offset + 3],
                value[offset + 4],
                value[offset + 5]);
        return timestamp;
    }

    private void bufferActivityData(byte[] value) {

        if (activityStruct.activityDataRemainingBytes >= value.length) {
            //I don't like this clause, but until we figure out why we get different data sometimes this should work
            if (value.length == 20 || value.length == activityStruct.activityDataRemainingBytes) {
                System.arraycopy(value, 0, activityStruct.activityDataHolder, activityStruct.activityDataHolderProgress, value.length);
                activityStruct.activityDataHolderProgress += value.length;
                activityStruct.activityDataRemainingBytes -= value.length;

                if (this.activityDataHolderSize == activityStruct.activityDataHolderProgress) {
                    flushActivityDataHolder();
                }
            } else {
                // the length of the chunk is not what we expect. We need to make sense of this data
                LOG.warn("GOT UNEXPECTED ACTIVITY DATA WITH LENGTH: " + value.length + ", EXPECTED LENGTH: " + activityStruct.activityDataRemainingBytes);
                for (byte b : value) {
                    LOG.warn("DATA: " + String.format("0x%8x", b));
                }
            }
        } else {
            LOG.error("error buffering activity data: remaining bytes: " + activityStruct.activityDataRemainingBytes + ", received: " + value.length);
        }
    }

    private void flushActivityDataHolder() {
        if (activityStruct == null) {
            LOG.debug("nothing to flush, struct is already null");
            return;
        }
        LOG.debug("flushing activity data holder");
        byte category, intensity, steps;

        ActivityDatabaseHandler dbHandler = GBApplication.getActivityDatabaseHandler();

        try (SQLiteDatabase db = dbHandler.getWritableDatabase()) { // explicitly keep the db open while looping over the samples
            for (int i = 0; i < activityStruct.activityDataHolderProgress; i += 3) { //TODO: check if multiple of 3, if not something is wrong
                category = activityStruct.activityDataHolder[i];
                intensity = activityStruct.activityDataHolder[i + 1];
                steps = activityStruct.activityDataHolder[i + 2];

                dbHandler.addGBActivitySample(
                        (int) (activityStruct.activityDataTimestampProgress.getTimeInMillis() / 1000),
                        SampleProvider.PROVIDER_MIBAND,
                        intensity,
                        steps,
                        category);
                activityStruct.activityDataTimestampProgress.add(Calendar.MINUTE, 1);
            }
        } finally {
            activityStruct.activityDataHolderProgress = 0;
        }
    }

    private void handleControlPointResult(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Could not write to the control point.");
        }
        LOG.info("handleControlPoint got status:" + status);

        if (value != null) {
            for (byte b : value) {
                LOG.info("handleControlPoint GOT DATA:" + String.format("0x%8x", b));
            }
        } else {
            LOG.warn("handleControlPoint GOT null");
        }
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

            // flush to the DB after sending the ACK
            flushActivityDataHolder();

            //The last data chunk sent by the miband has always length 0.
            //When we ack this chunk, the transfer is done.
            if (getDevice().isBusy() && bytesTransferred == 0) {
                handleActivityFetchFinish();
            }
        } catch (IOException ex) {
            LOG.error("Unable to send ack to MI", ex);
        }
    }

    private void handleActivityFetchFinish() {
        LOG.info("Fetching activity data has finished.");
        activityStruct = null;
        unsetBusy();
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

    private boolean sendFirmwareInfo(int currentFwVersion, int newFwVersion, int newFwSize, int checksum) {
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
        try {
            TransactionBuilder builder = performInitialized("send firmware info");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fwInfo);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send fwInfo to MI", ex);
            return false;
        }
        return true;
    }

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
                    builder.add(new SetProgressAction("Firmware update in progress", true, (firmwareProgress / len) * 100, getContext()));
                }

                LOG.info("Firmware update progress:" + firmwareProgress + " total len:" + len + " progress:" + (firmwareProgress / len));
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
                builder.add(new SetProgressAction("Firmware installation complete", false, 100, getContext()));
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

    @Override
    protected TransactionBuilder createTransactionBuilder(String taskName) {
        return new MiBandTransactionBuilder(taskName);
    }
}
