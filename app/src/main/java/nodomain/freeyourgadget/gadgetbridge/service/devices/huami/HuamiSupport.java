/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Christian
    Fischer, Daniele Gobbetti, JohnnySun, José Rebelo, Julien Pivotto, Kasha,
    Michal Novotny, Sergey Trofimov, Steffen Liebergeld

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLift;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband2.MiBand2FWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DateTimeDisplay;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DoNotDisturb;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEvents;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.Mi2NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.Mi2TextNotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.actions.StopNotificationAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchSportsSummaryOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.InitOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.RealtimeSamplesSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

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

public class HuamiSupport extends AbstractBTLEDeviceSupport {

    // We introduce key press counter for notification purposes
    private static int currentButtonActionId = 0;
    private static int currentButtonPressCount = 0;
    private static long currentButtonPressTime = 0;
    private static long currentButtonTimerActivationTime = 0;

    private static final Logger LOG = LoggerFactory.getLogger(HuamiSupport.class);
    private final DeviceInfoProfile<HuamiSupport> deviceInfoProfile;
    private final HeartRateProfile<HuamiSupport> heartRateProfile;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(s)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    private BluetoothGattCharacteristic characteristicHRControlPoint;
    protected BluetoothGattCharacteristic characteristicChunked;

    private boolean needsAuth;
    private volatile boolean telephoneRinging;
    private volatile boolean isLocatingDevice;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

    private RealtimeSamplesSupport realtimeSamplesSupport;
    private boolean alarmClockRinging;

    private boolean isMusicAppStarted = false;
    private MusicSpec bufferMusicSpec = null;
    private MusicStateSpec bufferMusicStateSpec = null;

    public HuamiSupport() {
        this(LOG);
    }

    public HuamiSupport(Logger logger) {
        super(logger);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        addSupportedService(GattService.UUID_SERVICE_IMMEDIATE_ALERT);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_ALERT_NOTIFICATION);

        addSupportedService(MiBandService.UUID_SERVICE_MIBAND_SERVICE);
        addSupportedService(MiBandService.UUID_SERVICE_MIBAND2_SERVICE);
        addSupportedService(HuamiService.UUID_SERVICE_FIRMWARE_SERVICE);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        addSupportedProfile(deviceInfoProfile);
        heartRateProfile = new HeartRateProfile<>(this);
        addSupportedProfile(heartRateProfile);

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
            byte authFlags = HuamiService.AUTH_BYTE;
            if (gbDevice.getType() == DeviceType.MIBAND3) {
                authFlags = 0x00;
            }
            new InitOperation(authenticate, authFlags, this, builder).perform();
            characteristicHRControlPoint = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT);
            characteristicChunked = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER);
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Mi Band 2 failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        return builder;
    }

    /**
     * Returns the given date/time (calendar) as a byte sequence, suitable for sending to the
     * Mi Band 2 (or derivative). The band appears to not handle DST offsets, so we simply add this
     * to the timezone.
     * @param calendar
     * @param precision
     * @return
     */
    public byte[] getTimeBytes(Calendar calendar, TimeUnit precision) {
        byte[] bytes;
        if (precision == TimeUnit.MINUTES) {
            bytes = BLETypeConversions.shortCalendarToRawBytes(calendar, true);
        } else if (precision == TimeUnit.SECONDS) {
            bytes = BLETypeConversions.calendarToRawBytes(calendar, true);
        } else {
            throw new IllegalArgumentException("Unsupported precision, only MINUTES and SECONDS are supported till now");
        }
        byte[] tail = new byte[] { 0, BLETypeConversions.mapTimeZone(calendar.getTimeZone(), BLETypeConversions.TZ_FLAG_INCLUDE_DST_IN_TZ) };
        // 0 = adjust reason bitflags? or DST offset?? , timezone
//        byte[] tail = new byte[] { 0x2 }; // reason
        byte[] all = BLETypeConversions.join(bytes, tail);
        return all;
    }

    public Calendar fromTimeBytes(byte[] bytes) {
        GregorianCalendar timestamp = BLETypeConversions.rawBytesToCalendar(bytes, true);
        return timestamp;
    }

    public HuamiSupport setCurrentTimeWithService(TransactionBuilder builder) {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] bytes = getTimeBytes(now, TimeUnit.SECONDS);
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), bytes);
        return this;
    }

    public HuamiSupport setLowLatency(TransactionBuilder builder) {
        // TODO: low latency?
        return this;
    }

    public HuamiSupport setHighLatency(TransactionBuilder builder) {
        // TODO: high latency?
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
    public HuamiSupport enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_NOTIFICATION), enable);
        builder.notify(getCharacteristic(GattService.UUID_SERVICE_CURRENT_TIME), enable);
        // Notify CHARACTERISTIC9 to receive random auth code
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUTH), enable);
        return this;
    }

    public HuamiSupport enableFurtherNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), enable);
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO), enable);
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT), enable);

        return this;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public boolean connectFirstTime() {
        needsAuth = true;
        return super.connect();
    }

    private HuamiSupport sendDefaultNotification(TransactionBuilder builder, SimpleNotification simpleNotification, short repeat, BtLEAction extraAction) {
        LOG.info("Sending notification to MiBand: (" + repeat + " times)");
        NotificationStrategy strategy = getNotificationStrategy();
        for (short i = 0; i < repeat; i++) {
            strategy.sendDefaultNotification(builder, simpleNotification, extraAction);
        }
        return this;
    }

    /**
     * Adds a custom notification to the given transaction builder
     * @param vibrationProfile specifies how and how often the Band shall vibrate.
     * @param simpleNotification
     * @param flashTimes
     * @param flashColour
     * @param originalColour
     * @param flashDuration
     * @param extraAction      an extra action to be executed after every vibration and flash sequence. Allows to abort the repetition, for example.
     * @param builder
     */
    private HuamiSupport sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        getNotificationStrategy().sendCustomNotification(vibrationProfile, simpleNotification, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);
        LOG.info("Sending notification to MiBand");
        return this;
    }

    public NotificationStrategy getNotificationStrategy() {
        String firmwareVersion = getDevice().getFirmwareVersion();
        if (firmwareVersion != null) {
            Version ver = new Version(firmwareVersion);
            if (MiBandConst.MI2_FW_VERSION_MIN_TEXT_NOTIFICATIONS.compareTo(ver) > 0) {
                return new Mi2NotificationStrategy(this);
            }
        }
        if (GBApplication.getPrefs().getBoolean(MiBandConst.PREF_MI2_ENABLE_TEXT_NOTIFICATIONS, true)) {
            return new Mi2TextNotificationStrategy(this);
        }
        return new Mi2NotificationStrategy(this);
    }

    private static final byte[] startHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 1};
    private static final byte[] stopHeartMeasurementManual = new byte[]{0x15, MiBandService.COMMAND_SET_HR_MANUAL, 0};
    private static final byte[] startHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 1};
    private static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, MiBandService.COMMAND_SET__HR_CONTINUOUS, 0};

    private HuamiSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info!");
        BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO);
        builder.read(characteristic);
        return this;
    }

    public HuamiSupport requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param transaction
     * @return
     */

    private HuamiSupport setFitnessGoal(TransactionBuilder transaction) {
        LOG.info("Attempting to set Fitness Goal...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_8_USER_SETTINGS);
        if (characteristic != null) {
            int fitnessGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, 10000);
            byte[] bytes = ArrayUtils.addAll(
                    HuamiService.COMMAND_SET_FITNESS_GOAL_START,
                    BLETypeConversions.fromUint16(fitnessGoal));
            bytes = ArrayUtils.addAll(bytes,
                    HuamiService.COMMAND_SET_FITNESS_GOAL_END);
            transaction.write(characteristic, bytes);
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

    private HuamiSupport setUserInfo(TransactionBuilder transaction) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_8_USER_SETTINGS);
        if (characteristic == null) {
            return this;
        }

        LOG.info("Attempting to set user info...");
        Prefs prefs = GBApplication.getPrefs();
        String alias = prefs.getString(MiBandConst.PREF_USER_ALIAS, null);
        ActivityUser activityUser = new ActivityUser();
        int height = activityUser.getHeightCm();
        int weight = activityUser.getWeightKg();
        int birth_year = activityUser.getYearOfBirth();
        byte birth_month = 7; // not in user attributes
        byte birth_day = 1; // not in user attributes

        if (alias == null || weight == 0 || height == 0 || birth_year == 0) {
            LOG.warn("Unable to set user info, make sure it is set up");
            return this;
        }

        byte sex = 2; // other
        switch (activityUser.getGender()) {
            case ActivityUser.GENDER_MALE:
                sex = 0;
                break;
            case ActivityUser.GENDER_FEMALE:
                sex = 1;
        }
        int userid = alias.hashCode(); // hash from alias like mi1

        // FIXME: Do encoding like in PebbleProtocol, this is ugly
        byte bytes[] = new byte[]{
                HuamiService.COMMAND_SET_USERINFO,
                0,
                0,
                (byte) (birth_year & 0xff),
                (byte) ((birth_year >> 8) & 0xff),
                birth_month,
                birth_day,
                sex,
                (byte) (height & 0xff),
                (byte) ((height >> 8) & 0xff),
                (byte) ((weight * 200) & 0xff),
                (byte) (((weight * 200) >> 8) & 0xff),
                (byte) (userid & 0xff),
                (byte) ((userid >> 8) & 0xff),
                (byte) ((userid >> 16) & 0xff),
                (byte) ((userid >> 24) & 0xff)
        };

        transaction.write(characteristic, bytes);
        return this;
    }

    /**
     * Part of device initialization process. Do not call manually.
     *
     * @param builder
     * @return
     */
    private HuamiSupport setWearLocation(TransactionBuilder builder) {
        LOG.info("Attempting to set wear location...");
        BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_8_USER_SETTINGS);
        if (characteristic != null) {
            builder.notify(characteristic, true);
            int location = MiBandCoordinator.getWearLocation(getDevice().getAddress());
            switch (location) {
                case 0: // left hand
                    builder.write(characteristic, HuamiService.WEAR_LOCATION_LEFT_WRIST);
                    break;
                case 1: // right hand
                    builder.write(characteristic, HuamiService.WEAR_LOCATION_RIGHT_WRIST);
                    break;
            }
            builder.notify(characteristic, false); // TODO: this should actually be in some kind of finally-block in the queue. It should also be sent asynchronously after the notifications have completely arrived and processed.
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
        try {
            int minuteInterval = seconds / 60;
            minuteInterval = Math.min(minuteInterval, 120);
            minuteInterval = Math.max(0,minuteInterval);
            TransactionBuilder builder = performInitialized("set heart rate interval to: " + minuteInterval + " minutes");
            setHeartrateMeasurementInterval(builder, minuteInterval);
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
    private HuamiSupport setHeartrateSleepSupport(TransactionBuilder builder) {
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(getDevice().getAddress());
        if (characteristicHRControlPoint != null) {
            builder.notify(characteristicHRControlPoint, true);
            if (enableHrSleepSupport) {
                LOG.info("Enabling heartrate sleep support...");
                builder.write(characteristicHRControlPoint, HuamiService.COMMAND_ENABLE_HR_SLEEP_MEASUREMENT);
            } else {
                LOG.info("Disabling heartrate sleep support...");
                builder.write(characteristicHRControlPoint, HuamiService.COMMAND_DISABLE_HR_SLEEP_MEASUREMENT);
            }
            builder.notify(characteristicHRControlPoint, false); // TODO: this should actually be in some kind of finally-block in the queue. It should also be sent asynchronously after the notifications have completely arrived and processed.
        }
        return this;
    }

    private HuamiSupport setHeartrateMeasurementInterval(TransactionBuilder builder, int minutes) {
        if (characteristicHRControlPoint != null) {
            builder.notify(characteristicHRControlPoint, true);
            LOG.info("Setting heart rate measurement interval to " + minutes + " minutes");
            builder.write(characteristicHRControlPoint, new byte[]{HuamiService.COMMAND_SET_PERIODIC_HR_MEASUREMENT_INTERVAL, (byte) minutes});
            builder.notify(characteristicHRControlPoint, false); // TODO: this should actually be in some kind of finally-block in the queue. It should also be sent asynchronously after the notifications have completely arrived and processed.
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

    protected void performPreferredNotification(String task, String notificationOrigin, SimpleNotification simpleNotification, int alertLevel, BtLEAction extraAction) {
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

            sendCustomNotification(profile, simpleNotification, flashTimes, flashColour, originalColour, flashDuration, extraAction, builder);

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
            BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION);
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
        if (notificationSpec.type == NotificationType.GENERIC_ALARM_CLOCK) {
            onAlarmClock(notificationSpec);
            return;
        }
        int alertLevel = HuamiService.ALERT_LEVEL_MESSAGE;
        if (notificationSpec.type == NotificationType.UNKNOWN) {
            alertLevel = HuamiService.ALERT_LEVEL_VIBRATE_ONLY;
        }
        String message = NotificationUtils.getPreferredTextFor(notificationSpec, 40, 40, getContext()).trim();
        String origin = notificationSpec.type.getGenericType();
        SimpleNotification simpleNotification = new SimpleNotification(message, BLETypeConversions.toAlertCategory(notificationSpec.type), notificationSpec.type);
        performPreferredNotification(origin + " received", origin, simpleNotification, alertLevel, null);
    }

    protected void onAlarmClock(NotificationSpec notificationSpec) {
        alarmClockRinging = true;
        AbortTransactionAction abortAction = new StopNotificationAction(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL)) {
            @Override
            protected boolean shouldAbort() {
                return !isAlarmClockRinging();
            }
        };
        String message = NotificationUtils.getPreferredTextFor(notificationSpec, 40, 40, getContext());
        SimpleNotification simpleNotification = new SimpleNotification(message, AlertCategory.HighPriorityAlert, notificationSpec.type);
        performPreferredNotification("alarm clock ringing", MiBandConst.ORIGIN_ALARM_CLOCK, simpleNotification, HuamiService.ALERT_LEVEL_VIBRATE_ONLY, abortAction);
    }

    @Override
    public void onDeleteNotification(int id) {
        alarmClockRinging = false; // we should have the notificationtype at least to check
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("Set date and time");
            setCurrentTimeWithService(builder);
            //TODO: once we have a common strategy for sending events (e.g. EventHandler), remove this call from here. Meanwhile it does no harm.
            sendCalendarEvents(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time on MI device", ex);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            telephoneRinging = true;
            AbortTransactionAction abortAction = new StopNotificationAction(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL)) {
                @Override
                protected boolean shouldAbort() {
                    return !isTelephoneRinging();
                }
            };
            String message = NotificationUtils.getPreferredTextFor(callSpec);
            SimpleNotification simpleNotification = new SimpleNotification(message, AlertCategory.IncomingCall, null);
            performPreferredNotification("incoming call", MiBandConst.ORIGIN_INCOMING_CALL, simpleNotification, HuamiService.ALERT_LEVEL_PHONE_CALL, abortAction);
        } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
            telephoneRinging = false;
            stopCurrentNotification();
        }
    }

    private void stopCurrentNotification() {
        try {
            TransactionBuilder builder = performInitialized("stop notification");
            getNotificationStrategy().stopCurrentNotification(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error stopping notification");
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
        if (bufferMusicStateSpec != stateSpec)
            bufferMusicStateSpec = stateSpec;
        sendMusicStateToDevice();

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (bufferMusicSpec != musicSpec)
            bufferMusicSpec = musicSpec;

        if (!isMusicAppStarted) {
            return;
        }
        sendMusicStateToDevice();
    }


    private void sendMusicStateToDevice() {


        if (characteristicChunked == null) {
            return;
        }
        if (bufferMusicSpec == null && bufferMusicStateSpec == null) {
            try {
                TransactionBuilder builder = performInitialized("send dummy playback info to enable music controls");
                writeToChunked(builder, 3, new byte[]{1, 0, 1, 0, 0, 0, 1, 0});
                builder.queue(getQueue());
            } catch (IOException e) {
                LOG.error("Unable to send dummy music controls");
            }
            return;
        }

        byte flags = 0x00;
        flags |= 0x01;
        int length = 8;
        if (bufferMusicSpec.track != null && bufferMusicSpec.track.getBytes().length > 0) {
            length += bufferMusicSpec.track.getBytes().length + 1;
            flags |= 0x02;
        }
        if (bufferMusicSpec.album != null && bufferMusicSpec.album.getBytes().length > 0) {
            length += bufferMusicSpec.album.getBytes().length + 1;
            flags |= 0x04;
        }
        if (bufferMusicSpec.artist != null && bufferMusicSpec.artist.getBytes().length > 0) {
            length += bufferMusicSpec.artist.getBytes().length + 1;
            flags |= 0x08;
        }


//        LOG.info("Music flags are: " + (flags & 0xff));
        try {
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(flags);
            byte state;
            switch (bufferMusicStateSpec.state) {
                case MusicStateSpec.STATE_PLAYING:
                    state = 1;
                    break;
                default:
                    state = 0;
            }

            buf.put(state);
            buf.put(new byte[]{0x1, 0x0, 0x0, 0x0}); //unknown
            buf.put(new byte[]{0x1,0x0}); //show track
//            buf.put(new byte[]{0x1,0x1}); //show album


            if (bufferMusicSpec.track != null && bufferMusicSpec.track.getBytes().length > 0) {
                buf.put(bufferMusicSpec.track.getBytes());
                buf.put((byte) 0);
            }
            if (bufferMusicSpec.album != null && bufferMusicSpec.album.getBytes().length > 0) {
                buf.put(bufferMusicSpec.album.getBytes());
                buf.put((byte) 0);
            }
            if (bufferMusicSpec.artist != null && bufferMusicSpec.artist.getBytes().length > 0) {
                buf.put(bufferMusicSpec.artist.getBytes());
                buf.put((byte) 0);
            }


            TransactionBuilder builder = performInitialized("send playback info");
            writeToChunked(builder, 3, buf.array());

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Unable to send playback state");
        }

//        LOG.info("Sent music: " + bufferMusicSpec.toString() + " " + bufferMusicStateSpec.toString());

    }

    @Override
    public void onReboot() {
        try {
            TransactionBuilder builder = performInitialized("Reboot");
            sendReboot(builder);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to reboot MI", ex);
        }
    }

    public HuamiSupport sendReboot(TransactionBuilder builder) {
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE), new byte[] { HuamiService.COMMAND_FIRMWARE_REBOOT});
        return this;
    }

    @Override
    public void onHeartRateTest() {
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.write(characteristicHRControlPoint, stopHeartMeasurementContinuous);
            builder.write(characteristicHRControlPoint, stopHeartMeasurementManual);
            builder.write(characteristicHRControlPoint, startHeartMeasurementManual);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read heart rate with MI2", ex);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("Enable realtime heart rate measurement");
            BluetoothGattCharacteristic heartrateCharacteristic = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT);
            if (heartrateCharacteristic != null) {
                builder.notify(heartrateCharacteristic, enable);
            }
            if (enable) {
                builder.write(characteristicHRControlPoint, stopHeartMeasurementManual);
                builder.write(characteristicHRControlPoint, startHeartMeasurementContinuous);
            } else {
                builder.write(characteristicHRControlPoint, stopHeartMeasurementContinuous);
            }
            builder.queue(getQueue());
            enableRealtimeSamplesTimer(enable);
        } catch (IOException ex) {
            LOG.error("Unable to enable realtime heart rate measurement", ex);
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
            TransactionBuilder builder = performInitialized(enable ? "Enabling realtime steps notifications" : "Disabling realtime steps notifications");
            if (enable) {
                builder.read(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS));
            }
            builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS), enable);
            builder.queue(getQueue());
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

    public void runButtonAction() {
        Prefs prefs = GBApplication.getPrefs();

        if (currentButtonTimerActivationTime != currentButtonPressTime) {
            return;
        }

        String requiredButtonPressMessage = prefs.getString(MiBandConst.PREF_MIBAND_BUTTON_PRESS_BROADCAST,
                this.getContext().getString(R.string.mi2_prefs_button_press_broadcast_default_value));

        Intent in = new Intent();
        in.setAction(requiredButtonPressMessage);
        in.putExtra("button_id", currentButtonActionId);
        LOG.info("Sending " + requiredButtonPressMessage + " with button_id " + currentButtonActionId);
        this.getContext().getApplicationContext().sendBroadcast(in);
        if (prefs.getBoolean(MiBandConst.PREF_MIBAND_BUTTON_ACTION_VIBRATE, false)) {
            performPreferredNotification(null, null, null, HuamiService.ALERT_LEVEL_VIBRATE_ONLY, null);
        }

        currentButtonActionId = 0;

        currentButtonPressCount = 0;
        currentButtonPressTime = System.currentTimeMillis();
    }

    public void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();

        switch (value[0]) {
            case HuamiDeviceEvent.CALL_REJECT:
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.CALL_IGNORE:
                LOG.info("ignore call (not yet supported)");
                //callCmd.event = GBDeviceEventCallControl.Event.IGNORE;
                //evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED:
                LOG.info("button pressed");
                handleButtonEvent();
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED_LONG:
                LOG.info("button long-pressed ");
                break;
            case HuamiDeviceEvent.START_NONWEAR:
                LOG.info("non-wear start detected");
                break;
            case HuamiDeviceEvent.ALARM_TOGGLED:
                LOG.info("An alarm was toggled");
                break;
            case HuamiDeviceEvent.FELL_ASLEEP:
                LOG.info("Fell asleep");
                break;
            case HuamiDeviceEvent.WOKE_UP:
                LOG.info("Woke up");
                break;
            case HuamiDeviceEvent.STEPSGOAL_REACHED:
                LOG.info("Steps goal reached");
                break;
            case HuamiDeviceEvent.TICK_30MIN:
                LOG.info("Tick 30 min (?)");
                break;
            case HuamiDeviceEvent.FIND_PHONE_START:
                LOG.info("find phone started");
                acknowledgeFindPhone(); // FIXME: premature
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case HuamiDeviceEvent.FIND_PHONE_STOP:
                LOG.info("find phone stopped");
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case HuamiDeviceEvent.MUSIC_CONTROL:
                LOG.info("got music control");
                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

                switch (value[1]) {
                    case 0:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 1:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 3:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 4:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case 5:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 6:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    case (byte) 224:
                        LOG.info("Music app started");
                        isMusicAppStarted = true;
                        sendMusicStateToDevice();
                        break;
                    case (byte) 225:
                        LOG.info("Music app terminated");
                        isMusicAppStarted = false;
                        break;
                    default:
                        LOG.info("unhandled music control event " + value[1]);
                        return;
                }
                evaluateGBDeviceEvent(deviceEventMusicControl);
                break;
            default:
                LOG.warn("unhandled event " + value[0]);
        }
    }

    private void acknowledgeFindPhone() {
        try {
            TransactionBuilder builder = performInitialized("acknowledge find phone");

            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), AmazfitBipService.COMMAND_ACK_FIND_PHONE_IN_PROGRESS);
            builder.queue(getQueue());
        } catch (Exception ex) {
            LOG.error("Error sending current weather", ex);
        }
    }

    public void handleButtonEvent() {
        ///logMessageContent(value);

        // If disabled we return from function immediately
        Prefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean(MiBandConst.PREF_MIBAND_BUTTON_ACTION_ENABLE, false)) {
            return;
        }

        int buttonPressMaxDelay = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_PRESS_MAX_DELAY, 2000);
        int buttonActionDelay = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_ACTION_DELAY, 0);
        int requiredButtonPressCount = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_PRESS_COUNT, 0);

        if (requiredButtonPressCount > 0) {
            long timeSinceLastPress = System.currentTimeMillis() - currentButtonPressTime;

            if ((currentButtonPressTime == 0) || (timeSinceLastPress < buttonPressMaxDelay)) {
                currentButtonPressCount++;
            }
            else {
                currentButtonPressCount = 1;
                currentButtonActionId = 0;
            }

            currentButtonPressTime = System.currentTimeMillis();
            if (currentButtonPressCount == requiredButtonPressCount) {
                currentButtonTimerActivationTime = currentButtonPressTime;
                if (buttonActionDelay > 0) {
                    LOG.info("Activating timer");
                    final Timer buttonActionTimer = new Timer("Mi Band Button Action Timer");
                    buttonActionTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            runButtonAction();
                            buttonActionTimer.cancel();
                        }
                    }, buttonActionDelay, buttonActionDelay);
                }
                else {
                    LOG.info("Activating button action");
                    runButtonAction();
                }
                currentButtonActionId++;
                currentButtonPressCount = 0;
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
            return true;
        } else if (MiBandService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            handleRealtimeSteps(characteristic.getValue());
            return true;
        } else if (GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            handleHeartrate(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            LOG.info("AUTHENTICATION?? " + characteristicUUID);
            logMessageContent(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT.equals(characteristicUUID)) {
            handleDeviceEvent(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS.equals(characteristicUUID)) {
            handleRealtimeSteps(characteristic.getValue());
            return true;
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
        if (GattCharacteristic.UUID_CHARACTERISTIC_GAP_DEVICE_NAME.equals(characteristicUUID)) {
            handleDeviceName(characteristic.getValue(), status);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
            return true;
        } else if (GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            logHeartrate(characteristic.getValue(), status);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS.equals(characteristicUUID)) {
            handleRealtimeSteps(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT.equals(characteristicUUID)) {
            handleDeviceEvent(characteristic.getValue());
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
        if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            LOG.info("KEY AES SEND");
            logMessageContent(characteristic.getValue());
            return true;
        }
        return false;
    }

    public void logHeartrate(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS && value != null) {
            LOG.info("Got heartrate:");
            if (value.length == 2 && value[0] == 0) {
                int hrValue = (value[1] & 0xff);
                GB.toast(getContext(), "Heart Rate measured: " + hrValue, Toast.LENGTH_LONG, GB.INFO);
            }
            return;
        }
        logMessageContent(value);
    }

    private void handleHeartrate(byte[] value) {
        if (value.length == 2 && value[0] == 0) {
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
        if (value == null) {
            LOG.error("realtime steps: value is null");
            return;
        }

        if (value.length == 13) {
            byte[] stepsValue = new byte[] {value[1], value[2]};
            int steps = BLETypeConversions.toUint16(stepsValue);
            if (LOG.isDebugEnabled()) {
                LOG.debug("realtime steps: " + steps);
            }
            getRealtimeSamplesSupport().setSteps(steps);
        } else {
            LOG.warn("Unrecognized realtime steps value: " + Logging.formatBytes(value));
        }
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
                        MiBand2SampleProvider provider = new MiBand2SampleProvider(gbDevice, session);
                        MiBandActivitySample sample = createActivitySample(device, user, ts, provider);
                        sample.setHeartRate(getHeartrateBpm());
                        sample.setSteps(getSteps());
                        sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                        sample.setRawKind(HuamiConst.TYPE_ACTIVITY); // to make it visible in the charts TODO: add a MANUAL kind for that?

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
        Calendar calendar = alarm.getAlarmCal();

        int maxAlarms = 5; // arbitrary at the moment...
        if (alarm.getIndex() >= maxAlarms) {
            if (alarm.isEnabled()) {
                GB.toast(getContext(), "Only 5 alarms are currently supported.", Toast.LENGTH_LONG, GB.WARN);
            }
            return;
        }

        int base = 0;
        if (alarm.isEnabled()) {
            base = 128;
        }
        int daysMask = alarm.getRepetitionMask();
        if (!alarm.isRepetitive()) {
            daysMask = 128;
        }
        byte[] alarmMessage = new byte[] {
                (byte) 0x2, // TODO what is this?
                (byte) (base + alarm.getIndex()), // 128 is the base, alarm slot is added
                (byte) calendar.get(Calendar.HOUR_OF_DAY),
                (byte) calendar.get(Calendar.MINUTE),
                (byte) daysMask,
        };
        builder.write(characteristic, alarmMessage);
        // TODO: react on 0x10, 0x02, 0x01 on notification (success)
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
//        if (getDeviceInfo().supportsHeartrate()) {
//            getDevice().addDeviceInfo(new GenericItem(
//                    getContext().getString(R.string.DEVINFO_HR_VER),
//                    info.getSoftwareRevision()));
//        }

        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        if (versionCmd.fwVersion == null) {
            versionCmd.fwVersion = info.getSoftwareRevision();
        }
        if (versionCmd.fwVersion != null && versionCmd.fwVersion.length() > 0 && versionCmd.fwVersion.charAt(0) == 'V') {
            versionCmd.fwVersion = versionCmd.fwVersion.substring(1);
        }
        handleGBDeviceEvent(versionCmd);
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            HuamiBatteryInfo info = new HuamiBatteryInfo(value);
            batteryCmd.level = ((short) info.getLevelInPercent());
            batteryCmd.state = info.getState();
            batteryCmd.lastChargeTime = info.getLastChargeTime();
            batteryCmd.numCharges = info.getNumCharges();
            handleGBDeviceEvent(batteryCmd);
        }
    }

    /**
     * Fetch the events from the android device calendars and set the alarms on the miband.
     * @param builder
     */
    private HuamiSupport sendCalendarEvents(TransactionBuilder builder) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION);

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
                Alarm alarm = GBAlarm.createSingleShot(slotToUse, false, calendar);
                queueAlarm(alarm, builder, characteristic);
                iteration++;
            }
        }
        return this;
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Sending configuration for option: " + config);
            switch (config) {
                case MiBandConst.PREF_MI2_DATEFORMAT:
                    setDateDisplay(builder);
                    break;
                case MiBandConst.PREF_MI2_GOAL_NOTIFICATION:
                    setGoalNotification(builder);
                    break;
                case MiBandConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
                case MiBandConst.PREF_DISPLAY_ON_LIFT_START:
                case MiBandConst.PREF_DISPLAY_ON_LIFT_END:
                    setActivateDisplayOnLiftWrist(builder);
                    break;
                case MiBandConst.PREF_MI2_DISPLAY_ITEMS:
                    setDisplayItems(builder);
                    break;
                case MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO:
                    setRotateWristToSwitchInfo(builder);
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    setFitnessGoal(builder);
                    break;
                case MiBandConst.PREF_MI2_DO_NOT_DISTURB:
                case MiBandConst.PREF_MI2_DO_NOT_DISTURB_START:
                case MiBandConst.PREF_MI2_DO_NOT_DISTURB_END:
                    setDoNotDisturb(builder);
                    break;
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_START:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_END:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_START:
                case MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_END:
                    setInactivityWarnings(builder);
                    break;
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                    setDistanceUnit(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onTestNewFunction() {
        try {
            new FetchSportsSummaryOperation(this).perform();
        } catch (IOException ex) {
            LOG.error("Unable to fetch MI activity data", ex);
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    private HuamiSupport setDateDisplay(TransactionBuilder builder) {
        DateTimeDisplay dateTimeDisplay = HuamiCoordinator.getDateDisplay(getContext());
        LOG.info("Setting date display to " + dateTimeDisplay);
        switch (dateTimeDisplay) {
            case TIME:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.DATEFORMAT_TIME);
                break;
            case DATE_TIME:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.DATEFORMAT_DATE_TIME);
                break;
        }
        return this;
    }

    private HuamiSupport setTimeFormat(TransactionBuilder builder) {
        boolean is24Format = DateFormat.is24HourFormat(getContext());
        LOG.info("Setting 24h time format to " + is24Format);
        if (is24Format) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.DATEFORMAT_TIME_24_HOURS);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.DATEFORMAT_TIME_12_HOURS);
        }
        return this;
    }

    private HuamiSupport setGoalNotification(TransactionBuilder builder) {
        boolean enable = HuamiCoordinator.getGoalNotification();
        LOG.info("Setting goal notification to " + enable);
        if (enable) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENABLE_GOAL_NOTIFICATION);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_GOAL_NOTIFICATION);
        }
        return this;
    }

    private HuamiSupport setActivateDisplayOnLiftWrist(TransactionBuilder builder) {
        ActivateDisplayOnLift displayOnLift = HuamiCoordinator.getActivateDisplayOnLiftWrist(getContext());
        LOG.info("Setting activate display on lift wrist to " + displayOnLift);

        switch (displayOnLift) {
            case ON:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST);
                break;
            case OFF:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST);
                break;
            case SCHEDULED:
                byte[] cmd = HuamiService.COMMAND_SCHEDULE_DISPLAY_ON_LIFT_WRIST.clone();

                Calendar calendar = GregorianCalendar.getInstance();

                Date start = HuamiCoordinator.getDisplayOnLiftStart();
                calendar.setTime(start);
                cmd[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[5] = (byte) calendar.get(Calendar.MINUTE);

                Date end = HuamiCoordinator.getDisplayOnLiftEnd();
                calendar.setTime(end);
                cmd[6] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[7] = (byte) calendar.get(Calendar.MINUTE);

                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), cmd);
        }
        return this;
    }

    protected HuamiSupport setDisplayItems(TransactionBuilder builder) {
        Set<String> pages = HuamiCoordinator.getDisplayItems();
        LOG.info("Setting display items to " + (pages == null ? "none" : pages));

        byte[] data = HuamiService.COMMAND_CHANGE_SCREENS.clone();

        if (pages != null) {
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_STEPS)) {
                data[HuamiService.SCREEN_CHANGE_BYTE] |= HuamiService.DISPLAY_ITEM_BIT_STEPS;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_DISTANCE)) {
                data[HuamiService.SCREEN_CHANGE_BYTE] |= HuamiService.DISPLAY_ITEM_BIT_DISTANCE;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_CALORIES)) {
                data[HuamiService.SCREEN_CHANGE_BYTE] |= HuamiService.DISPLAY_ITEM_BIT_CALORIES;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_HEART_RATE)) {
                data[HuamiService.SCREEN_CHANGE_BYTE] |= HuamiService.DISPLAY_ITEM_BIT_HEART_RATE;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_BATTERY)) {
                data[HuamiService.SCREEN_CHANGE_BYTE] |= HuamiService.DISPLAY_ITEM_BIT_BATTERY;
            }
        }

        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), data);
        return this;
    }

    private HuamiSupport setRotateWristToSwitchInfo(TransactionBuilder builder) {
        boolean enable = HuamiCoordinator.getRotateWristToSwitchInfo();
        LOG.info("Setting rotate wrist to cycle info to " + enable);
        if (enable) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENABLE_ROTATE_WRIST_TO_SWITCH_INFO);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_ROTATE_WRIST_TO_SWITCH_INFO);
        }
        return this;
    }

    private HuamiSupport setDisplayCaller(TransactionBuilder builder) {
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENABLE_DISPLAY_CALLER);
        return this;
    }

    private HuamiSupport setDoNotDisturb(TransactionBuilder builder) {
        DoNotDisturb doNotDisturb = HuamiCoordinator.getDoNotDisturb(getContext());
        LOG.info("Setting do not disturb to " + doNotDisturb);
        switch (doNotDisturb) {
            case OFF:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DO_NOT_DISTURB_OFF);
                break;
            case AUTOMATIC:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DO_NOT_DISTURB_AUTOMATIC);
                break;
            case SCHEDULED:
                byte[] data = HuamiService.COMMAND_DO_NOT_DISTURB_SCHEDULED.clone();

                Calendar calendar = GregorianCalendar.getInstance();

                Date start = HuamiCoordinator.getDoNotDisturbStart();
                calendar.setTime(start);
                data[HuamiService.DND_BYTE_START_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.DND_BYTE_START_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

                Date end = HuamiCoordinator.getDoNotDisturbEnd();
                calendar.setTime(end);
                data[HuamiService.DND_BYTE_END_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.DND_BYTE_END_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), data);

                break;
        }

        return this;
    }

    private HuamiSupport setInactivityWarnings(TransactionBuilder builder) {
        boolean enable = HuamiCoordinator.getInactivityWarnings();
        LOG.info("Setting inactivity warnings to " + enable);

        if (enable) {
            byte[] data = HuamiService.COMMAND_ENABLE_INACTIVITY_WARNINGS.clone();

            int threshold = HuamiCoordinator.getInactivityWarningsThreshold();
            data[HuamiService.INACTIVITY_WARNINGS_THRESHOLD] = (byte) threshold;

            Calendar calendar = GregorianCalendar.getInstance();

            boolean enableDnd = HuamiCoordinator.getInactivityWarningsDnd();

            Date intervalStart = HuamiCoordinator.getInactivityWarningsStart();
            Date intervalEnd = HuamiCoordinator.getInactivityWarningsEnd();
            Date dndStart = HuamiCoordinator.getInactivityWarningsDndStart();
            Date dndEnd = HuamiCoordinator.getInactivityWarningsDndEnd();

            // The first interval always starts when the warnings interval starts
            calendar.setTime(intervalStart);
            data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_START_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
            data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_START_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

            if(enableDnd) {
                // The first interval ends when the dnd interval starts
                calendar.setTime(dndStart);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_END_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_END_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

                // The second interval starts when the dnd interval ends
                calendar.setTime(dndEnd);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_2_START_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_2_START_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

                // ... and it ends when the warnings interval ends
                calendar.setTime(intervalEnd);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_2_END_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_2_END_MINUTES] = (byte) calendar.get(Calendar.MINUTE);
            } else {
                // No Dnd, use the first interval
                calendar.setTime(intervalEnd);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_END_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.INACTIVITY_WARNINGS_INTERVAL_1_END_MINUTES] = (byte) calendar.get(Calendar.MINUTE);
            }

            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), data);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_INACTIVITY_WARNINGS);
        }

        return this;
    }

    private HuamiSupport setDistanceUnit(TransactionBuilder builder) {
        MiBandConst.DistanceUnit unit = HuamiCoordinator.getDistanceUnit();
        LOG.info("Setting distance unit to " + unit);
        if (unit == MiBandConst.DistanceUnit.METRIC) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISTANCE_UNIT_METRIC);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISTANCE_UNIT_IMPERIAL);
        }
        return this;
    }

    protected void writeToChunked(TransactionBuilder builder, int type, byte[] data) {
        final int MAX_CHUNKLENGTH = 17;
        int remaining = data.length;
        byte count = 0;
        while (remaining > 0) {
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + 3];

            byte flags = 0;
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x80; // last chunk
                if (count == 0) {
                    flags |= 0x40; // weird but true
                }
            } else if (count > 0) {
                flags |= 0x40; // consecutive chunk
            }

            chunk[0] = 0;
            chunk[1] = (byte) (flags | type);
            chunk[2] = (byte) (count & 0xff);

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 3, copybytes);
            builder.write(characteristicChunked, chunk);
            remaining -= copybytes;
        }
    }

    public void phase2Initialize(TransactionBuilder builder) {
        LOG.info("phase2Initialize...");
        requestBatteryInfo(builder);
    }

    public void phase3Initialize(TransactionBuilder builder) {
        LOG.info("phase3Initialize...");
        setDateDisplay(builder);
        setTimeFormat(builder);
        setUserInfo(builder);
        setDistanceUnit(builder);
        setWearLocation(builder);
        setFitnessGoal(builder);
        setDisplayItems(builder);
        setDoNotDisturb(builder);
        setRotateWristToSwitchInfo(builder);
        setActivateDisplayOnLiftWrist(builder);
        setDisplayCaller(builder);
        setGoalNotification(builder);
        setInactivityWarnings(builder);
        setHeartrateSleepSupport(builder);
        setHeartrateMeasurementInterval(builder, getHeartRateMeasurementInterval());
    }

    private int getHeartRateMeasurementInterval() {
        return GBApplication.getPrefs().getInt("heartrate_measurement_interval", 0) / 60;
    }

    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand2FWHelper(uri, context);
    }
}
