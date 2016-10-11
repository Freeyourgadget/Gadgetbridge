package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandDateConverter;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEvents;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ConditionalWriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.Mi2NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations.InitOperation;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.FLASH_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.FLASH_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.FLASH_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.FLASH_ORIGINAL_COLOUR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefIntValue;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefStringValue;

public class MiBand2Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBand2Support.class);
    private final DeviceInfoProfile<MiBand2Support> deviceInfoProfile;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            if (s.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    private boolean needsAuth;
    private volatile boolean telephoneRinging;
    private volatile boolean isLocatingDevice;

    private DeviceInfo mDeviceInfo;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public MiBand2Support() {
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_IMMEDIATE_ALERT);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_ALERT_NOTIFICATION);

        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND2_SERVICE);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        addSupportedProfile(deviceInfoProfile);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DeviceInfoProfile.ACTION_DEVICE_INFO);
        intentFilter.addAction(DeviceService.ACTION_MIBAND2_AUTH);
        broadcastManager.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void dispose() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(mReceiver);
        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        try {
            boolean authenticate = needsAuth;
            needsAuth = false;
            new InitOperation(authenticate, this, builder).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Mi Band 2 failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

//        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZING, getContext()));
//        enableNotifications(builder, true)
//                .setLowLatency(builder)
//                .readDate(builder) // without reading the data, we get sporadic connection problems, especially directly after turning on BT
// this is apparently not needed anymore, and actually causes problems when bonding is not used/does not work
// so we simply not use the UUID_PAIR characteristic.
//                .pair(builder)
                //.requestDeviceInfo(builder)
                //.requestBatteryInfo(builder);
//                .sendUserInfo(builder)
//                .checkAuthenticationNeeded(builder, getDevice())
//                .setWearLocation(builder)
//                .setHeartrateSleepSupport(builder)
//                .setFitnessGoal(builder)
//                .enableFurtherNotifications(builder, true)
//                .setCurrentTime(builder)
//                .requestBatteryInfo(builder)
//                .setHighLatency(builder)
//                .setInitialized(builder);
        return builder;
    }

//    private MiBand2Support maybeAuth(TransactionBuilder builder) {
//        builder.write(getCharacteristic(MiBand2Service.UUID_UNKNOQN_CHARACTERISTIC0), new byte[] {0x20, 0x00});
//        builder.write(getCharacteristic(MiBand2Service.UUID_UNKNOQN_CHARACTERISTIC0), new byte[] {0x03,0x00,(byte)0x8e,(byte)0xce,0x5a,0x09,(byte)0xb3,(byte)0xd8,0x55,0x57,0x10,0x2a,(byte)0xed,0x7d,0x6b,0x78,(byte)0xc5,(byte)0xd2});
//        return this;
//    }

    public MiBand2Support setCurrentTimeWithService(TransactionBuilder builder) {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] bytes = BLETypeConversions.calendarToRawBytes(now, true);
        byte[] tail = new byte[] { 0, BLETypeConversions.mapTimeZone(now.getTimeZone()) }; // 0 = adjust reason bitflags? or DST offset?? , timezone
//        byte[] tail = new byte[] { 0x2 }; // reason
        byte[] all = BLETypeConversions.join(bytes, tail);
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), all);
//        byte[] localtime = BLETypeConversions.calendarToLocalTimeBytes(now);
//        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_LOCAL_TIME_INFORMATION), localtime);
//        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), new byte[] {0x2, 0x00});
//        builder.write(getCharacteristic(MiBand2Service.UUID_UNKNOQN_CHARACTERISTIC0), new byte[] {0x03,0x00,(byte)0x8e,(byte)0xce,0x5a,0x09,(byte)0xb3,(byte)0xd8,0x55,0x57,0x10,0x2a,(byte)0xed,0x7d,0x6b,0x78,(byte)0xc5,(byte)0xd2});
        return this;
    }

    private MiBand2Support readDate(TransactionBuilder builder) {
        // NAVL
//        builder.read(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DATE_TIME));
        // TODO: handle result
        builder.read(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME));
        return this;
    }

    // NAVL
    public MiBand2Support setLowLatency(TransactionBuilder builder) {
//        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getLowLatency());
        return this;
    }
    // NAVL
    public MiBand2Support setHighLatency(TransactionBuilder builder) {
//        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getHighLatency());
        return this;
    }

    private MiBand2Support checkAuthenticationNeeded(TransactionBuilder builder, GBDevice device) {
        builder.add(new CheckAuthenticationNeededAction(device));
        return this;
    }

    /**
     * Last action of initialization sequence. Sets the device to initialized.
     * It is only invoked if all other actions were successfully run, so the device
     * must be initialized, then.
     *
     * @param builder
     */
    public void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZED, getContext()));
    }

    // MB2: AVL
    // TODO: tear down the notifications on quit
    public MiBand2Support enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_NOTIFICATION), enable);
        builder.notify(getCharacteristic(GattService.UUID_SERVICE_CURRENT_TIME), enable);
        // Notify CHARACTERISTIC9 to receive random auth code
        builder.notify(getCharacteristic(MiBand2Service.UUID_CHARACTERISTIC_AUTH), enable);
        return this;
    }

    private MiBand2Support enableFurtherNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY), enable)
                .notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA), enable);
        // cannot use supportsHeartrate() here because we don't have that information yet
        BluetoothGattCharacteristic heartrateCharacteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT);
        if (heartrateCharacteristic != null) {
            builder.notify(heartrateCharacteristic, enable);
        }

        return this;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void pair() {
        needsAuth = true;
        for (int i = 0; i < 5; i++) {
            if (connect()) {
                return;
            }
        }
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    private MiBand2Support sendDefaultNotification(TransactionBuilder builder, short repeat, BtLEAction extraAction) {
        LOG.info("Sending notification to MiBand: (" + repeat + " times)");
        NotificationStrategy strategy = getNotificationStrategy();
        for (short i = 0; i < repeat; i++) {
            strategy.sendDefaultNotification(builder, extraAction);
        }
        return this;
    }

    /**
     * Adds a custom notification to the given transaction builder
     *
     * @param vibrationProfile specifies how and how often the Band shall vibrate.
     * @param flashTimes
     * @param flashColour
     * @param originalColour
     * @param flashDuration
     * @param extraAction      an extra action to be executed after every vibration and flash sequence. Allows to abort the repetition, for example.
     * @param builder
     */
    private MiBand2Support sendCustomNotification(VibrationProfile vibrationProfile, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        getNotificationStrategy().sendCustomNotification(vibrationProfile, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
        LOG.info("Sending notification to MiBand");
        return this;
    }

    private NotificationStrategy getNotificationStrategy() {
        return new Mi2NotificationStrategy(this);
    }

    static final byte[] reboot = new byte[]{MiBandService.COMMAND_REBOOT};

    static final byte[] startHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 1};
    static final byte[] stopHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 0};
    static final byte[] startHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 1};
    static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 0};
    static final byte[] startHeartMeasurementSleep = new byte[]{0x15, MiBandService.COMMAND_SET_HR_SLEEP, 1};
    static final byte[] stopHeartMeasurementSleep = new byte[]{0x15, MiBandService.COMMAND_SET_HR_SLEEP, 0};

    static final byte[] startRealTimeStepsNotifications = new byte[]{MiBandService.COMMAND_SET_REALTIME_STEPS_NOTIFICATION, 1};
    static final byte[] stopRealTimeStepsNotifications = new byte[]{MiBandService.COMMAND_SET_REALTIME_STEPS_NOTIFICATION, 0};

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBand2Support sendUserInfo(TransactionBuilder builder) {
        LOG.debug("Writing User Info!");
        // Use a custom action instead of just builder.write() because mDeviceInfo
        // is set by handleDeviceInfo *after* this action is created.
        builder.add(new BtLEAction(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_USER_INFO)) {
            @Override
            public boolean expectsResult() {
                return true;
            }

            @Override
            public boolean run(BluetoothGatt gatt) {
                // at this point, mDeviceInfo should be set
                return new WriteAction(getCharacteristic(),
                        MiBandCoordinator.getAnyUserInfo(getDevice().getAddress()).getData(mDeviceInfo)
                ).run(gatt);
            }
        });
        return this;
    }

    public MiBand2Support requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
        return this;
    }

   /* private MiBandSupport requestHRInfo(TransactionBuilder builder) {
        LOG.debug("Requesting HR Info!");
        BluetoothGattCharacteristic HRInfo = getCharacteristic(MiBandService.UUID_CHAR_HEART_RATE_MEASUREMENT);
        builder.read(HRInfo);
        BluetoothGattCharacteristic HR_Point = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT);
        builder.read(HR_Point);
        return this;
    }
    *//**
     * Part of HR test. Do not call manually.
     *
     * @param transaction
     * @return
     *//*
    private MiBandSupport heartrate(TransactionBuilder transaction) {
        LOG.info("Attempting to read HR ...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHAR_HEART_RATE_MEASUREMENT);
        if (characteristic != null) {
            transaction.write(characteristic, new byte[]{MiBandService.COMMAND_SET__HR_CONTINUOUS});
        } else {
            LOG.info("Unable to read HR from  MI device -- characteristic not available");
        }
        return this;
    }*/

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */

    private MiBand2Support setFitnessGoal(TransactionBuilder transaction) {
        LOG.info("Attempting to set Fitness Goal...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        if (characteristic != null) {
            int fitnessGoal = MiBandCoordinator.getFitnessGoal(getDevice().getAddress());
            transaction.write(characteristic, new byte[]{
                    MiBandService.COMMAND_SET_FITNESS_GOAL,
                    0,
                    (byte) (fitnessGoal & 0xff),
                    (byte) ((fitnessGoal >>> 8) & 0xff)
            });
        } else {
            LOG.info("Unable to set Fitness Goal");
        }
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */
    private MiBand2Support setWearLocation(TransactionBuilder transaction) {
        LOG.info("Attempting to set wear location...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        if (characteristic != null) {
            transaction.add(new ConditionalWriteAction(characteristic) {
                @Override
                protected byte[] checkCondition() {
                    if (getDeviceInfo() != null && getDeviceInfo().isAmazFit()) {
                        return null;
                    }
                    int location = MiBandCoordinator.getWearLocation(getDevice().getAddress());
                    return new byte[]{
                            MiBandService.COMMAND_SET_WEAR_LOCATION,
                            (byte) location
                    };
                }
            });
        } else {
            LOG.info("Unable to set Wear Location");
        }
        return this;
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("enable heart rate sleep support: " + enable);
            setHeartrateSleepSupport(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error toggling heart rate sleep support: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        // not supported
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        // not supported
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     */
    private MiBand2Support setHeartrateSleepSupport(TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT);
        if (characteristic != null) {
            builder.add(new ConditionalWriteAction(characteristic) {
                @Override
                protected byte[] checkCondition() {
                    if (!supportsHeartRate()) {
                        return null;
                    }
                    if (MiBandCoordinator.getHeartrateSleepSupport(getDevice().getAddress())) {
                        LOG.info("Enabling heartrate sleep support...");
                        return startHeartMeasurementSleep;
                    } else {
                        LOG.info("Disabling heartrate sleep support...");
                        return stopHeartMeasurementSleep;
                    }
                }
            });
        }
        return this;
    }

    private void performDefaultNotification(String task, short repeat, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder, repeat, extraAction);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

    private void performPreferredNotification(String task, String notificationOrigin, int alertLevel, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            Prefs prefs = GBApplication.getPrefs();
            int vibrateDuration = getPreferredVibrateDuration(notificationOrigin, prefs);
            int vibratePause = getPreferredVibratePause(notificationOrigin, prefs);
            short vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            VibrationProfile profile = getPreferredVibrateProfile(notificationOrigin, prefs, vibrateTimes);
            profile.setAlertLevel(alertLevel);

            int flashTimes = getPreferredFlashCount(notificationOrigin, prefs);
            int flashColour = getPreferredFlashColour(notificationOrigin, prefs);
            int originalColour = getPreferredOriginalColour(notificationOrigin, prefs);
            int flashDuration = getPreferredFlashDuration(notificationOrigin, prefs);

            sendCustomNotification(profile, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
//            sendCustomNotification(vibrateDuration, vibrateTimes, vibratePause, flashTimes, flashColour, originalColour, flashDuration, builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

    private int getPreferredFlashDuration(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(FLASH_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_DURATION);
    }

    private int getPreferredOriginalColour(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(FLASH_ORIGINAL_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR);
    }

    private int getPreferredFlashColour(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(FLASH_COLOUR, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COLOUR);
    }

    private int getPreferredFlashCount(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(FLASH_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_FLASH_COUNT);
    }

    private int getPreferredVibratePause(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(VIBRATION_PAUSE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PAUSE);
    }

    private short getPreferredVibrateCount(String notificationOrigin, Prefs prefs) {
        return (short) Math.min(Short.MAX_VALUE, getNotificationPrefIntValue(VIBRATION_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_COUNT));
    }

    private int getPreferredVibrateDuration(String notificationOrigin, Prefs prefs) {
        return getNotificationPrefIntValue(VIBRATION_DURATION, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_DURATION);
    }

    private VibrationProfile getPreferredVibrateProfile(String notificationOrigin, Prefs prefs, short repeat) {
        String profileId = getNotificationPrefStringValue(VIBRATION_PROFILE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PROFILE);
        return VibrationProfile.getProfile(profileId, repeat);
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
            TransactionBuilder builder = performInitialized("Set alarm");
            boolean anyAlarmEnabled = false;
            for (Alarm alarm : alarms) {
                anyAlarmEnabled |= alarm.isEnabled();
                queueAlarm(alarm, builder, characteristic);
            }
            builder.queue(getQueue());
            if (anyAlarmEnabled) {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);
            } else {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException ex) {
            GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        int alertLevel = MiBand2Service.ALERT_LEVEL_MESSAGE;
        if (notificationSpec.type == NotificationType.UNKNOWN) {
            alertLevel = MiBand2Service.ALERT_LEVEL_VIBRATE_ONLY;
        }
        String origin = notificationSpec.type.getGenericType();
        performPreferredNotification(origin + " received", origin, alertLevel, null);
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("Set date and time");
            setCurrentTime(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time on MI device", ex);
        }
        //TODO: once we have a common strategy for sending events (e.g. EventHandler), remove this call from here. Meanwhile it does no harm.
        sendCalendarEvents();
    }

    /**
     * Sets the current time to the Mi device using the given builder.
     *
     * @param builder
     */
    private MiBand2Support setCurrentTime(TransactionBuilder builder) {
        Calendar now = GregorianCalendar.getInstance();
        Date date = now.getTime();
        LOG.info("Sending current time to Mi Band: " + DateTimeUtils.formatDate(date) + " (" + date.toGMTString() + ")");
        byte[] nowBytes = MiBandDateConverter.calendarToRawBytes(now);
        byte[] time = new byte[]{
                nowBytes[0],
                nowBytes[1],
                nowBytes[2],
                nowBytes[3],
                nowBytes[4],
                nowBytes[5],
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
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            telephoneRinging = true;
            AbortTransactionAction abortAction = new AbortTransactionAction() {
                @Override
                protected boolean shouldAbort() {
                    return !isTelephoneRinging();
                }

                @Override
                public boolean run(BluetoothGatt gatt) {
                    if (!super.run(gatt)) {
                        // send a signal to stop the vibration
                        BluetoothGattCharacteristic characteristic = MiBand2Support.this.getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
                        characteristic.setValue(new byte[] {MiBand2Service.ALERT_LEVEL_NONE});
                        gatt.writeCharacteristic(characteristic);
                        return false;
                    }
                    return true;
                }
            };
            performPreferredNotification("incoming call", MiBandConst.ORIGIN_INCOMING_CALL, MiBand2Service.ALERT_LEVEL_PHONE_CALL, abortAction);
        } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
            telephoneRinging = false;
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
    }

    private boolean isTelephoneRinging() {
        // don't synchronize, this is not really important
        return telephoneRinging;
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        // not supported
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        // not supported
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
    public void onHeartRateTest() {
        if (supportsHeartRate()) {
            try {
                TransactionBuilder builder = performInitialized("HeartRateTest");
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), stopHeartMeasurementContinuous);
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), stopHeartMeasurementManual);
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), startHeartMeasurementManual);
                builder.queue(getQueue());
            } catch (IOException ex) {
                LOG.error("Unable to read HearRate in  MI1S", ex);
            }
        } else {
            GB.toast(getContext(), "Heart rate is not supported on this device", Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (supportsHeartRate()) {
            try {
                TransactionBuilder builder = performInitialized("EnableRealtimeHeartRateMeasurement");
                if (enable) {
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), stopHeartMeasurementManual);
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), startHeartMeasurementContinuous);
                } else {
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT), stopHeartMeasurementContinuous);
                }
                builder.queue(getQueue());
            } catch (IOException ex) {
                LOG.error("Unable to enable realtime heart rate measurement in  MI1S", ex);
            }
        }
    }

    public boolean supportsHeartRate() {
        return true;
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
    public void onSetConstantVibration(int intensity) {

    }

    @Override
    public void onFetchActivityData() {
// TODO: onFetchActivityData
//        try {
//            new FetchActivityOperation(this).perform();
//        } catch (IOException ex) {
//            LOG.error("Unable to fetch MI activity data", ex);
//        }
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        try {
            BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
            if (enable) {
                TransactionBuilder builder = performInitialized("Read realtime steps");
                builder.read(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS)).queue(getQueue());
            }
            performInitialized(enable ? "Enabling realtime steps notifications" : "Disabling realtime steps notifications")
                    .write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), enable ? getLowLatency() : getHighLatency())
                    .write(controlPoint, enable ? startRealTimeStepsNotifications : stopRealTimeStepsNotifications).queue(getQueue());
        } catch (IOException e) {
            LOG.error("Unable to change realtime steps notification to: " + enable, e);
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
// TODO: onInstallApp (firmware update)
//        try {
//            new UpdateFirmwareOperation(uri, this).perform();
//        } catch (IOException ex) {
//            GB.toast(getContext(), "Firmware cannot be installed: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
//        }
    }

    @Override
    public void onAppInfoReq() {
        // not supported
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        // not supported
    }

    @Override
    public void onAppDelete(UUID uuid) {
        // not supported
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config) {
        // not supported
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        // not supported
    }

    @Override
    public void onScreenshotReq() {
        // not supported
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_NOTIFICATION.equals(characteristicUUID)) {
            handleNotificationNotif(characteristic.getValue());
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            handleRealtimeSteps(characteristic.getValue());
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            handleRealtimeSteps(characteristic.getValue());
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            handleHeartrate(characteristic.getValue());
            return true;
//        } else if (MiBand2Service.UUID_UNKNOQN_CHARACTERISTIC0.equals(characteristicUUID)) {
//            handleUnknownCharacteristic(characteristic.getValue());
//            return true;
        } else if (MiBand2Service.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            LOG.info("AUTHENTICATION?? " + characteristicUUID);
            logMessageContent(characteristic.getValue());
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            logMessageContent(characteristic.getValue());
        }
        return false;
    }

    private void handleUnknownCharacteristic(byte[] value) {

    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO.equals(characteristicUUID)) {
            handleDeviceInfo(characteristic.getValue(), status);
            return true;
        } else if (GattCharacteristic.UUID_CHARACTERISTIC_GAP_DEVICE_NAME.equals(characteristicUUID)) {
            handleDeviceName(characteristic.getValue(), status);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            logHeartrate(characteristic.getValue(), status);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_DATE_TIME.equals(characteristicUUID)) {
            logDate(characteristic.getValue(), status);
            return true;
        } else {
            LOG.info("Unhandled characteristic read: " + characteristicUUID);
            logMessageContent(characteristic.getValue());
        }
        return false;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_PAIR.equals(characteristicUUID)) {
            handlePairResult(characteristic.getValue(), status);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_USER_INFO.equals(characteristicUUID)) {
            handleUserInfoResult(characteristic.getValue(), status);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT.equals(characteristicUUID)) {
            handleControlPointResult(characteristic.getValue(), status);
            return true;
        } else if (MiBand2Service.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            LOG.info("KEY AES SEND");
            logMessageContent(characteristic.getValue());
            return true;
        }
        return false;
    }

    /**
     * Utility method that may be used to log incoming messages when we don't know how to deal with them yet.
     *
     * @param value
     */
    public void logMessageContent(byte[] value) {
        LOG.info("RECEIVED DATA WITH LENGTH: " + ((value != null) ? value.length : "(null)"));
        if (value != null) {
            for (byte b : value) {
                LOG.warn("DATA: " + String.format("0x%2x", b));
            }
        }
    }

    public void logDate(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            GregorianCalendar calendar = MiBandDateConverter.rawBytesToCalendar(value);
            LOG.info("Got Mi Band Date: " + DateTimeUtils.formatDateTime(calendar.getTime()));
        } else {
            logMessageContent(value);
        }
    }

    public void logHeartrate(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS && value != null) {
            LOG.info("Got heartrate:");
            if (value.length == 2 && value[0] == 6) {
                int hrValue = (value[1] & 0xff);
                GB.toast(getContext(), "Heart Rate measured: " + hrValue, Toast.LENGTH_LONG, GB.INFO);
            }
            return;
        }
        logMessageContent(value);
    }

    private void handleHeartrate(byte[] value) {
        if (value.length == 2 && value[0] == 6) {
            int hrValue = (value[1] & 0xff);
            if (LOG.isDebugEnabled()) {
                LOG.debug("heart rate: " + hrValue);
            }
            Intent intent = new Intent(DeviceService.ACTION_HEARTRATE_MEASUREMENT)
                    .putExtra(DeviceService.EXTRA_HEART_RATE_VALUE, hrValue)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    private void handleRealtimeSteps(byte[] value) {
        int steps = 0xff & value[0] | (0xff & value[1]) << 8;
        if (LOG.isDebugEnabled()) {
            LOG.debug("realtime steps: " + steps);
        }
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_STEPS)
                .putExtra(DeviceService.EXTRA_REALTIME_STEPS, steps)
                .putExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
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
            LOG.info("RECEIVED DATA WITH LENGTH: " + value.length);
            for (byte b : value) {
                LOG.warn("DATA: " + String.format("0x%2x", b));
            }
            return;
        }
        switch (value[0]) {
            case MiBandService.NOTIFY_AUTHENTICATION_FAILED:
                // we get first FAILED, then NOTIFY_STATUS_MOTOR_AUTH (0x13)
                // which means, we need to authenticate by tapping
                getDevice().setState(State.AUTHENTICATION_REQUIRED);
                getDevice().sendDeviceUpdateIntent(getContext());
                GB.toast(getContext(), "Band needs pairing", Toast.LENGTH_LONG, GB.ERROR);
                break;
            case MiBandService.NOTIFY_AUTHENTICATION_SUCCESS: // fall through -- not sure which one we get
            case MiBandService.NOTIFY_RESET_AUTHENTICATION_SUCCESS: // for Mi 1A
            case MiBandService.NOTIFY_STATUS_MOTOR_AUTH_SUCCESS:
                LOG.info("Band successfully authenticated");
                // maybe we can perform the rest of the initialization from here
                doInitialize();
                break;

            case MiBandService.NOTIFY_STATUS_MOTOR_AUTH:
                LOG.info("Band needs authentication (MOTOR_AUTH)");
                getDevice().setState(State.AUTHENTICATING);
                getDevice().sendDeviceUpdateIntent(getContext());
                break;

            case MiBandService.NOTIFY_SET_LATENCY_SUCCESS:
                LOG.info("Setting latency succeeded.");
                break;
            default:
                for (byte b : value) {
                    LOG.warn("DATA: " + String.format("0x%2x", b));
                }
        }
    }

    private void doInitialize() {
        try {
            TransactionBuilder builder = performInitialized("just initializing after authentication");
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to initialize device after authentication", ex);
        }
    }

    private void handleDeviceInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mDeviceInfo = new DeviceInfo(value);
            if (getDeviceInfo().supportsHeartrate()) {
                getDevice().setFirmwareVersion2(MiBandFWHelper.formatFirmwareVersion(mDeviceInfo.getHeartrateFirmwareVersion()));
            }
            LOG.warn("Device info: " + mDeviceInfo);
            versionCmd.hwVersion = mDeviceInfo.getHwVersion();
            versionCmd.fwVersion = MiBandFWHelper.formatFirmwareVersion(mDeviceInfo.getFirmwareVersion());
            handleGBDeviceEvent(versionCmd);
        }
    }

    private void handleDeviceName(byte[] value, int status) {
//        if (status == BluetoothGatt.GATT_SUCCESS) {
//            versionCmd.hwVersion = new String(value);
//            handleGBDeviceEvent(versionCmd);
//        }
    }

    /**
     * Convert an alarm from the GB internal structure to a Mi Band message and put on the specified
     * builder queue as a write message for the passed characteristic
     *
     * @param alarm
     * @param builder
     * @param characteristic
     */
    private void queueAlarm(Alarm alarm, TransactionBuilder builder, BluetoothGattCharacteristic characteristic) {
        byte[] alarmCalBytes = MiBandDateConverter.calendarToRawBytes(alarm.getAlarmCal());

        byte[] alarmMessage = new byte[]{
                MiBandService.COMMAND_SET_TIMER,
                (byte) alarm.getIndex(),
                (byte) (alarm.isEnabled() ? 1 : 0),
                alarmCalBytes[0],
                alarmCalBytes[1],
                alarmCalBytes[2],
                alarmCalBytes[3],
                alarmCalBytes[4],
                alarmCalBytes[5],
                (byte) (alarm.isSmartWakeup() ? 30 : 0),
                (byte) alarm.getRepetitionMask()
        };
        builder.write(characteristic, alarmMessage);
    }

    private void handleControlPointResult(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Could not write to the control point.");
        }
        LOG.info("handleControlPoint write status:" + status + "; length: " + (value != null ? value.length : "(null)"));

        if (value != null) {
            for (byte b : value) {
                LOG.info("handleControlPoint WROTE DATA:" + String.format("0x%8x", b));
            }
        } else {
            LOG.warn("handleControlPoint WROTE null");
        }
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
//        if (getDeviceInfo().supportsHeartrate()) {
//            getDevice().addDeviceInfo(new GenericItem(
//                    getContext().getString(R.string.DEVINFO_HR_VER),
//                    info.getSoftwareRevision()));
//        }
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
//        versionCmd.fwVersion = info.getFirmwareRevision(); // always null
        versionCmd.fwVersion = info.getSoftwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BatteryInfo info = new BatteryInfo(value);
            batteryCmd.level = ((short) info.getLevelInPercent());
            batteryCmd.state = info.getState();
            batteryCmd.lastChargeTime = info.getLastChargeTime();
            batteryCmd.numCharges = info.getNumCharges();
            handleGBDeviceEvent(batteryCmd);
        }
    }

    private void handleUserInfoResult(byte[] value, int status) {
        // successfully transferred user info means we're initialized
// commented out, because we have SetDeviceStateAction which sets initialized
// state on every successful initialization.
//        if (status == BluetoothGatt.GATT_SUCCESS) {
//            setConnectionState(State.INITIALIZED);
//        }
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

    /**
     * Fetch the events from the android device calendars and set the alarms on the miband.
     */
    private void sendCalendarEvents() {
        try {
            TransactionBuilder builder = performInitialized("Send upcoming events");
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);

            Prefs prefs = GBApplication.getPrefs();
            int availableSlots = prefs.getInt(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, 0);

            if (availableSlots > 0) {
                CalendarEvents upcomingEvents = new CalendarEvents();
                List<CalendarEvents.CalendarEvent> mEvents = upcomingEvents.getCalendarEventList(getContext());

                int iteration = 0;
                for (CalendarEvents.CalendarEvent mEvt : mEvents) {
                    if (iteration >= availableSlots || iteration > 2) {
                        break;
                    }
                    int slotToUse = 2 - iteration;
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(mEvt.getBegin());
                    byte[] calBytes = MiBandDateConverter.calendarToRawBytes(calendar);

                    byte[] alarmMessage = new byte[]{
                            MiBandService.COMMAND_SET_TIMER,
                            (byte) slotToUse,
                            (byte) 1,
                            calBytes[0],
                            calBytes[1],
                            calBytes[2],
                            calBytes[3],
                            calBytes[4],
                            calBytes[5],
                            (byte) 0,
                            (byte) 0
                    };
                    builder.write(characteristic, alarmMessage);
                    iteration++;
                }
                builder.queue(getQueue());
            }
        } catch (IOException ex) {
            LOG.error("Unable to send Events to MI device", ex);
        }
    }


}
