/*  Copyright (C) 2015-2019 Andreas Shimokawa, atkyritsis, Carsten Pfeiffer,
    Christian Fischer, Daniele Gobbetti, freezed-or-frozen, JohnnySun, Julien
    Pivotto, Kasha, Sergey Trofimov, Steffen Liebergeld

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
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

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandDateConverter;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
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
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
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

public class MiBandSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBandSupport.class);
    /**
     * This is just for temporary testing of Mi1A double firmware update.
     * DO NOT SET TO TRUE UNLESS YOU KNOW WHAT YOU'RE DOING!
     */
    public static final boolean MI_1A_HR_FW_UPDATE_TEST_MODE_ENABLED = false;
    private volatile boolean telephoneRinging;
    private volatile boolean isLocatingDevice;
    private volatile boolean isReadingSensorData;

    private DeviceInfo mDeviceInfo;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private RealtimeSamplesSupport realtimeSamplesSupport;
    private boolean alarmClockRining;
    private boolean alarmClockRinging;

    public MiBandSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
        addSupportedService(MiBandService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_IMMEDIATE_ALERT);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZING, getContext()));
        enableNotifications(builder, true)
                .setLowLatency(builder)
                .readDate(builder) // without reading the data, we get sporadic connection problems, especially directly after turning on BT
                .pair(builder)
                .requestDeviceInfo(builder)
                .sendUserInfo(builder)
                .checkAuthenticationNeeded(builder, getDevice())
                .setWearLocation(builder)
                .setHeartrateSleepSupport(builder)
                .setFitnessGoal(builder)
                .enableFurtherNotifications(builder, true)
                .setCurrentTime(builder)
                .requestBatteryInfo(builder)
                .setHighLatency(builder)
                .setInitialized(builder);
        return builder;
    }

    private MiBandSupport readDate(TransactionBuilder builder) {
        builder.read(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DATE_TIME));
        return this;
    }

    public MiBandSupport setLowLatency(TransactionBuilder builder) {
        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getLowLatency());
        return this;
    }

    public MiBandSupport setHighLatency(TransactionBuilder builder) {
        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getHighLatency());
        return this;
    }

    private MiBandSupport checkAuthenticationNeeded(TransactionBuilder builder, GBDevice device) {
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
    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), State.INITIALIZED, getContext()));
    }

    // TODO: tear down the notifications on quit
    private MiBandSupport enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_NOTIFICATION), enable);
        return this;
    }

    private MiBandSupport enableFurtherNotifications(TransactionBuilder builder, boolean enable) {
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
    public boolean connectFirstTime() {
        for (int i = 0; i < 5; i++) {
            if (connect()) {
                return true;
            }
        }
        return false;
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    private MiBandSupport sendDefaultNotification(TransactionBuilder builder, SimpleNotification simpleNotification, short repeat, BtLEAction extraAction) {
        LOG.info("Sending notification to MiBand: (" + repeat + " times)");
        NotificationStrategy strategy = getNotificationStrategy();
        for (short i = 0; i < repeat; i++) {
            strategy.sendDefaultNotification(builder, simpleNotification, extraAction);
        }
        return this;
    }

    /**
     * Adds a custom notification to the given transaction builder
     *  @param vibrationProfile specifies how and how often the Band shall vibrate.
     * @param simpleNotification
     * @param flashTimes
     * @param flashColour
     * @param originalColour
     * @param flashDuration
     * @param extraAction      an extra action to be executed after every vibration and flash sequence. Allows to abort the repetition, for example.
     * @param builder
     */
    private MiBandSupport sendCustomNotification(VibrationProfile vibrationProfile, @Nullable SimpleNotification simpleNotification, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        getNotificationStrategy().sendCustomNotification(vibrationProfile, simpleNotification, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
        LOG.info("Sending notification to MiBand");
        return this;
    }

    private NotificationStrategy getNotificationStrategy() {
        if (mDeviceInfo == null) {
            // not initialized yet?
            return new NoNotificationStrategy();
        }
        if (mDeviceInfo.getFirmwareVersion() < MiBandFWHelper.FW_16779790) {
            return new V1NotificationStrategy(this);
        } else {
            //use the new alert characteristic
            return new V2NotificationStrategy(this);
        }
    }

    static final byte[] reboot = new byte[]{MiBandService.COMMAND_REBOOT};
    static final byte[] factoryReset = new byte[]{MiBandService.COMMAND_FACTORYRESET};

    static final byte[] startHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 1};
    static final byte[] stopHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 0};
    static final byte[] startHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 1};
    static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 0};
    static final byte[] startHeartMeasurementSleep = new byte[]{0x15, MiBandService.COMMAND_SET_HR_SLEEP, 1};
    static final byte[] stopHeartMeasurementSleep = new byte[]{0x15, MiBandService.COMMAND_SET_HR_SLEEP, 0};

    static final byte[] startRealTimeStepsNotifications = new byte[]{MiBandService.COMMAND_SET_REALTIME_STEPS_NOTIFICATION, 1};
    static final byte[] stopRealTimeStepsNotifications = new byte[]{MiBandService.COMMAND_SET_REALTIME_STEPS_NOTIFICATION, 0};

    private static final byte[] startSensorRead = new byte[]{MiBandService.COMMAND_GET_SENSOR_DATA, 1};
    private static final byte[] stopSensorRead = new byte[]{MiBandService.COMMAND_GET_SENSOR_DATA, 0};

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private MiBandSupport sendUserInfo(TransactionBuilder builder) {
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

    private MiBandSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_BATTERY);
        builder.read(characteristic);
        return this;
    }

    private MiBandSupport requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        BluetoothGattCharacteristic deviceInfo = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_DEVICE_INFO);
        builder.read(deviceInfo);
        BluetoothGattCharacteristic deviceName = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_GAP_DEVICE_NAME);
        builder.read(deviceName);
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
    private MiBandSupport pair(TransactionBuilder transaction) {
// this is apparently only needed to get a more strict bond between mobile and mi band,
// e.g. such that Mi Fit and Gadgetbridge can coexist without needing to re-pair (with
// full device-data-reset).
// Unfortunately this extra pairing causes problems when bonding is not used/does not work
// so we only do this when configured to keep data on the device

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean(MiBandConst.PREF_MIBAND_DONT_ACK_TRANSFER, false)) {
            LOG.info("Attempting to pair MI device...");
            BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_PAIR);
            if (characteristic != null) {
                transaction.write(characteristic, new byte[]{2});
            } else {
                LOG.info("Unable to pair MI device -- characteristic not available");
            }
        }
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */

    private MiBandSupport setFitnessGoal(TransactionBuilder transaction) {
        LOG.info("Attempting to set Fitness Goal...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        if (characteristic != null) {
            int fitnessGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
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
    private MiBandSupport setWearLocation(TransactionBuilder transaction) {
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
    public void onSetHeartRateMeasurementInterval(int seconds) {

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
    private MiBandSupport setHeartrateSleepSupport(TransactionBuilder builder) {
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

    private void performDefaultNotification(String task, SimpleNotification simpleNotification, short repeat, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            sendDefaultNotification(builder, simpleNotification, repeat, extraAction);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to MI device", ex);
        }
    }

    private void performPreferredNotification(String task, @Nullable SimpleNotification simpleNotification, String notificationOrigin, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            Prefs prefs = GBApplication.getPrefs();
            int vibrateDuration = getPreferredVibrateDuration(notificationOrigin, prefs);
            int vibratePause = getPreferredVibratePause(notificationOrigin, prefs);
            short vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            VibrationProfile profile = getPreferredVibrateProfile(notificationOrigin, prefs, vibrateTimes);

            int flashTimes = getPreferredFlashCount(notificationOrigin, prefs);
            int flashColour = getPreferredFlashColour(notificationOrigin, prefs);
            int originalColour = getPreferredOriginalColour(notificationOrigin, prefs);
            int flashDuration = getPreferredFlashDuration(notificationOrigin, prefs);

//            setLowLatency(builder);
            sendCustomNotification(profile, simpleNotification, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
//            setHighLatency(builder);
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
                anyAlarmEnabled |= alarm.getEnabled();
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
        if (notificationSpec.type == NotificationType.GENERIC_ALARM_CLOCK) {
            onAlarmClock(notificationSpec);
            return;
        }

        String origin = notificationSpec.type.getGenericType();
        performPreferredNotification(origin + " received", null, origin, null);
    }

    private void onAlarmClock(NotificationSpec notificationSpec) {
        alarmClockRining = true;
        AbortTransactionAction abortAction = new AbortTransactionAction() {
            @Override
            protected boolean shouldAbort() {
                return !isAlarmClockRinging();
            }
        };
        String message = NotificationUtils.getPreferredTextFor(notificationSpec, 40, 40, getContext());
        SimpleNotification simpleNotification = new SimpleNotification(message, AlertCategory.HighPriorityAlert, null);
        performPreferredNotification("alarm clock ringing", simpleNotification, MiBandConst.ORIGIN_ALARM_CLOCK, abortAction);
    }

    @Override
    public void onDeleteNotification(int id) {
        alarmClockRining = false; // we should have the notificationtype at least to check
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
    private MiBandSupport setCurrentTime(TransactionBuilder builder) {
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
            };
            String message = NotificationUtils.getPreferredTextFor(callSpec);
            SimpleNotification simpleNotification = new SimpleNotification(message, AlertCategory.IncomingCall, null);
            performPreferredNotification("incoming call", simpleNotification, MiBandConst.ORIGIN_INCOMING_CALL, abortAction);
        } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
            telephoneRinging = false;
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
    }

    private boolean isAlarmClockRinging() {
        // don't synchronize, this is not really important
        return alarmClockRinging;
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
    public void onReset(int flags) {
        try {
            TransactionBuilder builder = performInitialized("reset");
            if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), factoryReset);
            } else {
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), reboot);
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to reset", ex);
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
                enableRealtimeSamplesTimer(enable);
            } catch (IOException ex) {
                LOG.error("Unable to enable realtime heart rate measurement in  MI1S", ex);
            }
        }
    }

    public boolean supportsHeartRate() {
        return getDeviceInfo() != null && getDeviceInfo().supportsHeartrate();
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
            SimpleNotification simpleNotification = new SimpleNotification(getContext().getString(R.string.find_device_you_found_it), AlertCategory.HighPriorityAlert, null);
            performDefaultNotification("locating device", simpleNotification, (short) 255, abortAction);
        }
    }

    @Override
    public void onSetConstantVibration(int intensity) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            new FetchActivityOperation(this).perform();
        } catch (IOException ex) {
            LOG.error("Unable to fetch MI activity data", ex);
        }
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
            enableRealtimeSamplesTimer(enable);
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
        try {
            new UpdateFirmwareOperation(uri, this).perform();
        } catch (IOException ex) {
            GB.toast(getContext(), "Firmware cannot be installed: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
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
    public void onAppConfiguration(UUID uuid, String config, Integer id) {
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
        } else if (MiBandService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            handleHeartrate(characteristic.getValue());
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_SENSOR_DATA.equals(characteristicUUID)) {
            handleSensorData(characteristic.getValue());
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            logMessageContent(characteristic.getValue());
        }
        return false;
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
        }
        return false;
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
            RealtimeSamplesSupport realtimeSamplesSupport = getRealtimeSamplesSupport();
            realtimeSamplesSupport.setHeartrateBpm(hrValue);
            if (!realtimeSamplesSupport.isRunning()) {
                // single shot measurement, manually invoke storage and result publishing
                realtimeSamplesSupport.triggerCurrentSample();
            }
        }
    }

    private void handleRealtimeSteps(byte[] value) {
        int steps = BLETypeConversions.toUint16(value);
        if (LOG.isDebugEnabled()) {
            LOG.debug("realtime steps: " + steps);
        }
        getRealtimeSamplesSupport().setSteps(steps);
    }

    private void enableRealtimeSamplesTimer(boolean enable) {
        if (enable) {
            getRealtimeSamplesSupport().start();
        } else {
            if (realtimeSamplesSupport != null) {
                realtimeSamplesSupport.stop();
            }
        }
    }

    public MiBandActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        MiBandActivitySample sample = new MiBandActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }

    private RealtimeSamplesSupport getRealtimeSamplesSupport() {
        if (realtimeSamplesSupport == null) {
            realtimeSamplesSupport = new RealtimeSamplesSupport(1000, 1000) {
                @Override
                public void doCurrentSample() {

                    try (DBHandler handler = GBApplication.acquireDB()) {
                        DaoSession session = handler.getDaoSession();

                        Device device = DBHelper.getDevice(getDevice(), session);
                        User user = DBHelper.getUser(session);
                        int ts = (int) (System.currentTimeMillis() / 1000);
                        MiBandSampleProvider provider = new MiBandSampleProvider(gbDevice, session);
                        MiBandActivitySample sample = createActivitySample(device, user, ts, provider);
                        sample.setHeartRate(getHeartrateBpm());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                        sample.setRawKind(MiBandSampleProvider.TYPE_ACTIVITY); // to make it visible in the charts TODO: add a MANUAL kind for that?

                        provider.addGBActivitySample(sample);

                        // set the steps only afterwards, since realtime steps are also recorded
                        // in the regular samples and we must not count them twice
                        // Note: we know that the DAO sample is never committed again, so we simply
                        // change the value here in memory.
                        sample.setSteps(getSteps());

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("realtime sample: " + sample);
                        }

                        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

                    } catch (Exception e) {
                        LOG.warn("Unable to acquire db for saving realtime samples", e);
                    }
                }
            };
        }
        return realtimeSamplesSupport;
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
            LOG.warn("DATA: " + GB.hexdump(value, 0, value.length));
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
                LOG.warn("DATA: " + GB.hexdump(value, 0, value.length));
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
            mDeviceInfo.setTest1AHRMode(MI_1A_HR_FW_UPDATE_TEST_MODE_ENABLED);
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
        byte[] alarmCalBytes = MiBandDateConverter.calendarToRawBytes(AlarmUtils.toCalendar(alarm));

        byte[] alarmMessage = new byte[]{
                MiBandService.COMMAND_SET_TIMER,
                (byte) alarm.getPosition(),
                (byte) (alarm.getEnabled() ? 1 : 0),
                alarmCalBytes[0],
                alarmCalBytes[1],
                alarmCalBytes[2],
                alarmCalBytes[3],
                alarmCalBytes[4],
                alarmCalBytes[5],
                (byte) (alarm.getSmartWakeup() ? 30 : 0),
                (byte) alarm.getRepetition()
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
                    Alarm alarm = AlarmUtils.createSingleShot(slotToUse, false, calendar);
                    queueAlarm(alarm, builder, characteristic);
                    iteration++;
                }
                builder.queue(getQueue());
            }
        } catch (IOException ex) {
            LOG.error("Unable to send Events to MI device", ex);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        // nothing yet
    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {
        try {
            TransactionBuilder builder = performInitialized("Toggle sensor reading");
            if (isReadingSensorData) {
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), stopSensorRead);
                isReadingSensorData = false;
            } else {
                builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), startSensorRead);
                isReadingSensorData = true;
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to toggle sensor reading MI", ex);
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    /**
     * Analyse and decode sensor data from ADXL362 accelerometer
     * @param value to decode
     * @return nothing
     *
     * Each axis raw value is 16bits long and look like : ttssvvvvvvvvvvvv
     *   tt : 2 bits for the type of data (00=x, 01=y, 10=z, 11=temperature)
     *   ss : sign of the value
     *   vvvvvvvvvvvv : accelerometer value encoded using two complements
     *
     * TODO: Because each accelerometer is different, all values should be calibrated with  :
     *   a scale factor
     *   an offset factor
     */
    private static void handleSensorData(byte[] value) {
        int counter=0, step=0;
        double xAxis=0.0, yAxis=0.0, zAxis=0.0;
        double scale_factor = 1000.0;
        double gravity = 9.81;

        if ((value.length - 2) % 6 != 0) {
            LOG.warn("GOT UNEXPECTED SENSOR DATA WITH LENGTH: " + value.length);
            LOG.warn("DATA: " + GB.hexdump(value, 0, value.length));
        }
        else {
            counter = (value[0] & 0xff) | ((value[1] & 0xff) << 8);
            for (int idx = 0; idx < ((value.length - 2) / 6); idx++) {
                step = idx * 6;

                // Analyse X-axis data
                int xAxisRawValue = (value[step+2] & 0xff) | ((value[step+3] & 0xff) << 8);
                int xAxisSign = (value[step+3] & 0x30) >> 4;
                int xAxisType = (value[step+3] & 0xc0) >> 6;
                if (xAxisSign == 0) {
                    xAxis = xAxisRawValue & 0xfff;
                }
                else {
                    xAxis = (xAxisRawValue & 0xfff) - 4097;
                }
                xAxis = (xAxis*1.0 / scale_factor) * gravity;

                // Analyse Y-axis data
                int yAxisRawValue = (value[step+4] & 0xff) | ((value[step+5] & 0xff) << 8);
                int yAxisSign = (value[step+5] & 0x30) >> 4;
                int yAxisType = (value[step+5] & 0xc0) >> 6;
                if (yAxisSign == 0) {
                    yAxis = yAxisRawValue & 0xfff;
                }
                else {
                    yAxis = (yAxisRawValue & 0xfff) - 4097;
                }
                yAxis = (yAxis / scale_factor) * gravity;

                // Analyse Z-axis data
                int zAxisRawValue = (value[step+6] & 0xff) | ((value[step+7] & 0xff) << 8);
                int zAxisSign = (value[step+7] & 0x30) >> 4;
                int zAxisType = (value[step+7] & 0xc0) >> 6;
                if (zAxisSign == 0) {
                    zAxis = zAxisRawValue & 0xfff;
                }
                else {
                    zAxis = (zAxisRawValue & 0xfff) - 4097;
                }
                zAxis = (zAxis / scale_factor) * gravity;

                // Print results in log
                LOG.info("READ SENSOR DATA VALUES: counter:"+counter+" step:"+step+" x-axis:"+ String.format("%.03f",xAxis)+" y-axis:"+String.format("%.03f",yAxis)+" z-axis:"+String.format("%.03f",zAxis)+";");
            }
        }
    }
}
