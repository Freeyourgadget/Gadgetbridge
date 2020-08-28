/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Christian
    Fischer, Daniele Gobbetti, JohnnySun, José Rebelo, Julien Pivotto, Kasha,
    Michal Novotny, Sebastian Kranz, Sergey Trofimov, Steffen Liebergeld, vanous

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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cyanogenmod.weather.util.WeatherUtils;
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
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLift;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.DisconnectNotificationSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband2.MiBand2FWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Service;
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
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.NewAlert;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.actions.StopNotificationAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.Mi2NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.Mi2TextNotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.InitOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.NotificationStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.RealtimeSamplesSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_RESERVER_ALARMS_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SYNC_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WEARLOCATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_SELECTION_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_BROADCAST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefIntValue;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefStringValue;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL;

public class HuamiSupport extends AbstractBTLEDeviceSupport {

    // We introduce key press counter for notification purposes
    private static int currentButtonActionId = 0;
    private static int currentButtonPressCount = 0;
    private static long currentButtonPressTime = 0;
    private static long currentButtonTimerActivationTime = 0;
    private Timer buttonActionTimer = null;

    private static final Logger LOG = LoggerFactory.getLogger(HuamiSupport.class);
    private final DeviceInfoProfile<HuamiSupport> deviceInfoProfile;
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String s = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(s)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    private BluetoothGattCharacteristic characteristicHRControlPoint;
    private BluetoothGattCharacteristic characteristicChunked;

    private boolean needsAuth;
    private volatile boolean telephoneRinging;
    private volatile boolean isLocatingDevice;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

    private RealtimeSamplesSupport realtimeSamplesSupport;
    private boolean alarmClockRinging;

    protected boolean isMusicAppStarted = false;
    protected MusicSpec bufferMusicSpec = null;
    protected MusicStateSpec bufferMusicStateSpec = null;
    private boolean heartRateNotifyEnabled;
    private int mMTU = 23;
    protected int mActivitySampleSize = 4;

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
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        try {
            byte authFlags = getAuthFlags();
            byte cryptFlags = getCryptFlags();
            heartRateNotifyEnabled = false;
            boolean authenticate = needsAuth && (cryptFlags == 0x00);
            needsAuth = false;
            new InitOperation(authenticate, authFlags, cryptFlags, this, builder).perform();
            characteristicHRControlPoint = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT);
            characteristicChunked = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER);
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Huami device failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        return builder;
    }

    protected byte getAuthFlags() {
        return HuamiService.AUTH_BYTE;
    }

    public byte getCryptFlags() {
        return 0x00;
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
            bytes = BLETypeConversions.shortCalendarToRawBytes(calendar);
        } else if (precision == TimeUnit.SECONDS) {
            bytes = BLETypeConversions.calendarToRawBytes(calendar);
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
        GregorianCalendar timestamp = BLETypeConversions.rawBytesToCalendar(bytes);
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
        builder.add(new SetDeviceStateAction(gbDevice, State.INITIALIZED, getContext()));
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
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUDIO), enable);
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUDIODATA), enable);
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
        return connect();
    }

    private HuamiSupport sendDefaultNotification(TransactionBuilder builder, SimpleNotification simpleNotification, short repeat, BtLEAction extraAction) {
        LOG.info("Sending notification to MiBand: (" + repeat + " times)");
        NotificationStrategy strategy = getNotificationStrategy();
        for (short i = 0; i < repeat; i++) {
            strategy.sendDefaultNotification(builder, simpleNotification, extraAction);
        }
        return this;
    }

    public NotificationStrategy getNotificationStrategy() {
        String firmwareVersion = gbDevice.getFirmwareVersion();
        if (firmwareVersion != null) {
            Version ver = new Version(firmwareVersion);
            if (MiBandConst.MI2_FW_VERSION_MIN_TEXT_NOTIFICATIONS.compareTo(ver) > 0) {
                return new Mi2NotificationStrategy(this);
            }
        }
        if (GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(MiBandConst.PREF_MI2_ENABLE_TEXT_NOTIFICATIONS, true)) {
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
            int fitnessGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
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
        byte[] bytes = new byte[]{
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
            int location = MiBandCoordinator.getWearLocation(gbDevice.getAddress());
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
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(gbDevice.getAddress());
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

    private void performPreferredNotification(String task, String notificationOrigin, SimpleNotification simpleNotification, int alertLevel, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            Prefs prefs = GBApplication.getPrefs();
            short vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            VibrationProfile profile = getPreferredVibrateProfile(notificationOrigin, prefs, vibrateTimes);
            profile.setAlertLevel(alertLevel);

            getNotificationStrategy().sendCustomNotification(profile, simpleNotification, 0, 0, 0, 0, extraAction, builder);

            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to device", ex);
        }
    }

    private short getPreferredVibrateCount(String notificationOrigin, Prefs prefs) {
        return (short) Math.min(Short.MAX_VALUE, getNotificationPrefIntValue(VIBRATION_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_COUNT));
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


    /*
     This works on all Huami devices except Mi Band 2
     */
    protected void sendNotificationNew(NotificationSpec notificationSpec, boolean hasExtraHeader) {
        sendNotificationNew(notificationSpec, hasExtraHeader, 230);
    }
    protected void sendNotificationNew(NotificationSpec notificationSpec, boolean hasExtraHeader, int maxLength) {
        if (notificationSpec.type == NotificationType.GENERIC_ALARM_CLOCK) {
            onAlarmClock(notificationSpec);
            return;
        }

        String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        String message = StringUtils.truncate(senderOrTitle, 32) + "\0";
        if (notificationSpec.subject != null) {
            message += StringUtils.truncate(notificationSpec.subject, 128) + "\n\n";
        }
        if (notificationSpec.body != null) {
            message += StringUtils.truncate(notificationSpec.body, 512);
        }

        try {
            TransactionBuilder builder = performInitialized("new notification");

            byte customIconId = HuamiIcon.mapToIconId(notificationSpec.type);
            AlertCategory alertCategory = AlertCategory.CustomHuami;

            // The SMS icon for AlertCategory.SMS is unique and not available as iconId
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                alertCategory = AlertCategory.SMS;
            }
            // EMAIL icon does not work in FW 0.0.8.74, it did in 0.0.7.90
            else if (customIconId == HuamiIcon.EMAIL) {
                alertCategory = AlertCategory.Email;
            }

            if (characteristicChunked != null) {
                int prefixlength = 2;

                // We also need a (fake) source name for Mi Band 3 for SMS/EMAIL, else the message is not displayed
                byte[] appSuffix = "\0 \0".getBytes();
                int suffixlength = appSuffix.length;

                if (alertCategory == AlertCategory.CustomHuami) {
                    String appName;
                    prefixlength = 3;
                    final PackageManager pm = getContext().getPackageManager();
                    ApplicationInfo ai = null;
                    try {
                        ai = pm.getApplicationInfo(notificationSpec.sourceAppId, 0);
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }

                    if (ai != null) {
                        appName = "\0" + pm.getApplicationLabel(ai) + "\0";
                    } else {
                        appName = "\0" + "UNKNOWN" + "\0";
                    }
                    appSuffix = appName.getBytes();
                    suffixlength = appSuffix.length;
                }
                if (hasExtraHeader) {
                    prefixlength += 4;
                }

                byte[] rawmessage = message.getBytes();
                int length = Math.min(rawmessage.length, maxLength - prefixlength);
                if (length < rawmessage.length) {
                    length = StringUtils.utf8ByteLength(message, length);
                }

                byte[] command = new byte[length + prefixlength + suffixlength];
                int pos = 0;
                command[pos++] = (byte) alertCategory.getId();
                if (hasExtraHeader) {
                    command[pos++] = 0; // TODO
                    command[pos++] = 0;
                    command[pos++] = 0;
                    command[pos++] = 0;
                }
                command[pos++] = 1;
                if (alertCategory == AlertCategory.CustomHuami) {
                    command[pos] = customIconId;
                }

                System.arraycopy(rawmessage, 0, command, prefixlength, length);
                System.arraycopy(appSuffix, 0, command, prefixlength + length, appSuffix.length);

                writeToChunked(builder, 0, command);
            } else {
                AlertNotificationProfile<?> profile = new AlertNotificationProfile(this);
                NewAlert alert = new NewAlert(alertCategory, 1, message, customIconId);
                profile.setMaxLength(maxLength);
                profile.newAlert(builder, alert);
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to device", ex);
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
        AbortTransactionAction abortAction = new StopNotificationAction(getCharacteristic(UUID_CHARACTERISTIC_ALERT_LEVEL)) {
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
            // = we should genaralize the pebble calender code
            if (characteristicChunked == null) { // all except Mi Band 2
                sendCalendarEvents(builder);
            }
            else {
                sendCalendarEventsAsReminder(builder);
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set time on Huami device", ex);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            telephoneRinging = true;
            AbortTransactionAction abortAction = new StopNotificationAction(getCharacteristic(UUID_CHARACTERISTIC_ALERT_LEVEL)) {
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
            stopCurrentCallNotification();
        }
    }

    private void stopCurrentCallNotification() {
        try {
            TransactionBuilder builder = performInitialized("stop notification");
            getNotificationStrategy().stopCurrentNotification(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error stopping call notification");
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
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        if (!coordinator.supportsMusicInfo()) {
            return;
        }

        if (stateSpec != null && !stateSpec.equals(bufferMusicStateSpec)) {
            bufferMusicStateSpec = stateSpec;
            if (isMusicAppStarted) {
                sendMusicStateToDevice(null, bufferMusicStateSpec);
            }
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        if (!coordinator.supportsMusicInfo()) {
            return;
        }

        if (musicSpec != null && !musicSpec.equals(bufferMusicSpec)) {
            bufferMusicSpec = musicSpec;
            if (bufferMusicStateSpec != null) {
                bufferMusicStateSpec.state = 0;
                bufferMusicStateSpec.position = 0;
            }
            if (isMusicAppStarted) {
                sendMusicStateToDevice(bufferMusicSpec, bufferMusicStateSpec);
            }
        }
    }


    private void sendMusicStateToDevice() {
        sendMusicStateToDevice(bufferMusicSpec, bufferMusicStateSpec);
    }

    protected void sendMusicStateToDevice(MusicSpec musicSpec, MusicStateSpec musicStateSpec) {
        if (characteristicChunked == null) {
            return;
        }

        if (musicStateSpec == null) {
            return;
        }

        byte flags = 0x00;
        flags |= 0x01;
        int length = 5;
        if (musicSpec != null) {
            if (musicSpec.artist != null && musicSpec.artist.getBytes().length > 0) {
                length += musicSpec.artist.getBytes().length + 1;
                flags |= 0x02;
            }
            if (musicSpec.album != null && musicSpec.album.getBytes().length > 0) {
                length += musicSpec.album.getBytes().length + 1;
                flags |= 0x04;
            }
            if (musicSpec.track != null && musicSpec.track.getBytes().length > 0) {
                length += musicSpec.track.getBytes().length + 1;
                flags |= 0x08;
            }
            if (musicSpec.duration != 0) {
                length += 2;
                flags |= 0x10;
            }
        }

        try {
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(flags);
            byte state;
            switch (musicStateSpec.state) {
                case MusicStateSpec.STATE_PLAYING:
                    state = 1;
                    break;
                default:
                    state = 0;
            }

            buf.put(state);
            buf.put((byte) 0);
            buf.putShort((short) musicStateSpec.position);

            if (musicSpec != null) {
                if (musicSpec.artist != null && musicSpec.artist.getBytes().length > 0) {
                    buf.put(musicSpec.artist.getBytes());
                    buf.put((byte) 0);
                }
                if (musicSpec.album != null && musicSpec.album.getBytes().length > 0) {
                    buf.put(musicSpec.album.getBytes());
                    buf.put((byte) 0);
                }
                if (musicSpec.track != null && musicSpec.track.getBytes().length > 0) {
                    buf.put(musicSpec.track.getBytes());
                    buf.put((byte) 0);
                }
                if (musicSpec.duration != 0) {
                    buf.putShort((short) musicSpec.duration);
                }
            }

            TransactionBuilder builder = performInitialized("send playback info");
            writeToChunked(builder, 3, buf.array());

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Unable to send playback state");
        }
        LOG.info("sendMusicStateToDevice: " + musicSpec + " " + musicStateSpec);
    }

    @Override
    public void onReset(int flags) {
        try {
            TransactionBuilder builder = performInitialized("Reset");
            if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
                sendFactoryReset(builder);
            } else {
                sendReboot(builder);
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to reset", ex);
        }
    }

    public HuamiSupport sendReboot(TransactionBuilder builder) {
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE), new byte[] { HuamiService.COMMAND_FIRMWARE_REBOOT});
        return this;
    }

    public HuamiSupport sendFactoryReset(TransactionBuilder builder) {
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_FACTORY_RESET);
        return this;
    }

    @Override
    public void onHeartRateTest() {
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            enableNotifyHeartRateMeasurements(true, builder);
            builder.write(characteristicHRControlPoint, stopHeartMeasurementContinuous);
            builder.write(characteristicHRControlPoint, stopHeartMeasurementManual);
            builder.write(characteristicHRControlPoint, startHeartMeasurementManual);
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to read heart rate from Huami device", ex);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("Enable realtime heart rate measurement");
            enableNotifyHeartRateMeasurements(enable, builder);
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

    private void enableNotifyHeartRateMeasurements(boolean enable, TransactionBuilder builder) {
        if (heartRateNotifyEnabled != enable) {
            BluetoothGattCharacteristic heartrateCharacteristic = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT);
            if (heartrateCharacteristic != null) {
                builder.notify(heartrateCharacteristic, enable);
                heartRateNotifyEnabled = enable;
            }
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
            LOG.error("Unable to fetch activity data", ex);
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
        byte[] result = new byte[12];
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
            createUpdateFirmwareOperation(uri).perform();
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

    // this could go though onion code with preferrednotification, but I this should work on all huami devices
    private void vibrateOnce() {
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_CHARACTERISTIC_ALERT_LEVEL);
        try {
            TransactionBuilder builder = performInitialized("Vibrate once");
            builder.write(characteristic,new byte[] {3});
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("error while sending simple vibrate command", e);
        }
    }

    private void runButtonAction() {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        if (currentButtonTimerActivationTime != currentButtonPressTime) {
            return;
        }
        //handle user events settings. 0 is long press, rest are button_id 1-3
        switch (currentButtonActionId) {
            case 0:
                handleMediaButton(prefs.getString("button_long_press_action_selection","UNKNOWN"));
                break;
            case 1:
                handleMediaButton(prefs.getString("button_single_press_action_selection", "UNKNOWN"));
                break;
            case 2:
                handleMediaButton(prefs.getString("button_double_press_action_selection", "UNKNOWN"));
                break;
            case 3:
                handleMediaButton(prefs.getString("button_triple_press_action_selection", "UNKNOWN"));
                break;
            default:
                break;
        }

        String requiredButtonPressMessage = prefs.getString(HuamiConst.PREF_BUTTON_ACTION_BROADCAST,
                this.getContext().getString(R.string.mi2_prefs_button_press_broadcast_default_value));

        Intent in = new Intent();
        in.setAction(requiredButtonPressMessage);
        in.putExtra("button_id", currentButtonActionId);
        LOG.info("Sending " + requiredButtonPressMessage + " with button_id " + currentButtonActionId);
        this.getContext().getApplicationContext().sendBroadcast(in);

        if (prefs.getBoolean(HuamiConst.PREF_BUTTON_ACTION_VIBRATE, false)) {
            vibrateOnce();
        }

        currentButtonActionId = 0;
        currentButtonPressCount = 0;
        currentButtonPressTime = System.currentTimeMillis();
    }

    private void handleDeviceAction(String deviceAction, String message) {
        if (deviceAction.equals(PREF_DEVICE_ACTION_SELECTION_OFF)) {
            return;
        }
        if (deviceAction.equals(PREF_DEVICE_ACTION_SELECTION_BROADCAST)) {
            sendSystemBroadcast(message);
        }else {
            handleMediaButton(deviceAction);
        }
    }

    private void sendSystemBroadcast(String message){
        if (message !=null) {
            Intent in = new Intent();
            in.setAction(message);
            LOG.info("Sending broadcast " + message);
            this.getContext().getApplicationContext().sendBroadcast(in);
        }
    }
    private void handleMediaButton(String MediaAction) {
        if (MediaAction.equals(PREF_DEVICE_ACTION_SELECTION_OFF)) {
            return;
        }
        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.valueOf(MediaAction);
        evaluateGBDeviceEvent(deviceEventMusicControl);
    }

    private void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();

        switch (value[0]) {
            case HuamiDeviceEvent.CALL_REJECT:
                LOG.info("call rejected");
                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.CALL_IGNORE:
                LOG.info("call ignored");
                callCmd.event = GBDeviceEventCallControl.Event.IGNORE;
                evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED:
                LOG.info("button pressed");
                handleButtonEvent();
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED_LONG:
                LOG.info("button long-pressed ");
                handleLongButtonEvent();
                break;
            case HuamiDeviceEvent.START_NONWEAR:
                LOG.info("non-wear start detected");
                processDeviceEvent(HuamiDeviceEvent.START_NONWEAR);
                break;
            case HuamiDeviceEvent.ALARM_TOGGLED:
                LOG.info("An alarm was toggled");
                TransactionBuilder builder = new TransactionBuilder("requestAlarms");
                requestAlarms(builder);
                builder.queue(getQueue());
                break;
            case HuamiDeviceEvent.FELL_ASLEEP:
                LOG.info("Fell asleep");
                processDeviceEvent(HuamiDeviceEvent.FELL_ASLEEP);
                break;
            case HuamiDeviceEvent.WOKE_UP:
                LOG.info("Woke up");
                processDeviceEvent(HuamiDeviceEvent.WOKE_UP);
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
            case HuamiDeviceEvent.MTU_REQUEST:
                int mtu = (value[2] & 0xff) << 8 | value[1] & 0xff;
                LOG.info("device announced MTU of " + mtu);
                Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
                if (!prefs.getBoolean(PREF_ALLOW_HIGH_MTU, false)) {
                    break;
                }
                if (mtu < 23) {
                    LOG.error("Device announced unreasonable low MTU of " + mtu + ", ignoring");
                    break;
                }
                mMTU = mtu;
                /*
                 * not really sure if this would make sense, is this event already a proof of a successful MTU
                 * negotiation initiated by the Huami device, and acknowledged by the phone? do we really have to
                 * requestMTU() from our side after receiving this?
                 * /
                if (mMTU != mtu) {
                    requestMTU(mtu);
                }
                */
                break;
            default:
                LOG.warn("unhandled event " + value[0]);
        }
    }

    private void requestMTU(int mtu) {
        if (GBApplication.isRunningLollipopOrLater()) {
            new TransactionBuilder("requestMtu")
                    .requestMtu(mtu)
                    .queue(getQueue());
            mMTU = mtu;
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

    private void processDeviceEvent(int event){
        LOG.debug("Handling device event: " + event);
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        String deviceActionBroadcastMessage=null;

        switch (event) {
            case HuamiDeviceEvent.WOKE_UP:
                String wakeupAction = prefs.getString(PREF_DEVICE_ACTION_WOKE_UP_SELECTION,PREF_DEVICE_ACTION_SELECTION_OFF);
                if (wakeupAction.equals(PREF_DEVICE_ACTION_SELECTION_OFF)) return;
                deviceActionBroadcastMessage= prefs.getString(PREF_DEVICE_ACTION_WOKE_UP_BROADCAST,
                        this.getContext().getString(R.string.prefs_events_forwarding_wokeup_broadcast_default_value));
                handleDeviceAction(wakeupAction, deviceActionBroadcastMessage);
                break;
            case HuamiDeviceEvent.FELL_ASLEEP:
                String fellsleepAction = prefs.getString(PREF_DEVICE_ACTION_FELL_SLEEP_SELECTION,PREF_DEVICE_ACTION_SELECTION_OFF);
                if (fellsleepAction.equals(PREF_DEVICE_ACTION_SELECTION_OFF)) return;
                deviceActionBroadcastMessage= prefs.getString(PREF_DEVICE_ACTION_FELL_SLEEP_BROADCAST,
                        this.getContext().getString(R.string.prefs_events_forwarding_fellsleep_broadcast_default_value));
                handleDeviceAction(fellsleepAction, deviceActionBroadcastMessage);
                break;
            case HuamiDeviceEvent.START_NONWEAR:
                String nonwearAction = prefs.getString(PREF_DEVICE_ACTION_START_NON_WEAR_SELECTION,PREF_DEVICE_ACTION_SELECTION_OFF);
                if (nonwearAction.equals(PREF_DEVICE_ACTION_SELECTION_OFF)) return;
                deviceActionBroadcastMessage= prefs.getString(PREF_DEVICE_ACTION_START_NON_WEAR_BROADCAST,
                        this.getContext().getString(R.string.prefs_events_forwarding_startnonwear_broadcast_default_value));
                handleDeviceAction(nonwearAction, deviceActionBroadcastMessage);
                break;
        }

    }

    private void handleLongButtonEvent(){
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        if (!prefs.getBoolean(HuamiConst.PREF_BUTTON_ACTION_ENABLE, false)) {
            return;
        }

        currentButtonActionId = 0;
        currentButtonPressTime = System.currentTimeMillis();
        currentButtonTimerActivationTime = currentButtonPressTime;
        runButtonAction();

    }

    private void handleButtonEvent() {

        // If disabled we return from function immediately
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!prefs.getBoolean(HuamiConst.PREF_BUTTON_ACTION_ENABLE, false)) {
            return;
        }

        int buttonPressMaxDelay = prefs.getInt(HuamiConst.PREF_BUTTON_ACTION_PRESS_MAX_INTERVAL, 2000);
        int requiredButtonPressCount = prefs.getInt(HuamiConst.PREF_BUTTON_ACTION_PRESS_COUNT, 0);

        if (requiredButtonPressCount > 0) {
            long timeSinceLastPress = System.currentTimeMillis() - currentButtonPressTime;

            if ((currentButtonPressTime == 0) || (timeSinceLastPress < buttonPressMaxDelay)) {
                currentButtonPressCount++;
            } else {
                currentButtonPressCount = 1;
                currentButtonActionId = 0;
            }
            if (buttonActionTimer != null){
                buttonActionTimer.cancel();
            }

            currentButtonPressTime = System.currentTimeMillis();
            if (currentButtonPressCount == requiredButtonPressCount) {
                currentButtonTimerActivationTime = currentButtonPressTime;
                LOG.info("Activating button timer");
                buttonActionTimer = new Timer("Huami Button Action Timer");
                buttonActionTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        runButtonAction();
                        buttonActionTimer.cancel();
                    }
                }, buttonPressMaxDelay, buttonPressMaxDelay);

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
        } else if (HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION.equals(characteristicUUID)) {
            handleConfigurationInfo(characteristic.getValue());
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

    private void handleConfigurationInfo(byte[] value) {
        if (value == null || value.length < 4) {
            return;
        }
        if (value[0] == 0x10 && value[2] == 0x01) {
            if (value[1] == 0x0e) {
                String gpsVersion = new String(value, 3, value.length - 3);
                LOG.info("got gps version = " + gpsVersion);
                gbDevice.setFirmwareVersion2(gpsVersion);
            } else if (value[1] == 0x0d) {
                LOG.info("got alarms from watch");
                decodeAndUpdateAlarmStatus(value);
            } else {
                LOG.warn("got configuration info we do not handle yet " + GB.hexdump(value, 3, -1));
            }
        } else {
            LOG.warn("error received from configuration request " + GB.hexdump(value, 0, -1));
        }
    }

    private void decodeAndUpdateAlarmStatus(byte[] response) {
        List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> alarms = DBHelper.getAlarms(gbDevice);
        int maxAlarms = 10;
        boolean[] alarmsInUse = new boolean[maxAlarms];
        boolean[] alarmsEnabled = new boolean[maxAlarms];
        int nr_alarms = response[8];
        for (int i = 0; i < nr_alarms; i++) {
            byte alarm_data = response[9 + i];
            int index = alarm_data & 0xf;
            if (index >= maxAlarms) {
                GB.toast("Unexpected alarm index from device, ignoring: " + index, Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
            alarmsInUse[index] = true;
            boolean enabled = (alarm_data & 0x10) == 0x10;
            alarmsEnabled[index] = enabled;
            LOG.info("alarm " + index + " is enabled:" + enabled);
        }
        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : alarms) {
            boolean enabled = alarmsEnabled[alarm.getPosition()];
            boolean unused = !alarmsInUse[alarm.getPosition()];
            if (alarm.getEnabled() != enabled || alarm.getUnused() != unused) {
                LOG.info("updating alarm index " + alarm.getPosition() + " unused=" + unused + ", enabled=" + enabled);
                alarm.setEnabled(enabled);
                alarm.setUnused(unused);
                DBHelper.store(alarm);
                Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            }
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

                        Device device = DBHelper.getDevice(gbDevice, session);
                        User user = DBHelper.getUser(session);
                        int ts = (int) (System.currentTimeMillis() / 1000);
                        MiBand2SampleProvider provider = new MiBand2SampleProvider(gbDevice, session);
                        MiBandActivitySample sample = createActivitySample(device, user, ts, provider);
                        sample.setHeartRate(getHeartrateBpm());
//                        sample.setSteps(getSteps());
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
        Calendar calendar = AlarmUtils.toCalendar(alarm);

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        int maxAlarms = coordinator.getAlarmSlotCount();

        if (alarm.getPosition() >= maxAlarms) {
            if (alarm.getEnabled()) {
                GB.toast(getContext(), "Only " + maxAlarms + " alarms are currently supported.", Toast.LENGTH_LONG, GB.WARN);
            }
            return;
        }

        int actionMask = 0;
        int daysMask = 0;
        if (alarm.getEnabled() && !alarm.getUnused()) {
            actionMask = 0x80;

            if (coordinator.supportsAlarmSnoozing() && !alarm.getSnooze()) {
                actionMask |= 0x40;
            }
        }
        if (!alarm.getUnused()) {
            daysMask = alarm.getRepetition();
            if (!alarm.isRepetitive()) {
                daysMask = 128;
            }
        }

        byte[] alarmMessage = new byte[] {
                (byte) 0x2, // TODO what is this?
                (byte) (actionMask | alarm.getPosition()), // action mask + alarm slot
                (byte) calendar.get(Calendar.HOUR_OF_DAY),
                (byte) calendar.get(Calendar.MINUTE),
                (byte) daysMask,
        };
        builder.write(characteristic, alarmMessage);
        // TODO: react on 0x10, 0x02, 0x01 on notification (success)
    }

    protected void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
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

        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        int availableSlots = prefs.getInt(PREF_RESERVER_ALARMS_CALENDAR, 0);

        if (availableSlots > 0) {
            CalendarEvents upcomingEvents = new CalendarEvents();
            List<CalendarEvents.CalendarEvent> mEvents = upcomingEvents.getCalendarEventList(getContext());

            int iteration = 0;

            for (CalendarEvents.CalendarEvent mEvt : mEvents) {
                if (mEvt.isAllDay()) {
                    continue;
                }
                if (iteration >= availableSlots || iteration > 2) {
                    break;
                }
                int slotToUse = 2 - iteration;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(mEvt.getBegin());
                Alarm alarm = AlarmUtils.createSingleShot(slotToUse, false, true, calendar);
                queueAlarm(alarm, builder, characteristic);
                iteration++;
            }
        }
        return this;
    }

    private HuamiSupport sendCalendarEventsAsReminder(TransactionBuilder builder) {
        boolean syncCalendar = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(PREF_SYNC_CALENDAR, false);
        if (!syncCalendar) {
            return this;
        }

        CalendarEvents upcomingEvents = new CalendarEvents();
        List<CalendarEvents.CalendarEvent> calendarEvents = upcomingEvents.getCalendarEventList(getContext());
        Calendar calendar = Calendar.getInstance();

        int iteration = 0;

        for (CalendarEvents.CalendarEvent calendarEvent : calendarEvents) {
            if (calendarEvent.isAllDay()) {
                continue;
            }

            if (iteration > 8) { // limit ?
                break;
            }

            calendar.setTimeInMillis(calendarEvent.getBegin());
            byte[] title;
            byte[] body;
            if (calendarEvent.getTitle() != null) {
                title = calendarEvent.getTitle().getBytes();
            } else {
                title = new byte[]{};
            }
            if (calendarEvent.getDescription() != null) {
                body = calendarEvent.getDescription().getBytes();
            } else {
                body = new byte[]{};
            }

            int length = 18 + title.length + 1 + body.length + 1;
            ByteBuffer buf = ByteBuffer.allocate(length);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 0x0b); // always 0x0b?
            buf.put((byte) iteration); // îd
            buf.putInt(0x08 | 0x04 | 0x01); // flags 0x01 = enable, 0x04 = end date present, 0x08 = has text
            calendar.setTimeInMillis(calendarEvent.getBegin());
            buf.put(BLETypeConversions.shortCalendarToRawBytes(calendar));
            calendar.setTimeInMillis(calendarEvent.getEnd());
            buf.put(BLETypeConversions.shortCalendarToRawBytes(calendar));
            buf.put(title);
            buf.put((byte) 0); // 0 Terminated
            buf.put(body);
            buf.put((byte) 0); // 0 Terminated
            writeToChunked(builder, 2, buf.array());

            iteration++;
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
                case HuamiConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
                case HuamiConst.PREF_DISPLAY_ON_LIFT_START:
                case HuamiConst.PREF_DISPLAY_ON_LIFT_END:
                    setActivateDisplayOnLiftWrist(builder);
                    break;
                case HuamiConst.PREF_DISCONNECT_NOTIFICATION:
                case HuamiConst.PREF_DISCONNECT_NOTIFICATION_START:
                case HuamiConst.PREF_DISCONNECT_NOTIFICATION_END:
                    setDisconnectNotification(builder);
                    break;
                case HuamiConst.PREF_DISPLAY_ITEMS:
                    setDisplayItems(builder);
                    break;
                case HuamiConst.PREF_SHORTCUTS:
                    setShortcuts(builder);
                    break;
                case MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO:
                    setRotateWristToSwitchInfo(builder);
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    setFitnessGoal(builder);
                    break;
                case MiBandConst.PREF_DO_NOT_DISTURB:
                case MiBandConst.PREF_DO_NOT_DISTURB_START:
                case MiBandConst.PREF_DO_NOT_DISTURB_END:
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
                case MiBandConst.PREF_SWIPE_UNLOCK:
                    setBandScreenUnlock(builder);
                    break;
                case PREF_TIMEFORMAT:
                    setTimeFormat(builder);
                    break;
                case PREF_DATEFORMAT:
                    setDateFormat(builder);
                    break;
                case PREF_LANGUAGE:
                    setLanguage(builder);
                    break;
                case HuamiConst.PREF_EXPOSE_HR_THIRDPARTY:
                    setExposeHRThridParty(builder);
                    break;
                case PREF_WEARLOCATION:
                    setWearLocation(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        // FIXME: currently HuamiSupport *is* MiBand2 support, so return if we are using Mi Band 2
        if (gbDevice.getType() == DeviceType.MIBAND2) {
            return;
        }

        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, so not sending weather info");
            return;
        }
        boolean supportsConditionString = true;

        Version version = new Version(gbDevice.getFirmwareVersion());
        if (gbDevice.getType() == DeviceType.AMAZFITBIP && version.compareTo(new Version("0.0.8.74")) < 0) {
            supportsConditionString = false;
        }

        MiBandConst.DistanceUnit unit = HuamiCoordinator.getDistanceUnit();
        int tz_offset_hours = SimpleTimeZone.getDefault().getOffset(weatherSpec.timestamp * 1000L) / (1000 * 60 * 60);
        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending current temp");

            byte condition = HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode);

            int length = 8;
            if (supportsConditionString) {
                length += weatherSpec.currentCondition.getBytes().length + 1;
            }
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            buf.put((byte) 2);
            buf.putInt(weatherSpec.timestamp);
            buf.put((byte) (tz_offset_hours * 4));
            buf.put(condition);

            int currentTemp = weatherSpec.currentTemp - 273;
            if (unit == MiBandConst.DistanceUnit.IMPERIAL) {
                currentTemp = (int) WeatherUtils.celsiusToFahrenheit(currentTemp);
            }
            buf.put((byte) currentTemp);

            if (supportsConditionString) {
                buf.put(weatherSpec.currentCondition.getBytes());
                buf.put((byte) 0);
            }

            if (characteristicChunked != null) {
                writeToChunked(builder, 1, buf.array());
            } else {
                builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            }

            builder.queue(getQueue());
        } catch (Exception ex) {
            LOG.error("Error sending current weather", ex);
        }

        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending air quality index");
            int length = 8;
            String aqiString = "(n/a)";
            if (supportsConditionString) {
                length += aqiString.getBytes().length + 1;
            }
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 4);
            buf.putInt(weatherSpec.timestamp);
            buf.put((byte) (tz_offset_hours * 4));
            buf.putShort((short) -1);
            if (supportsConditionString) {
                buf.put(aqiString.getBytes());
                buf.put((byte) 0);
            }

            if (characteristicChunked != null) {
                writeToChunked(builder, 1, buf.array());
            } else {
                builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            }

            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Error sending air quality");
        }

        try {
            TransactionBuilder builder = performInitialized("Sending weather forecast");
            if (weatherSpec.forecasts.size() > 6) { //TDOD: find out the limits for each device
                weatherSpec.forecasts.subList(6, weatherSpec.forecasts.size()).clear();
            }
            final byte NR_DAYS = (byte) (1 + weatherSpec.forecasts.size());
            int bytesPerDay = 4;

            int conditionsLength = 0;
            if (supportsConditionString) {
                bytesPerDay = 5;
                conditionsLength = weatherSpec.currentCondition.getBytes().length;
                for (WeatherSpec.Forecast forecast : weatherSpec.forecasts) {
                    conditionsLength += Weather.getConditionString(forecast.conditionCode).getBytes().length;
                }
            }

            int length = 7 + bytesPerDay * NR_DAYS + conditionsLength;
            ByteBuffer buf = ByteBuffer.allocate(length);

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 1);
            buf.putInt(weatherSpec.timestamp);
            buf.put((byte) (tz_offset_hours * 4));

            buf.put(NR_DAYS);

            byte condition = HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode);
            buf.put(condition);
            buf.put(condition);

            int todayMaxTemp = weatherSpec.todayMaxTemp - 273;
            int todayMinTemp = weatherSpec.todayMinTemp - 273;
            if (unit == MiBandConst.DistanceUnit.IMPERIAL) {
                todayMaxTemp = (int) WeatherUtils.celsiusToFahrenheit(todayMaxTemp);
                todayMinTemp = (int) WeatherUtils.celsiusToFahrenheit(todayMinTemp);
            }
            buf.put((byte) todayMaxTemp);
            buf.put((byte) todayMinTemp);

            if (supportsConditionString) {
                buf.put(weatherSpec.currentCondition.getBytes());
                buf.put((byte) 0);
            }

            for (WeatherSpec.Forecast forecast : weatherSpec.forecasts) {
                condition = HuamiWeatherConditions.mapToAmazfitBipWeatherCode(forecast.conditionCode);
                buf.put(condition);
                buf.put(condition);

                int forecastMaxTemp = forecast.maxTemp - 273;
                int forecastMinTemp = forecast.minTemp - 273;
                if (unit == MiBandConst.DistanceUnit.IMPERIAL) {
                    forecastMaxTemp = (int) WeatherUtils.celsiusToFahrenheit(forecastMaxTemp);
                    forecastMinTemp = (int) WeatherUtils.celsiusToFahrenheit(forecastMinTemp);
                }
                buf.put((byte) forecastMaxTemp);
                buf.put((byte) forecastMinTemp);

                if (supportsConditionString) {
                    buf.put(Weather.getConditionString(forecast.conditionCode).getBytes());
                    buf.put((byte) 0);
                }
            }

            if (characteristicChunked != null) {
                writeToChunked(builder, 1, buf.array());
            } else {
                builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            }

            builder.queue(getQueue());
        } catch (Exception ex) {
            LOG.error("Error sending weather forecast", ex);
        }

        try {
            TransactionBuilder builder;
            builder = performInitialized("Sending forecast location");

            int length = 2 + weatherSpec.location.getBytes().length;
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put((byte) 8);
            buf.put(weatherSpec.location.getBytes());
            buf.put((byte) 0);


            if (characteristicChunked != null) {
                writeToChunked(builder, 1, buf.array());
            } else {
                builder.write(getCharacteristic(AmazfitBipService.UUID_CHARACTERISTIC_WEATHER), buf.array());
            }

            builder.queue(getQueue());
        } catch (Exception ex) {
            LOG.error("Error sending current forecast location", ex);
        }
    }

    private HuamiSupport setDateDisplay(TransactionBuilder builder) {
        DateTimeDisplay dateTimeDisplay = HuamiCoordinator.getDateDisplay(getContext(), gbDevice.getAddress());
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

    protected HuamiSupport setDateFormat(TransactionBuilder builder) {
        String dateFormat = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("dateformat", "MM/dd/yyyy");
        if (dateFormat == null) {
            return null;
        }
        switch (dateFormat) {
            case "MM/dd/yyyy":
            case "dd.MM.yyyy":
            case "dd/MM/yyyy":
                byte[] command = HuamiService.DATEFORMAT_DATE_MM_DD_YYYY;
                System.arraycopy(dateFormat.getBytes(), 0, command, 3, 10);
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), command);
                break;
            default:
                LOG.warn("unsupported date format " + dateFormat);
        }

        return this;
    }

    private HuamiSupport setTimeFormat(TransactionBuilder builder) {
        GBPrefs gbPrefs = new GBPrefs(new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())));
        String timeFormat = gbPrefs.getTimeFormat();

        LOG.info("Setting time format to " + timeFormat);
        if (timeFormat.equals("24h")) {
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
        ActivateDisplayOnLift displayOnLift = HuamiCoordinator.getActivateDisplayOnLiftWrist(getContext(), gbDevice.getAddress());
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

                Date start = HuamiCoordinator.getDisplayOnLiftStart(gbDevice.getAddress());
                calendar.setTime(start);
                cmd[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[5] = (byte) calendar.get(Calendar.MINUTE);

                Date end = HuamiCoordinator.getDisplayOnLiftEnd(gbDevice.getAddress());
                calendar.setTime(end);
                cmd[6] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[7] = (byte) calendar.get(Calendar.MINUTE);

                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), cmd);
        }
        return this;
    }

    protected HuamiSupport setDisplayItems(TransactionBuilder builder) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        Set<String> pages = prefs.getStringSet(HuamiConst.PREF_DISPLAY_ITEMS, new HashSet<>(Arrays.asList(getContext().getResources().getStringArray(R.array.pref_mi2_display_items_default))));

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
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), data);
        }

        return this;
    }

    protected HuamiSupport setShortcuts(TransactionBuilder builder) {
        return this;
    }

    private HuamiSupport setRotateWristToSwitchInfo(TransactionBuilder builder) {
        boolean enable = HuamiCoordinator.getRotateWristToSwitchInfo(gbDevice.getAddress());
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
        DoNotDisturb doNotDisturb = HuamiCoordinator.getDoNotDisturb(gbDevice.getAddress());
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

                Date start = HuamiCoordinator.getDoNotDisturbStart(gbDevice.getAddress());
                calendar.setTime(start);
                data[HuamiService.DND_BYTE_START_HOURS] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[HuamiService.DND_BYTE_START_MINUTES] = (byte) calendar.get(Calendar.MINUTE);

                Date end = HuamiCoordinator.getDoNotDisturbEnd(gbDevice.getAddress());
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

    private HuamiSupport setDisconnectNotification(TransactionBuilder builder) {
        DisconnectNotificationSetting disconnectNotificationSetting = HuamiCoordinator.getDisconnectNotificationSetting(getContext(), gbDevice.getAddress());
        LOG.info("Setting disconnect notification to " + disconnectNotificationSetting);

        switch (disconnectNotificationSetting) {
            case ON:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENABLE_DISCONNECT_NOTIFCATION);
                break;
            case OFF:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_DISCONNECT_NOTIFCATION);
                break;
            case SCHEDULED:
                byte[] cmd = HuamiService.COMMAND_ENABLE_DISCONNECT_NOTIFCATION.clone();

                Calendar calendar = GregorianCalendar.getInstance();

                Date start = HuamiCoordinator.getDisconnectNotificationStart(gbDevice.getAddress());
                calendar.setTime(start);
                cmd[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[5] = (byte) calendar.get(Calendar.MINUTE);

                Date end = HuamiCoordinator.getDisconnectNotificationEnd(gbDevice.getAddress());
                calendar.setTime(end);
                cmd[6] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[7] = (byte) calendar.get(Calendar.MINUTE);

                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), cmd);
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

    protected HuamiSupport setBandScreenUnlock(TransactionBuilder builder) {
        boolean enable = MiBand3Coordinator.getBandScreenUnlock(gbDevice.getAddress());
        LOG.info("Setting band screen unlock to " + enable);

        if (enable) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_ENABLE_BAND_SCREEN_UNLOCK);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_DISABLE_BAND_SCREEN_UNLOCK);
        }

        return this;
    }


    protected HuamiSupport setLanguage(TransactionBuilder builder) {
        String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("language", "auto");
        if (localeString == null || localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (country == null) {
                // sometimes country is null, no idea why, guess it.
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }
        LOG.info("Setting device to locale: " + localeString);
        final byte[] command_new = HuamiService.COMMAND_SET_LANGUAGE_NEW_TEMPLATE.clone();
        System.arraycopy(localeString.getBytes(), 0, command_new, 3, localeString.getBytes().length);

        byte[] command_old;
        switch (localeString.substring(0, 2)) {
            case "es":
                command_old = AmazfitBipService.COMMAND_SET_LANGUAGE_SPANISH;
                break;
            case "zh":
                if (localeString.equals("zh_CN")) {
                    command_old = AmazfitBipService.COMMAND_SET_LANGUAGE_SIMPLIFIED_CHINESE;
                } else {
                    command_old = AmazfitBipService.COMMAND_SET_LANGUAGE_TRADITIONAL_CHINESE;

                }
                break;
            default:
                command_old = AmazfitBipService.COMMAND_SET_LANGUAGE_ENGLISH;
        }
        final byte[] finalCommand_old = command_old;
        builder.add(new ConditionalWriteAction(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION)) {
            @Override
            protected byte[] checkCondition() {
                if ((gbDevice.getType() == DeviceType.AMAZFITBIP && new Version(gbDevice.getFirmwareVersion()).compareTo(new Version("0.1.0.77")) < 0) ||
                        (gbDevice.getType() == DeviceType.AMAZFITCOR && new Version(gbDevice.getFirmwareVersion()).compareTo(new Version("1.0.7.23")) < 0)) {
                    return finalCommand_old;
                } else {
                    return command_new;
                }
            }
        });

        return this;
    }


    private HuamiSupport setExposeHRThridParty(TransactionBuilder builder) {
        boolean enable = HuamiCoordinator.getExposeHRThirdParty(gbDevice.getAddress());
        LOG.info("Setting exposure of HR to third party apps to: " + enable);

        if (enable) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_ENBALE_HR_CONNECTION);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_DISABLE_HR_CONNECTION);
        }

        return this;
    }

    protected void writeToChunked(TransactionBuilder builder, int type, byte[] data) {
        final int MAX_CHUNKLENGTH = mMTU - 6;
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


    protected HuamiSupport requestGPSVersion(TransactionBuilder builder) {
        LOG.info("Requesting GPS version");
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_REQUEST_GPS_VERSION);
        return this;
    }

    private HuamiSupport requestAlarms(TransactionBuilder builder) {
        LOG.info("Requesting alarms");
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_REQUEST_ALARMS);
        return this;
    }

    @Override
    public String customStringFilter(String inputString) {
        if (HuamiCoordinator.getUseCustomFont(gbDevice.getAddress())) {
            return convertEmojiToCustomFont(inputString);
        }
        return inputString;
    }


    private String convertEmojiToCustomFont(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char charAt = str.charAt(i);
            if (Character.isHighSurrogate(charAt)) {
                int i2 = i + 1;
                try {
                    int codePoint = Character.toCodePoint(charAt, str.charAt(i2));
                    if (codePoint < 127744 || codePoint > 129510) {
                        sb.append(charAt);
                    } else {
                        sb.append((char) (codePoint - 83712));
                        i = i2;
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    LOG.warn("error while converting emoji to custom font", e);
                    sb.append(charAt);
                }
            } else {
                sb.append(charAt);
            }
            i++;
        }
        return sb.toString();
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
        setDisconnectNotification(builder);
        setExposeHRThridParty(builder);
        setHeartrateMeasurementInterval(builder, getHeartRateMeasurementInterval());
        requestAlarms(builder);
    }

    private int getHeartRateMeasurementInterval() {
        return GBApplication.getPrefs().getInt("heartrate_measurement_interval", 0) / 60;
    }

    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand2FWHelper(uri, context);
    }

    public UpdateFirmwareOperation createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperation(uri, this);
    }

    public int getMTU() {
        return mMTU;
    }

    public int getActivitySampleSize() {
        return mActivitySampleSize;
    }
}
