/*  Copyright (C) 2022 José Rebelo

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

import static org.apache.commons.lang3.ArrayUtils.subarray;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_LANGUAGE_AUTO;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_AUTO;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.SUCCESS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint16;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint8;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.mapTimeZone;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.ALWAYS_ON_DISPLAY_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.ALWAYS_ON_DISPLAY_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.BLUETOOTH_CONNECTED_ADVERTISING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.DATE_FORMAT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.DISPLAY_CALLER;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.DND_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.DND_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.DND_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.FITNESS_GOAL_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.FITNESS_GOAL_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.HEART_RATE_ALL_DAY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.HEART_RATE_HIGH_ALERTS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.HEART_RATE_LOW_ALERTS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_DND_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_DND_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.INACTIVITY_WARNINGS_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LANGUAGE_FOLLOW_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LIFT_WRIST_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LIFT_WRIST_RESPONSE_SENSITIVITY;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LIFT_WRIST_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.LIFT_WRIST_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.NIGHT_MODE_MODE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.NIGHT_MODE_SCHEDULED_END;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.NIGHT_MODE_SCHEDULED_START;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.PASSWORD_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.PASSWORD_TEXT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SCREEN_BRIGHTNESS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SCREEN_ON_ON_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SCREEN_TIMEOUT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SLEEP_BREATHING_QUALITY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SLEEP_HIGH_ACCURACY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SPO2_ALL_DAY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.SPO2_LOW_ALERT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.STRESS_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.STRESS_RELAXATION_REMINDER;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.TEMPERATURE_UNIT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.THIRD_PARTY_HR_SHARING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigArg.TIME_FORMAT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigSetter;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Config.ConfigType;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLift;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.ActivateDisplayOnLiftSensitivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.AlwaysOnDisplay;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DoNotDisturb;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchActivityOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.FetchSportsSummaryOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.HuamiFetchDebugLogsOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation2021;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public abstract class Huami2021Support extends HuamiSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021Support.class);

    // Keep track of Notification ID -> action handle, as BangleJSDeviceSupport.
    // This needs to be simplified.
    private final LimitedQueue mNotificationReplyAction = new LimitedQueue(16);

    // Tracks whether realtime HR monitoring is already started, so we can just
    // send CONTINUE commands
    private boolean heartRateRealtimeStarted;

    public Huami2021Support() {
        this(LOG);
    }

    public Huami2021Support(final Logger logger) {
        super(logger);
    }

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    public byte getCryptFlags() {
        return (byte) 0x80;
    }

    @Override
    public void onTestNewFunction() {
        try {
            final TransactionBuilder builder = performInitialized("test");
            findBandOneShot(builder);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to test new function", e);
        }
    }

    @Override
    protected void acknowledgeFindPhone() {
        LOG.info("Acknowledging find phone");

        final byte[] cmd = new byte[]{FIND_PHONE_ACK, SUCCESS};

        writeToChunked2021("ack find phone", CHUNKED2021_ENDPOINT_FIND_DEVICE, cmd, true);
    }

    protected void findBandOneShot(final TransactionBuilder builder) {
        LOG.info("Sending one-shot find band");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_FIND_DEVICE, new byte[]{FIND_BAND_ONESHOT}, true);
    }

    @Override
    public void onFindDevice(final boolean start) {
        // FIXME: This does not work while band is in DND (#752)
        final CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    public void onPhoneFound() {
        LOG.info("Sending phone found");

        final byte[] cmd = new byte[]{FIND_PHONE_STOP_FROM_PHONE};

        writeToChunked2021("found phone", CHUNKED2021_ENDPOINT_FIND_DEVICE, cmd, true);
    }

    @Override
    public void onSetHeartRateMeasurementInterval(final int seconds) {
        try {
            int minuteInterval;
            if (seconds == -1) {
                // Smart
                minuteInterval = -1;
            } else {
                minuteInterval = seconds / 60;
                minuteInterval = Math.min(minuteInterval, 120);
                minuteInterval = Math.max(0, minuteInterval);
            }

            final TransactionBuilder builder = performInitialized(String.format("set heart rate interval to: %d minutes", minuteInterval));
            setHeartrateMeasurementInterval(builder, minuteInterval);
            builder.queue(getQueue());
        } catch (final IOException e) {
            GB.toast(getContext(), "Error toggling heart measurement interval: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    protected Huami2021Support sendCalendarEvents(final TransactionBuilder builder) {
        // We have native calendar sync
        return this;
    }

    protected void requestCalendarEvents() {
        LOG.info("Requesting calendar events from band");

        writeToChunked2021(
                "request calendar events",
                CHUNKED2021_ENDPOINT_CALENDAR,
                new byte[]{CALENDAR_CMD_EVENTS_REQUEST, 0x00, 0x00},
                false
        );
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        if (calendarEventSpec.type != CalendarEventSpec.TYPE_UNKNOWN) {
            LOG.warn("Unsupported calendar event type {}", calendarEventSpec.type);
            return;
        }

        LOG.info("Sending calendar event {} to band", calendarEventSpec.id);

        int length = 34;
        if (calendarEventSpec.title != null) {
            length += calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length;
        }
        if (calendarEventSpec.description != null) {
            length += calendarEventSpec.description.getBytes(StandardCharsets.UTF_8).length;
        }

        final ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CALENDAR_CMD_CREATE_EVENT);
        buf.putInt((int) calendarEventSpec.id);

        if (calendarEventSpec.title != null) {
            buf.put(calendarEventSpec.title.getBytes(StandardCharsets.UTF_8));
        }
        buf.put((byte) 0x00);

        if (calendarEventSpec.description != null) {
            buf.put(calendarEventSpec.description.getBytes(StandardCharsets.UTF_8));
        }
        buf.put((byte) 0x00);

        buf.putInt(calendarEventSpec.timestamp);
        buf.putInt(calendarEventSpec.timestamp + calendarEventSpec.durationInSeconds);

        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put(bool(calendarEventSpec.allDay));
        buf.put((byte) 0x00); // ?
        buf.put((byte) 130); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?

        writeToChunked2021("delete calendar event", CHUNKED2021_ENDPOINT_CALENDAR, buf.array(), false);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        if (type != CalendarEventSpec.TYPE_UNKNOWN) {
            LOG.warn("Unsupported calendar event type {}", type);
            return;
        }

        LOG.info("Deleting calendar event {} from band", id);

        final ByteBuffer buf = ByteBuffer.allocate(5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CALENDAR_CMD_DELETE_EVENT);
        buf.putInt((int) id);

        writeToChunked2021("delete calendar event", CHUNKED2021_ENDPOINT_CALENDAR, buf.array(), false);
    }

    @Override
    public void onFetchRecordedData(final int dataTypes) {
        try {
            // FIXME: currently only one data type supported, these are meant to be flags
            switch (dataTypes) {
                case RecordedDataTypes.TYPE_ACTIVITY:
                    new FetchActivityOperation(this).perform();
                    break;
                case RecordedDataTypes.TYPE_GPS_TRACKS:
                    new FetchSportsSummaryOperation(this).perform();
                    break;
                case RecordedDataTypes.TYPE_DEBUGLOGS:
                    new HuamiFetchDebugLogsOperation(this).perform();
                    break;
                default:
                    LOG.warn("fetching multiple data types at once is not supported yet");
            }
        } catch (final Exception e) {
            LOG.error("Unable to fetch recorded data types {}", dataTypes, e);
        }
    }

    @Override
    public void onHeartRateTest() {
        // TODO onHeartRateTest - what modes? this only works sometimes

        try {
            final TransactionBuilder builder = performInitialized("HeartRateTest");
            enableNotifyHeartRateMeasurements(true, builder);
            //writeToChunked2021(builder, CHUNKED2021_ENDPOINT_HEARTRATE, new byte[]{HEART_RATE_CMD_REALTIME_SET, HEART_RATE_REALTIME_MODE_START}, false);
            builder.queue(getQueue());
        } catch (final IOException e) {
            LOG.error("Unable to read heart rate from Huami 2021 device", e);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        final byte hrcmd;
        if (!enable) {
            hrcmd = HEART_RATE_REALTIME_MODE_STOP;
        } else if (heartRateRealtimeStarted == enable) {
            hrcmd = HEART_RATE_REALTIME_MODE_CONTINUE;
        } else {
            // enable == true, for the first time
            hrcmd = HEART_RATE_REALTIME_MODE_START;
        }

        heartRateRealtimeStarted = enable;

        try {
            final TransactionBuilder builder = performInitialized("Set realtime heart rate measurement = " + enable);
            enableNotifyHeartRateMeasurements(enable, builder);
            writeToChunked2021(builder, CHUNKED2021_ENDPOINT_HEARTRATE, new byte[]{HEART_RATE_CMD_REALTIME_SET, hrcmd}, false);
            builder.queue(getQueue());
            enableRealtimeSamplesTimer(enable);
        } catch (final IOException e) {
            LOG.error("Unable to set realtime heart rate measurement", e);
        }
    }

    @Override
    protected Huami2021Support requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_BATTERY, new byte[]{BATTERY_REQUEST}, false);

        return this;
    }

    @Override
    protected Huami2021Support setFitnessGoal(final TransactionBuilder builder) {
        final int fitnessGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        LOG.info("Setting Fitness Goal to {}", fitnessGoal);

        new ConfigSetter(ConfigType.HEALTH)
                .setInt(FITNESS_GOAL_STEPS, fitnessGoal)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setUserInfo(final TransactionBuilder builder) {
        LOG.info("Attempting to set user info...");

        final Prefs prefs = GBApplication.getPrefs();
        final String alias = prefs.getString(PREF_USER_NAME, null);
        final ActivityUser activityUser = new ActivityUser();
        final int height = activityUser.getHeightCm();
        final int weight = activityUser.getWeightKg();
        final int birthYear = activityUser.getYearOfBirth();
        final byte birthMonth = 7; // not in user attributes
        final byte birthDay = 1; // not in user attributes

        if (alias == null || weight == 0 || height == 0 || birthYear == 0) {
            LOG.warn("Unable to set user info, make sure it is set up");
            return this;
        }

        byte genderByte = 2; // other
        switch (activityUser.getGender()) {
            case ActivityUser.GENDER_MALE:
                genderByte = 0;
                break;
            case ActivityUser.GENDER_FEMALE:
                genderByte = 1;
        }
        final int userid = alias.hashCode();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(new byte[]{0x01, 0x4f, 0x07, 0x00, 0x00});
            baos.write(fromUint16(birthYear));
            baos.write(birthMonth);
            baos.write(birthDay);
            baos.write(genderByte);
            baos.write((byte) height);
            baos.write((byte) 0); // TODO ?
            baos.write(fromUint16(weight * 200));
            baos.write(BLETypeConversions.fromUint32(userid));
            baos.write(new byte[]{0x00, 0x00, 0x00, 0x00, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x00, 0x09}); // TODO ?
            baos.write(alias.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0);

            writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_USER_INFO, baos.toByteArray(), true);
        } catch (final Exception e) {
            LOG.error("Failed to send user info", e);
        }

        return this;
    }

    @Override
    protected Huami2021Support setWearLocation(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setPassword(final TransactionBuilder builder) {
        final boolean passwordEnabled = HuamiCoordinator.getPasswordEnabled(gbDevice.getAddress());
        final String password = HuamiCoordinator.getPassword(gbDevice.getAddress());

        LOG.info("Setting password: {}, {}", passwordEnabled, password);

        if (password == null || password.isEmpty()) {
            LOG.warn("Invalid password: {}", password);
            return this;
        }

        new ConfigSetter(ConfigType.LOCKSCREEN)
                .setBoolean(PASSWORD_ENABLED, passwordEnabled)
                .setString(PASSWORD_TEXT, password)
                .write(this, builder);

        return this;
    }

    @Override
    protected void queueAlarm(final Alarm alarm, final TransactionBuilder builder) {
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);

        final Calendar calendar = AlarmUtils.toCalendar(alarm);

        final byte[] alarmMessage;
        if (!alarm.getUnused()) {
            int alarmFlags = 0;
            if (alarm.getEnabled()) {
                alarmFlags = ALARM_FLAG_ENABLED;
            }
            if (coordinator.supportsSmartWakeup(gbDevice) && alarm.getSmartWakeup()) {
                alarmFlags |= ALARM_FLAG_SMART;
            }
            alarmMessage = new byte[]{
                    ALARMS_CMD_CREATE,
                    (byte) 0x01, // ?
                    (byte) alarmFlags,
                    (byte) alarm.getPosition(),
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE),
                    (byte) alarm.getRepetition(),
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?, this is usually 0 in the create command, 1 in the watch response
                    (byte) 0x00, // ?
            };
        } else {
            // Delete it from the band
            alarmMessage = new byte[]{
                    ALARMS_CMD_DELETE,
                    (byte) 0x01, // ?
                    (byte) alarm.getPosition()
            };
        }

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_ALARMS, alarmMessage, false);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            final TransactionBuilder builder = performInitialized("send notification");

            baos.write(NOTIFICATION_CMD_SEND);

            // ID
            baos.write(BLETypeConversions.fromUint32(0));

            baos.write(NOTIFICATION_TYPE_CALL);
            if (callSpec.command == CallSpec.CALL_INCOMING) {
                baos.write(NOTIFICATION_CALL_STATE_START);
            } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
                baos.write(NOTIFICATION_CALL_STATE_END);
            }

            baos.write(0x00); // ?
            if (callSpec.name != null) {
                baos.write(callSpec.name.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0x00);

            baos.write(0x00); // ?
            baos.write(0x00); // ?

            if (callSpec.number != null) {
                baos.write(callSpec.number.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0x00);

            // TODO put this behind a setting?
            baos.write(callSpec.number != null ? 0x01 : 0x00); // reply from watch

            writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_NOTIFICATIONS, baos.toByteArray(), true);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to send call", e);
        }
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        // TODO Check real limit for notificationMaxLength / respect across all fields

        try {
            final TransactionBuilder builder = performInitialized("send notification");

            baos.write(NOTIFICATION_CMD_SEND);
            baos.write(BLETypeConversions.fromUint32(notificationSpec.getId()));
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                baos.write(NOTIFICATION_TYPE_SMS);
            } else {
                baos.write(NOTIFICATION_TYPE_NORMAL);
            }
            baos.write(NOTIFICATION_SUBCMD_SHOW);

            // app package
            if (notificationSpec.sourceAppId != null) {
                baos.write(notificationSpec.sourceAppId.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // sender/title
            if (!senderOrTitle.isEmpty()) {
                baos.write(senderOrTitle.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // body
            if (notificationSpec.body != null) {
                baos.write(StringUtils.truncate(notificationSpec.body, notificationMaxLength()).getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // app name
            if (notificationSpec.sourceName != null) {
                baos.write(notificationSpec.sourceName.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // reply
            boolean hasReply = false;
            if (notificationSpec.attachedActions != null && notificationSpec.attachedActions.size() > 0) {
                for (int i = 0; i < notificationSpec.attachedActions.size(); i++) {
                    final NotificationSpec.Action action = notificationSpec.attachedActions.get(i);

                    switch (action.type) {
                        case NotificationSpec.Action.TYPE_WEARABLE_REPLY:
                        case NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR:
                            hasReply = true;
                            mNotificationReplyAction.add(notificationSpec.getId(), ((long) notificationSpec.getId() << 4) + i + 1);
                            break;
                        default:
                            break;
                    }
                }
            }

            baos.write((byte) (hasReply ? 1 : 0));

            writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_NOTIFICATIONS, baos.toByteArray(), true);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to send notification", e);
        }

    }

    @Override
    protected int notificationMaxLength() {
        return 512;
    }

    protected Huami2021Support requestReminders(final TransactionBuilder builder) {
        LOG.info("Requesting reminders");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_REMINDERS, new byte[]{REMINDERS_CMD_REQUEST}, false);

        return this;
    }

    @Override
    protected void sendReminderToDevice(final TransactionBuilder builder, int position, final Reminder reminder) {
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        if (position + 1 > coordinator.getReminderSlotCount()) {
            LOG.error("Reminder for position {} is over the limit of {} reminders", position, coordinator.getReminderSlotCount());
            return;
        }

        if (reminder == null) {
            // Delete reminder
            writeToChunked2021(builder, CHUNKED2021_ENDPOINT_REMINDERS, new byte[]{REMINDERS_CMD_DELETE, (byte) (position & 0xFF)}, false);

            return;
        }

        final String message;
        if (reminder.getMessage().length() > coordinator.getMaximumReminderMessageLength()) {
            LOG.warn("The reminder message length {} is longer than {}, will be truncated",
                    reminder.getMessage().length(),
                    coordinator.getMaximumReminderMessageLength()
            );
            message = StringUtils.truncate(reminder.getMessage(), coordinator.getMaximumReminderMessageLength());
        } else {
            message = reminder.getMessage();
        }

        final ByteBuffer buf = ByteBuffer.allocate(1 + 10 + message.getBytes(StandardCharsets.UTF_8).length + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Update does an upsert, so let's use it. If we call create twice on the same ID, it becomes weird
        buf.put(REMINDERS_CMD_UPDATE);
        buf.put((byte) (position & 0xFF));

        final Calendar cal = Calendar.getInstance();
        cal.setTime(reminder.getDate());

        int reminderFlags = REMINDER_FLAG_ENABLED | REMINDER_FLAG_TEXT;

        switch (reminder.getRepetition()) {
            case Reminder.ONCE:
                // Default is once, nothing to do
                break;
            case Reminder.EVERY_DAY:
                reminderFlags |= 0x0fe0; // all week day bits set
                break;
            case Reminder.EVERY_WEEK:
                int dayOfWeek = BLETypeConversions.dayOfWeekToRawBytes(cal) - 1; // Monday = 0
                reminderFlags |= 0x20 << dayOfWeek;
                break;
            case Reminder.EVERY_MONTH:
                reminderFlags |= REMINDER_FLAG_REPEAT_MONTH;
                break;
            case Reminder.EVERY_YEAR:
                reminderFlags |= REMINDER_FLAG_REPEAT_YEAR;
                break;
            default:
                LOG.warn("Unknown repetition for reminder in position {}, defaulting to once", position);
        }

        buf.putInt(reminderFlags);

        buf.putInt((int) (cal.getTimeInMillis() / 1000L));
        buf.put((byte) 0x00);

        buf.put(message.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_REMINDERS, buf.array(), false);
    }

    @Override
    protected void sendWorldClocks(final TransactionBuilder builder,
                                   final List<? extends WorldClock> clocks) {
        // TODO not yet supported by the official app, but menu option shows up on the band
    }

    @Override
    public void onDeleteNotification(final int id) {
        LOG.info("Deleting notification {} from band", id);

        final ByteBuffer buf = ByteBuffer.allocate(12);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_SEND);
        buf.putInt(id);
        buf.put(NOTIFICATION_TYPE_NORMAL);
        buf.put(NOTIFICATION_SUBCMD_DISMISS_FROM_PHONE);
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?

        writeToChunked2021("delete notification", CHUNKED2021_ENDPOINT_NOTIFICATIONS, buf.array(), true);
    }

    @Override
    protected void sendPhoneGps(final HuamiPhoneGpsStatus status, final Location location) {
        final byte[] locationBytes = encodePhoneGpsPayload(status, location);

        final ByteBuffer buf = ByteBuffer.allocate(2 + locationBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(WORKOUT_CMD_GPS_LOCATION);
        buf.put((byte) 0x00); // ?
        buf.put(locationBytes);

        writeToChunked2021("send phone gps", CHUNKED2021_ENDPOINT_WORKOUT, buf.array(), true);
    }

    @Override
    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        if (cannedMessagesSpec.type != CannedMessagesSpec.TYPE_GENERIC) {
            LOG.warn("Got unsupported canned messages type: {}", cannedMessagesSpec.type);
            return;
        }

        try {
            final TransactionBuilder builder = performInitialized("set canned messages");

            for (int i = 0; i < 16; i++) {
                LOG.debug("Deleting canned message {}", i);
                final ByteBuffer buf = ByteBuffer.allocate(5);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.put(CANNED_MESSAGES_CMD_DELETE);
                buf.putInt(i);
                writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CANNED_MESSAGES, buf.array(), false);
            }

            int i = 0;
            for (String cannedMessage : cannedMessagesSpec.cannedMessages) {
                cannedMessage = StringUtils.truncate(cannedMessage, 140);
                LOG.debug("Setting canned message {} = '{}'", i, cannedMessage);

                final int length = cannedMessage.getBytes(StandardCharsets.UTF_8).length + 7;
                final ByteBuffer buf = ByteBuffer.allocate(length);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.put(CANNED_MESSAGES_CMD_SET);
                buf.putInt(i++);
                buf.put((byte) cannedMessage.getBytes(StandardCharsets.UTF_8).length);
                buf.put((byte) 0x00);
                buf.put(cannedMessage.getBytes(StandardCharsets.UTF_8));
                writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CANNED_MESSAGES, buf.array(), false);
            }
            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to set canned messages on Huami device", ex);
        }
    }

    protected void requestCannedMessages(final TransactionBuilder builder) {
        LOG.info("Requesting canned messages");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CANNED_MESSAGES, new byte[]{CANNED_MESSAGES_CMD_REQUEST}, false);
    }

    protected void requestCannedMessages() {
        try {
            final TransactionBuilder builder = performInitialized("request canned messages");
            requestCannedMessages(builder);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to request canned messages", e);
        }
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        // FIXME: we need to send the music info and state as well, or it breaks the info
        sendMusicStateToDevice(bufferMusicSpec, bufferMusicStateSpec);
    }

    protected void sendMusicStateToDevice(final MusicSpec musicSpec,
                                          final MusicStateSpec musicStateSpec) {
        byte[] cmd = ArrayUtils.addAll(new byte[]{MUSIC_CMD_MEDIA_INFO}, encodeMusicState(musicSpec, musicStateSpec, true));

        LOG.info("sendMusicStateToDevice: {}, {}", musicSpec, musicStateSpec);

        writeToChunked2021("send playback info", CHUNKED2021_ENDPOINT_MUSIC, cmd, false);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        final byte[] cmd = {STEPS_CMD_ENABLE_REALTIME, bool(enable)};

        writeToChunked2021("toggle realtime steps", CHUNKED2021_ENDPOINT_STEPS, cmd, false);
    }

    @Override
    protected Huami2021Support setHeartrateSleepSupport(final TransactionBuilder builder) {
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(gbDevice.getAddress());

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(SLEEP_HIGH_ACCURACY_MONITORING, enableHrSleepSupport)
                .write(this, builder);

        return this;
    }

    @Override
    public Huami2021Support setCurrentTimeWithService(TransactionBuilder builder) {
        // It seems that the format sent to the Current Time characteristic changed in newer devices
        // to kind-of match the GATT spec, but it doesn't quite respect it?
        // - 11 bytes get sent instead of 10 (extra byte at the end for the offset in quarter-hours?)
        // - Day of week starts at 0
        // Otherwise, the command gets rejected with an "Out of Range" error and init fails.

        final Calendar timestamp = Calendar.getInstance();
        final byte[] year = fromUint16(timestamp.get(Calendar.YEAR));

        final byte[] cmd = {
                year[0],
                year[1],
                fromUint8(timestamp.get(Calendar.MONTH) + 1),
                fromUint8(timestamp.get(Calendar.DATE)),
                fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
                fromUint8(timestamp.get(Calendar.MINUTE)),
                fromUint8(timestamp.get(Calendar.SECOND)),
                fromUint8(timestamp.get(Calendar.DAY_OF_WEEK) - 1),
                0x00, // Fractions256?
                0x08, // Reason for change?
                mapTimeZone(timestamp, BLETypeConversions.TZ_FLAG_INCLUDE_DST_IN_TZ), // TODO: Confirm this
        };

        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), cmd);

        return this;
    }

    @Override
    public Huami2021Support enableFurtherNotifications(final TransactionBuilder builder,
                                                       final boolean enable) {
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ), enable);

        return this;
    }

    @Override
    protected Huami2021Support setHeartrateActivityMonitoring(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("setHeartrateActivityMonitoring not implemented");
        return null;
    }

    @Override
    protected Huami2021Support setHeartrateAlert(final TransactionBuilder builder) {
        final int hrAlertThresholdHigh = HuamiCoordinator.getHeartrateAlertHighThreshold(gbDevice.getAddress());
        final int hrAlertThresholdLow = HuamiCoordinator.getHeartrateAlertLowThreshold(gbDevice.getAddress());

        LOG.info("Setting heart rate alert thresholds to {}, {}", hrAlertThresholdHigh, hrAlertThresholdLow);

        new ConfigSetter(ConfigType.HEALTH)
                .setByte(HEART_RATE_HIGH_ALERTS, (byte) hrAlertThresholdHigh)
                .setByte(HEART_RATE_LOW_ALERTS, (byte) hrAlertThresholdLow)
                .write(this, builder);

        return null;
    }

    @Override
    protected HuamiSupport setHeartrateSleepBreathingQualityMonitoring(TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getHeartrateSleepBreathingQualityMonitoring(gbDevice.getAddress());
        LOG.info("Setting stress relaxation reminder to {}", enable);

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(SLEEP_BREATHING_QUALITY_MONITORING, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected HuamiSupport setSPO2AllDayMonitoring(TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getSPO2AllDayMonitoring(gbDevice.getAddress());
        LOG.info("Setting SPO2 All-day monitoring to {}", enable);

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(SPO2_ALL_DAY_MONITORING, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected HuamiSupport setSPO2AlertThreshold(TransactionBuilder builder) {
        final int spo2threshold = HuamiCoordinator.getSPO2AlertThreshold(gbDevice.getAddress());
        LOG.info("Setting SPO2 alert threshold to {}", spo2threshold);

        new ConfigSetter(ConfigType.HEALTH)
                .setByte(SPO2_LOW_ALERT, (byte) spo2threshold)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setHeartrateStressMonitoring(final TransactionBuilder builder) {
        final boolean enableHrStressMonitoring = HuamiCoordinator.getHeartrateStressMonitoring(gbDevice.getAddress());
        LOG.info("Setting heart rate stress monitoring to {}", enableHrStressMonitoring);

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(STRESS_MONITORING, enableHrStressMonitoring)
                .write(this, builder);

        return this;
    }

    @Override
    protected HuamiSupport setHeartrateStressRelaxationReminder(TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getHeartrateStressRelaxationReminder(gbDevice.getAddress());
        LOG.info("Setting stress relaxation reminder to {}", enable);

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(STRESS_RELAXATION_REMINDER, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected HuamiSupport setHeartrateMeasurementInterval(TransactionBuilder builder, int minutes) {
        new ConfigSetter(ConfigType.HEALTH)
                .setByte(HEART_RATE_ALL_DAY_MONITORING, (byte) minutes)
                .write(this, builder);

        return this;
    }

    @Override
    public Huami2021Support sendFactoryReset(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("sendFactoryReset not implemented");
        return null;
    }

    @Override
    protected void setVibrationPattern(final TransactionBuilder builder,
                                       final HuamiVibrationPatternNotificationType notificationType,
                                       final boolean test,
                                       final VibrationProfile profile) {
        final int MAX_TOTAL_LENGTH_MS = 10_000; // 10 seconds, about as long as Mi Fit allows

        // The on-off sequence, until the max total length is reached
        final List<Short> onOff = truncateVibrationsOnOff(profile, MAX_TOTAL_LENGTH_MS);

        final ByteBuffer buf = ByteBuffer.allocate(5 + 2 * onOff.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(VIBRATION_PATTERN_SET);
        buf.put(notificationType.getCode());
        buf.put((byte) 0x01);
        buf.put((byte) (test ? 1 : 0));
        buf.put((byte) (onOff.size() / 2));

        for (Short time : onOff) {
            buf.putShort(time);
        }

        writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS, buf.array(), true);
    }

    @Override
    public void onSendWeather(final WeatherSpec weatherSpec) {
        // Weather is not sent directly to the bands, they send HTTP requests for each location.
        // When we have a weather update, set the default location to that location on the band.
        // TODO: Support for multiple weather locations

        final String locationKey = "1.234,-5.678,xiaomi_accu:" + System.currentTimeMillis(); // dummy
        final String locationName = weatherSpec.location;

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write((byte) 0x09);
            baos.write((byte) 0x02); // ? 2 for current, 4 for default
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write(locationKey.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00); // ?
            baos.write(locationName.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00); // ?

            final TransactionBuilder builder = performInitialized("set weather location");
            writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_WEATHER, baos.toByteArray(), false);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to set weather location", e);
        }
    }

    @Override
    protected Huami2021Support setDateDisplay(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Request GPS version not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setDateFormat(final TransactionBuilder builder) {
        final String dateFormat = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("dateformat", "MM/dd/yyyy");
        if (dateFormat == null) {
            return this;
        }

        switch (dateFormat) {
            case "YYYY/MM/DD":
            case "yyyy/mm/dd":
            case "YYYY.MM.DD":
            case "yyyy.mm.dd":
            case "MM/DD/YYYY":
            case "MM.DD.YYYY":
            case "mm/dd/yyyy":
            case "mm.dd.yyyy":
            case "DD/MM/YYYY":
            case "DD.MM.YYYY":
            case "dd/mm/yyyy":
            case "dd.mm.yyyy":
                break;
            default:
                LOG.warn("unsupported date format " + dateFormat);
                return this;
        }

        new ConfigSetter(ConfigType.SYSTEM)
                .setString(DATE_FORMAT, dateFormat.replace("/", ".").toLowerCase(Locale.ROOT))
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setTimeFormat(final TransactionBuilder builder) {
        final GBPrefs gbPrefs = new GBPrefs(new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())));
        final String timeFormat = gbPrefs.getTimeFormat();

        // FIXME: This "works", but the band does not update when the setting changes, so we don't do anything
        if (true) {
            LOG.warn("setDateTime is disabled");
            return this;
        }

        LOG.info("Setting time format to {}", timeFormat);

        final byte timeFormatByte;
        if (timeFormat.equals("24h")) {
            timeFormatByte = 0x01;
        } else {
            timeFormatByte = 0x00;
        }

        new ConfigSetter(ConfigType.SYSTEM)
                .setByte(TIME_FORMAT, timeFormatByte)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setGoalNotification(final TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getGoalNotification(gbDevice.getAddress());
        LOG.info("Setting goal notification to {}", enable);

        // TODO confirm this works

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(FITNESS_GOAL_NOTIFICATION, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setAlwaysOnDisplay(final TransactionBuilder builder) {
        final AlwaysOnDisplay alwaysOnDisplay = HuamiCoordinator.getAlwaysOnDisplay(gbDevice.getAddress());
        LOG.info("Setting always on display mode {}", alwaysOnDisplay);

        final byte aodByte;
        switch (alwaysOnDisplay) {
            case AUTO:
                aodByte = 0x01;
                break;
            case SCHEDULED:
                aodByte = 0x02;
                break;
            case ALWAYS:
                aodByte = 0x03;
                break;
            case OFF:
            default:
                aodByte = 0x00;
                break;
        }

        final Date start = HuamiCoordinator.getAlwaysOnDisplayStart(gbDevice.getAddress());
        final Date end = HuamiCoordinator.getAlwaysOnDisplayEnd(gbDevice.getAddress());

        new ConfigSetter(ConfigType.DISPLAY)
                .setByte(ALWAYS_ON_DISPLAY_MODE, aodByte)
                .setHourMinute(ALWAYS_ON_DISPLAY_SCHEDULED_START, start)
                .setHourMinute(ALWAYS_ON_DISPLAY_SCHEDULED_END, end)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setActivateDisplayOnLiftWrist(final TransactionBuilder builder) {
        final ActivateDisplayOnLift displayOnLift = HuamiCoordinator.getActivateDisplayOnLiftWrist(getContext(), gbDevice.getAddress());
        LOG.info("Setting activate display on lift wrist to {}", displayOnLift);

        final byte liftWristByte;
        switch (displayOnLift) {
            case SCHEDULED:
                liftWristByte = 0x01;
                break;
            case ON:
                liftWristByte = 0x02;
                break;
            case OFF:
            default:
                liftWristByte = 0x00;
                break;
        }

        final Date start = HuamiCoordinator.getDisplayOnLiftStart(gbDevice.getAddress());
        final Date end = HuamiCoordinator.getDisplayOnLiftEnd(gbDevice.getAddress());

        new ConfigSetter(ConfigType.DISPLAY)
                .setByte(LIFT_WRIST_MODE, liftWristByte)
                .setHourMinute(LIFT_WRIST_SCHEDULED_START, start)
                .setHourMinute(LIFT_WRIST_SCHEDULED_END, end)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setActivateDisplayOnLiftWristSensitivity(final TransactionBuilder builder) {
        final ActivateDisplayOnLiftSensitivity sensitivity = HuamiCoordinator.getDisplayOnLiftSensitivity(gbDevice.getAddress());
        LOG.info("Setting activate display on lift wrist sensitivity to {}", sensitivity);

        final byte sensitivityByte;
        switch (sensitivity) {
            case SENSITIVE:
                sensitivityByte = 0x01;
                break;
            case NORMAL:
            default:
                sensitivityByte = 0x00;
                break;
        }

        new ConfigSetter(ConfigType.DISPLAY)
                .setByte(LIFT_WRIST_RESPONSE_SENSITIVITY, sensitivityByte)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setDisplayItems(final TransactionBuilder builder) {
        setDisplayItems2021(builder, false);
        return this;
    }

    @Override
    protected Huami2021Support setShortcuts(final TransactionBuilder builder) {
        setDisplayItems2021(builder, true);
        return this;
    }

    private void setDisplayItems2021(final TransactionBuilder builder,
                                     final boolean isShortcuts) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        final List<String> allSettings;
        List<String> enabledList;
        final byte menuType;

        if (isShortcuts) {
            menuType = Huami2021Service.DISPLAY_ITEMS_SHORTCUTS;
            allSettings = prefs.getList(HuamiConst.PREF_ALL_SHORTCUTS, Collections.<String>emptyList());
            enabledList = prefs.getList(HuamiConst.PREF_SHORTCUTS_SORTABLE, Collections.<String>emptyList());
            LOG.info("Setting shortcuts");
        } else {
            menuType = Huami2021Service.DISPLAY_ITEMS_MENU;
            allSettings = prefs.getList(HuamiConst.PREF_ALL_DISPLAY_ITEMS, Collections.<String>emptyList());
            enabledList = prefs.getList(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, Collections.<String>emptyList());
            LOG.info("Setting menu items");
        }

        if (allSettings.isEmpty()) {
            LOG.warn("List of all display items is missing");
            return;
        }

        if (!isShortcuts && !enabledList.contains("00000013")) {
            // Settings can't be disabled
            enabledList.add("00000013");
        }

        if (isShortcuts && enabledList.size() > 10) {
            // Enforced by official app
            LOG.warn("Truncating shortcuts list to 10");
            enabledList = enabledList.subList(0, 10);
        }

        LOG.info("Setting display items (shortcuts={}): {}", isShortcuts, enabledList);

        int numItems = allSettings.size();
        if (!isShortcuts) {
            // Exclude the "more" item from the main menu, since it's not a real item
            numItems--;
        }

        final ByteBuffer buf = ByteBuffer.allocate(4 + numItems * 12);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 0x05);
        buf.put(menuType);
        buf.put((byte) numItems);
        buf.put((byte) 0x00);

        byte pos = 0;
        boolean inMoreSection = false;

        for (final String id : enabledList) {
            if (id.equals("more")) {
                inMoreSection = true;
                pos = 0;
                continue;
            }

            final byte sectionKey;
            if (inMoreSection) {
                // In more section
                sectionKey = DISPLAY_ITEMS_SECTION_MORE;
            } else {
                // In main section
                sectionKey = DISPLAY_ITEMS_SECTION_MAIN;
            }

            // Screen IDs are sent as literal hex strings
            buf.put(id.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0);
            buf.put(sectionKey);
            buf.put(pos++);
            buf.put((byte) (id.equals("00000013") ? 1 : 0));
        }

        // Set all disabled items
        pos = 0;
        for (final String id : allSettings) {
            if (enabledList.contains(id) || id.equals("more")) {
                continue;
            }

            // Screen IDs are sent as literal hex strings
            buf.put(id.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0);
            buf.put(DISPLAY_ITEMS_SECTION_DISABLED);
            buf.put(pos++);
            buf.put((byte) (id.equals("00000013") ? 1 : 0));
        }

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_DISPLAY_ITEMS, buf.array(), true);
    }

    @Override
    protected Huami2021Support setWorkoutActivityTypes(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setBeepSounds(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setRotateWristToSwitchInfo(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setDisplayCaller(final TransactionBuilder builder) {
        // TODO: Make this configurable

        LOG.info("Enabling caller display");

        new ConfigSetter(ConfigType.SYSTEM)
                .setBoolean(DISPLAY_CALLER, true)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setDoNotDisturb(final TransactionBuilder builder) {
        final DoNotDisturb doNotDisturb = HuamiCoordinator.getDoNotDisturb(gbDevice.getAddress());
        LOG.info("Setting do not disturb to {}", doNotDisturb);

        final byte dndByte;
        switch (doNotDisturb) {
            case SCHEDULED:
                dndByte = 0x01;
                break;
            case AUTOMATIC:
                dndByte = 0x02;
                break;
            case ALWAYS:
                dndByte = 0x03;
                break;
            case OFF:
            default:
                dndByte = 0x00;
                break;
        }

        final Date start = HuamiCoordinator.getDoNotDisturbStart(gbDevice.getAddress());
        final Date end = HuamiCoordinator.getDoNotDisturbEnd(gbDevice.getAddress());

        new ConfigSetter(ConfigType.SYSTEM)
                .setByte(DND_MODE, dndByte)
                .setHourMinute(DND_SCHEDULED_START, start)
                .setHourMinute(DND_SCHEDULED_END, end)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setNightMode(final TransactionBuilder builder) {
        final String nightMode = MiBand3Coordinator.getNightMode(gbDevice.getAddress());
        LOG.info("Setting night mode to {}", nightMode);

        final byte nightModeByte;
        switch (nightMode) {
            case MiBandConst.PREF_NIGHT_MODE_SUNSET:
                nightModeByte = 0x01;
                break;
            case MiBandConst.PREF_NIGHT_MODE_SCHEDULED:
                nightModeByte = 0x02;
                break;
            case MiBandConst.PREF_NIGHT_MODE_OFF:
            default:
                nightModeByte = 0x00;
        }

        final Date start = MiBand3Coordinator.getNightModeStart(gbDevice.getAddress());
        final Date end = MiBand3Coordinator.getNightModeEnd(gbDevice.getAddress());

        new ConfigSetter(ConfigType.SYSTEM)
                .setByte(NIGHT_MODE_MODE, nightModeByte)
                .setHourMinute(NIGHT_MODE_SCHEDULED_START, start)
                .setHourMinute(NIGHT_MODE_SCHEDULED_END, end)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setInactivityWarnings(final TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getInactivityWarnings(gbDevice.getAddress());
        LOG.info("Setting inactivity warnings to {}", enable);

        final Date intervalStart = HuamiCoordinator.getInactivityWarningsStart(gbDevice.getAddress());
        final Date intervalEnd = HuamiCoordinator.getInactivityWarningsEnd(gbDevice.getAddress());
        boolean enableDnd = HuamiCoordinator.getInactivityWarningsDnd(gbDevice.getAddress());
        final Date dndStart = HuamiCoordinator.getInactivityWarningsDndStart(gbDevice.getAddress());
        final Date dndEnd = HuamiCoordinator.getInactivityWarningsDndEnd(gbDevice.getAddress());

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(INACTIVITY_WARNINGS_ENABLED, enable)
                .setHourMinute(INACTIVITY_WARNINGS_SCHEDULED_START, intervalStart)
                .setHourMinute(INACTIVITY_WARNINGS_SCHEDULED_END, intervalEnd)
                .setBoolean(INACTIVITY_WARNINGS_DND_ENABLED, enableDnd)
                .setHourMinute(INACTIVITY_WARNINGS_DND_SCHEDULED_START, dndStart)
                .setHourMinute(INACTIVITY_WARNINGS_DND_SCHEDULED_END, dndEnd)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setDisconnectNotification(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setDistanceUnit(final TransactionBuilder builder) {
        final MiBandConst.DistanceUnit unit = HuamiCoordinator.getDistanceUnit();
        LOG.info("Setting distance unit to {}", unit);

        final byte unitByte;
        switch (unit) {
            case IMPERIAL:
                unitByte = 0x01;
                break;
            case METRIC:
            default:
                unitByte = 0x00;
                break;
        }

        new ConfigSetter(ConfigType.SYSTEM)
                .setByte(TEMPERATURE_UNIT, unitByte)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setBandScreenUnlock(final TransactionBuilder builder) {
        // Supported by the Mi Band 7 through the band, but not configurable through the app
        LOG.warn("Function not implemented");
        return this;
    }

    @Override
    protected Huami2021Support setScreenOnOnNotification(final TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getScreenOnOnNotification(gbDevice.getAddress());
        LOG.info("Set Screen On on notification = {}", enable);

        new ConfigSetter(ConfigType.DISPLAY)
                .setBoolean(SCREEN_ON_ON_NOTIFICATIONS, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setScreenBrightness(final TransactionBuilder builder) {
        final int brightness = HuamiCoordinator.getScreenBrightness(gbDevice.getAddress());
        LOG.info("Setting band screen brightness to {}", brightness);

        new ConfigSetter(ConfigType.DISPLAY)
                .setShort(SCREEN_BRIGHTNESS, (byte) brightness)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setScreenTimeout(final TransactionBuilder builder) {
        final int timeout = HuamiCoordinator.getScreenTimeout(gbDevice.getAddress());
        LOG.info("Setting band screen timeout to {}", timeout);

        new ConfigSetter(ConfigType.DISPLAY)
                .setByte(SCREEN_TIMEOUT, (byte) timeout)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setLanguage(final TransactionBuilder builder) {
        final String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                .getString("language", "auto");

        LOG.info("Setting device language to {}", localeString);

        new ConfigSetter(ConfigType.LANGUAGE)
                .setByte(LANGUAGE, getLanguageId())
                .setBoolean(LANGUAGE_FOLLOW_PHONE, localeString.equals("auto"))
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setExposeHRThirdParty(final TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getExposeHRThirdParty(gbDevice.getAddress());
        LOG.info("Setting exposure of HR to third party apps to {}", enable);

        new ConfigSetter(ConfigType.HEALTH)
                .setBoolean(THIRD_PARTY_HR_SHARING, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected Huami2021Support setBtConnectedAdvertising(final TransactionBuilder builder) {
        final boolean enable = HuamiCoordinator.getBtConnectedAdvertising(gbDevice.getAddress());
        LOG.info("Setting connected advertisement to: {}", enable);

        new ConfigSetter(ConfigType.BLUETOOTH)
                .setBoolean(BLUETOOTH_CONNECTED_ADVERTISING, enable)
                .write(this, builder);

        return this;
    }

    @Override
    protected void writeToChunked(final TransactionBuilder builder,
                                  final int type,
                                  final byte[] data) {
        LOG.warn("writeToChunked is not supported");
    }

    @Override
    protected void writeToChunkedOld(final TransactionBuilder builder, final int type, final byte[] data) {
        LOG.warn("writeToChunkedOld is not supported");
    }

    @Override
    public void writeToConfiguration(final TransactionBuilder builder, final byte[] data) {
        LOG.warn("writeToConfiguration is not supported");
    }

    @Override
    protected Huami2021Support requestGPSVersion(final TransactionBuilder builder) {
        // Not supported by the Mi Band 7 at least
        LOG.warn("Request GPS version not implemented");
        return this;
    }

    @Override
    protected Huami2021Support requestAlarms(final TransactionBuilder builder) {
        LOG.info("Requesting alarms");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_ALARMS, new byte[]{ALARMS_CMD_REQUEST}, false);

        return this;
    }

    private void requestAlarms() {
        try {
            final TransactionBuilder builder = performInitialized("request alarms");
            requestAlarms(builder);
            builder.queue(getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to request alarms", e);
        }
    }

    @Override
    public Huami2021Support requestDisplayItems(final TransactionBuilder builder) {
        LOG.info("Requesting display items");

        writeToChunked2021(
                builder,
                CHUNKED2021_ENDPOINT_DISPLAY_ITEMS,
                new byte[]{DISPLAY_ITEMS_CMD_REQUEST, DISPLAY_ITEMS_MENU},
                true
        );

        return this;
    }

    @Override
    protected Huami2021Support requestShortcuts(final TransactionBuilder builder) {
        LOG.info("Requesting shortcuts");

        writeToChunked2021(
                builder,
                CHUNKED2021_ENDPOINT_DISPLAY_ITEMS,
                new byte[]{DISPLAY_ITEMS_CMD_REQUEST, DISPLAY_ITEMS_SHORTCUTS},
                true
        );

        return this;
    }

    @Override
    public void phase2Initialize(final TransactionBuilder builder) {
        LOG.info("2021 phase2Initialize...");
        requestBatteryInfo(builder);
    }

    @Override
    public void phase3Initialize(final TransactionBuilder builder) {
        LOG.info("2021 phase3Initialize...");
        setUserInfo(builder);

        for (final ConfigType configType : ConfigType.values()) {
            // FIXME: Request only supported args?
            requestConfig(builder, configType);
        }

        for (final HuamiVibrationPatternNotificationType type : HuamiVibrationPatternNotificationType.values()) {
            // FIXME: Can we read these from the band?
            final String typeKey = type.name().toLowerCase(Locale.ROOT);
            setVibrationPattern(builder, HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey);
        }

        requestCannedMessages(builder);
        requestDisplayItems(builder);
        requestShortcuts(builder);
        requestAlarms(builder);
        //requestReminders(builder);
    }

    @Override
    public UpdateFirmwareOperation createUpdateFirmwareOperation(final Uri uri) {
        return new UpdateFirmwareOperation2021(uri, this);
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }

    @Override
    public boolean force2021Protocol() {
        return true;
    }

    @Override
    public void handle2021Payload(final int type, final byte[] payload) {
        if (payload == null || payload.length == 0) {
            LOG.warn("Empty or null payload for {}", String.format("0x%04x", type));
            return;
        }

        LOG.debug("Got 2021 payload for {}: {}", String.format("0x%04x", type), GB.hexdump(payload));

        switch (type) {
            case CHUNKED2021_ENDPOINT_ALARMS:
                handle2021Alarms(payload);
                return;
            case CHUNKED2021_ENDPOINT_AUTH:
                LOG.warn("Unexpected auth payload {}", GB.hexdump(payload));
                return;
            case CHUNKED2021_ENDPOINT_CALENDAR:
                handle2021Calendar(payload);
                return;
            case CHUNKED2021_ENDPOINT_COMPAT:
                LOG.warn("Unexpected compat payload {}", GB.hexdump(payload));
                return;
            case CHUNKED2021_ENDPOINT_CONFIG:
                handle2021Config(payload);
                return;
            case CHUNKED2021_ENDPOINT_ICONS:
                handle2021Icons(payload);
                return;
            case CHUNKED2021_ENDPOINT_WORKOUT:
                handle2021Workout(payload);
                return;
            case CHUNKED2021_ENDPOINT_DISPLAY_ITEMS:
                handle2021DisplayItems(payload);
                return;
            case CHUNKED2021_ENDPOINT_FIND_DEVICE:
                handle2021FindDevice(payload);
                return;
            case CHUNKED2021_ENDPOINT_HTTP:
                handle2021Http(payload);
                return;
            case CHUNKED2021_ENDPOINT_HEARTRATE:
                handle2021HeartRate(payload);
                return;
            case CHUNKED2021_ENDPOINT_NOTIFICATIONS:
                handle2021Notifications(payload);
                return;
            case CHUNKED2021_ENDPOINT_REMINDERS:
                handle2021Reminders(payload);
                return;
            case CHUNKED2021_ENDPOINT_CANNED_MESSAGES:
                handle2021CannedMessages(payload);
                return;
            case CHUNKED2021_ENDPOINT_USER_INFO:
                handle2021UserInfo(payload);
                return;
            case CHUNKED2021_ENDPOINT_STEPS:
                handle2021Steps(payload);
                return;
            case CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS:
                handle2021VibrationPatterns(payload);
                return;
            case CHUNKED2021_ENDPOINT_BATTERY:
                handle2021Battery(payload);
                return;
            case CHUNKED2021_ENDPOINT_SILENT_MODE:
                handle2021SilentMode(payload);
                return;
            case CHUNKED2021_ENDPOINT_MUSIC:
                handle2021Music(payload);
                return;
            default:
                LOG.warn("Unhandled 2021 payload {}", String.format("0x%04x", type));
        }
    }

    protected void handle2021Alarms(final byte[] payload) {
        switch (payload[0]) {
            case ALARMS_CMD_CREATE_ACK:
                LOG.info("Alarm create ACK, status = {}", payload[1]);
                return;
            case ALARMS_CMD_DELETE_ACK:
                LOG.info("Alarm delete ACK, status = {}", payload[1]);
                return;
            case ALARMS_CMD_UPDATE_ACK:
                LOG.info("Alarm update ACK, status = {}", payload[1]);
                return;
            case ALARMS_CMD_NOTIFY_CHANGE:
                LOG.info("Alarms changed on band");
                requestAlarms();
                return;
            case ALARMS_CMD_RESPONSE:
                LOG.info("Got alarms from band");
                decodeAndUpdateAlarms(payload);
                return;
            default:
                LOG.warn("Unexpected alarms payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void decodeAndUpdateAlarms(final byte[] payload) {
        final int numAlarms = payload[1];

        if (payload.length != 2 + numAlarms * 10) {
            LOG.warn("Unexpected payload length of {} for {} alarms", payload.length, numAlarms);
            return;
        }

        // Map of alarm position to Alarm, as returned by the band
        final Map<Integer, Alarm> payloadAlarms = new HashMap<>();
        for (int i = 0; i < numAlarms; i++) {
            final Alarm alarm = parseAlarm(payload, 2 + i * 10);
            payloadAlarms.put(alarm.getPosition(), alarm);
        }

        final List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> dbAlarms = DBHelper.getAlarms(gbDevice);
        int numUpdatedAlarms = 0;

        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : dbAlarms) {
            final int pos = alarm.getPosition();
            final Alarm updatedAlarm = payloadAlarms.get(pos);
            final boolean alarmNeedsUpdate = updatedAlarm == null ||
                    alarm.getUnused() != updatedAlarm.getUnused() ||
                    alarm.getEnabled() != updatedAlarm.getEnabled() ||
                    alarm.getSmartWakeup() != updatedAlarm.getSmartWakeup() ||
                    alarm.getHour() != updatedAlarm.getHour() ||
                    alarm.getMinute() != updatedAlarm.getMinute() ||
                    alarm.getRepetition() != updatedAlarm.getRepetition();

            if (alarmNeedsUpdate) {
                numUpdatedAlarms++;
                LOG.info("Updating alarm index={}, unused={}", pos, updatedAlarm == null);
                alarm.setUnused(updatedAlarm == null);
                if (updatedAlarm != null) {
                    alarm.setEnabled(updatedAlarm.getEnabled());
                    alarm.setSmartWakeup(updatedAlarm.getSmartWakeup());
                    alarm.setHour(updatedAlarm.getHour());
                    alarm.setMinute(updatedAlarm.getMinute());
                    alarm.setRepetition(updatedAlarm.getRepetition());
                }
                DBHelper.store(alarm);
            }
        }

        if (numUpdatedAlarms > 0) {
            final Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    private Alarm parseAlarm(final byte[] payload, final int offset) {
        final nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm = new nodomain.freeyourgadget.gadgetbridge.entities.Alarm();

        alarm.setUnused(false); // If the band sent it, it's not unused
        alarm.setPosition(payload[offset + ALARM_IDX_POSITION]);
        alarm.setEnabled((payload[offset + ALARM_IDX_FLAGS] & ALARM_FLAG_ENABLED) > 0);
        alarm.setSmartWakeup((payload[offset + ALARM_IDX_FLAGS] & ALARM_FLAG_SMART) > 0);
        alarm.setHour(payload[offset + ALARM_IDX_HOUR]);
        alarm.setMinute(payload[offset + ALARM_IDX_MINUTE]);
        alarm.setRepetition(payload[offset + ALARM_IDX_REPETITION]);

        return alarm;
    }

    protected void handle2021Calendar(final byte[] payload) {
        switch (payload[0]) {
            case CALENDAR_CMD_EVENTS_RESPONSE:
                LOG.info("Got calendar events from band");
                decodeAndUpdateCalendarEvents(payload);
                return;
            case CALENDAR_CMD_CREATE_EVENT_ACK:
                LOG.info("Calendar create event ACK, status = {}", payload[1]);
                return;
            case CALENDAR_CMD_DELETE_EVENT_ACK:
                LOG.info("Calendar delete event ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected calendar payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void decodeAndUpdateCalendarEvents(final byte[] payload) {
        final int numEvents = payload[1];
        // FIXME there's a 0 after this, is it actually a 2-byte short?

        if (payload.length < 1 + numEvents * 34) {
            LOG.warn("Unexpected payload length of {} for {} calendar events", payload.length, numEvents);
            return;
        }

        int i = 3;
        while (i < payload.length) {
            if (payload.length - i < 34) {
                LOG.error("Not enough bytes remaining to parse a calendar event ({})", payload.length - i);
                return;
            }

            final int eventId = BLETypeConversions.toUint32(payload, i);
            i += 4;

            final String title = StringUtils.untilNullTerminator(payload, i);
            if (title == null) {
                LOG.error("Failed to decode title");
                return;
            }
            i += title.length() + 1;

            final String description = StringUtils.untilNullTerminator(payload, i);
            if (description == null) {
                LOG.error("Failed to decode description");
                return;
            }
            i += description.length() + 1;

            final int startTime = BLETypeConversions.toUint32(payload, i);
            i += 4;

            final int endTime = BLETypeConversions.toUint32(payload, i);
            i += 4;

            // ? 00 00 00 00 00 00 00 00 ff ff ff ff
            i += 12;

            boolean allDay = (payload[i] == 0x01);
            i++;

            // ? 00 82 00 00 00 00
            i += 6;

            LOG.info("Calendar Event {}: {}", eventId, title);
        }

        if (i != payload.length) {
            LOG.error("Unexpected calendar events payload trailer, {} bytes were not consumed", payload.length - i);
            return;
        }

        // TODO update database?
    }

    private void requestConfig(final TransactionBuilder builder,
                               final ConfigType config,
                               final List<Huami2021Config.ConfigArg> args) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(CONFIG_CMD_REQUEST);
        baos.write(args.isEmpty() ? CONFIG_REQUEST_TYPE_ALL : CONFIG_REQUEST_TYPE_SPECIFIC);
        baos.write(config.getValue());
        baos.write(args.size());
        for (final Huami2021Config.ConfigArg arg : args) {
            baos.write(arg.getCode());
        }

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_CONFIG, baos.toByteArray(), true);
    }

    private void requestConfig(final TransactionBuilder builder, final ConfigType config) {
        requestConfig(builder, config, Huami2021Config.ConfigArg.getAllArgsForConfigType(config));
    }

    protected void handle2021Config(final byte[] payload) {
        switch (payload[0]) {
            case CONFIG_CMD_ACK:
                LOG.info("Configuration ACK, status = {}", payload[1]);
                return;

            case CONFIG_CMD_RESPONSE:
                if (payload[1] != 1) {
                    LOG.warn("Configuration response not success: {}", payload[1]);
                    return;
                }

                handle2021ConfigResponse(payload);
                return;
            default:
                LOG.warn("Unexpected configuration payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void handle2021ConfigResponse(final byte[] payload) {
        final ConfigType configType = ConfigType.fromValue(payload[2]);
        if (configType == null) {
            LOG.warn("Unknown config type {}", String.format("0x%02x", payload[2]));
            return;
        }

        int numConfigs = payload[5] & 0xff;

        LOG.info("Got {} configs for {}", numConfigs, configType);

        final Map<String, Object> prefs = new Huami2021Config.ConfigParser(configType)
                .parse(numConfigs, subarray(payload, 6, payload.length));

        if (prefs == null) {
            return;
        }

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(prefs);
        evaluateGBDeviceEvent(eventUpdatePreferences);

        if (isInitialized()) {
            final TransactionBuilder builder;
            boolean hasAutoConfigsToSend = false;

            try {
                builder = performInitialized("set auto band configs");
            } catch (final Exception e) {
                LOG.error("Failed to set auto band configs", e);
                return;
            }

            if (prefs.containsKey(PREF_LANGUAGE) && prefs.get(PREF_LANGUAGE).equals(PREF_LANGUAGE_AUTO)) {
                // Band is reporting automatic language, we need to send the actual language
                setLanguage(builder);
                hasAutoConfigsToSend = true;
            }
            if (prefs.containsKey(PREF_TIMEFORMAT) && prefs.get(PREF_TIMEFORMAT).equals(PREF_TIMEFORMAT_AUTO)) {
                // Band is reporting automatic time format, we need to send the actual time format
                setTimeFormat(builder);
                hasAutoConfigsToSend = true;
            }

            if (hasAutoConfigsToSend) {
                builder.queue(getQueue());
            }
        }
    }

    protected void handle2021Workout(final byte[] payload) {
        switch (payload[0]) {
            case WORKOUT_CMD_APP_OPEN:
                final Huami2021WorkoutTrackActivityType activityType = Huami2021WorkoutTrackActivityType.fromCode(payload[3]);
                final boolean workoutNeedsGps = (payload[2] == 1);

                if (activityType == null) {
                    LOG.warn("Unknown workout activity type {}", String.format("0x%x", payload[3]));
                }

                LOG.info("Workout starting on band: {}, needs gps = {}", activityType, workoutNeedsGps);

                onWorkoutOpen(workoutNeedsGps);
                return;
            case WORKOUT_CMD_STATUS:
                switch (payload[1]) {
                    case WORKOUT_STATUS_START:
                        LOG.info("Workout Start");
                        onWorkoutStart();
                        break;
                    case WORKOUT_STATUS_END:
                        LOG.info("Workout End");
                        onWorkoutEnd();
                        break;
                    default:
                        LOG.warn("Unexpected workout status {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;
            default:
                LOG.warn("Unexpected workout byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021DisplayItems(final byte[] payload) {
        switch (payload[0]) {
            case DISPLAY_ITEMS_CMD_RESPONSE:
                LOG.info("Got display items from band");
                decodeAndUpdateDisplayItems(payload);
                break;
            case DISPLAY_ITEMS_CMD_CREATE_ACK:
                LOG.info("Display items set ACK, type = {}, status = {}", payload[1], payload[2]);
                break;
            default:
                LOG.warn("Unexpected display items payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void decodeAndUpdateDisplayItems(final byte[] payload) {
        final int numberScreens = payload[2];
        final int expectedLength = 4 + numberScreens * 12;
        if (payload.length != 4 + numberScreens * 12) {
            LOG.error("Unexpected display items payload length {}, expected {}", payload.length, expectedLength);
            return;
        }

        final String allScreensPrefKey;
        final String prefKey;
        switch (payload[1]) {
            case DISPLAY_ITEMS_MENU:
                LOG.info("Got {} display items", numberScreens);
                allScreensPrefKey = HuamiConst.PREF_ALL_DISPLAY_ITEMS;
                prefKey = HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
                break;
            case DISPLAY_ITEMS_SHORTCUTS:
                LOG.info("Got {} shortcuts", numberScreens);
                allScreensPrefKey = HuamiConst.PREF_ALL_SHORTCUTS;
                prefKey = HuamiConst.PREF_SHORTCUTS_SORTABLE;
                break;
            default:
                LOG.error("Unknown display items type {}", String.format("0x%x", payload[1]));
                return;
        }

        final String[] mainScreensArr = new String[numberScreens];
        final String[] moreScreensArr = new String[numberScreens];
        final List<String> allScreens = new LinkedList<>();
        if (payload[1] == DISPLAY_ITEMS_MENU) {
            // The band doesn't report the "more" screen, so we add it
            allScreens.add("more");
        }

        for (int i = 0; i < numberScreens; i++) {
            // Screen IDs are sent as literal hex strings
            final String screenId = new String(subarray(payload, 4 + i * 12, 4 + i * 12 + 8));

            allScreens.add(screenId);

            final int screenSectionVal = payload[4 + i * 12 + 9];
            final int screenPosition = payload[4 + i * 12 + 10];

            if (screenPosition >= numberScreens) {
                LOG.warn("Invalid screen position {}, ignoring", screenPosition);
                continue;
            }

            switch (screenSectionVal) {
                case DISPLAY_ITEMS_SECTION_MAIN:
                    if (mainScreensArr[screenPosition] != null) {
                        LOG.warn("Duplicate position {} for main section", screenPosition);
                    }
                    //LOG.debug("mainScreensArr[{}] = {}", screenPosition, screenKey);
                    mainScreensArr[screenPosition] = screenId;
                    break;
                case DISPLAY_ITEMS_SECTION_MORE:
                    if (moreScreensArr[screenPosition] != null) {
                        LOG.warn("Duplicate position {} for more section", screenPosition);
                    }
                    //LOG.debug("moreScreensArr[{}] = {}", screenPosition, screenKey);
                    moreScreensArr[screenPosition] = screenId;
                    break;
                case DISPLAY_ITEMS_SECTION_DISABLED:
                    // Ignore disabled screens
                    //LOG.debug("Ignoring disabled screen {} {}", screenPosition, screenKey);
                    break;
                default:
                    LOG.warn("Unknown screen section {}, ignoring", String.format("0x%02x", screenSectionVal));
            }
        }

        final List<String> screens = new ArrayList<>(Arrays.asList(mainScreensArr));
        if (payload[1] == DISPLAY_ITEMS_MENU) {
            screens.add("more");
            screens.addAll(Arrays.asList(moreScreensArr));
        }
        screens.removeAll(Collections.singleton(null));

        final String allScrensPrefValue = StringUtils.join(",", allScreens.toArray(new String[0])).toString();
        final String prefValue = StringUtils.join(",", screens.toArray(new String[0])).toString();
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(allScreensPrefKey, allScrensPrefValue)
                .withPreference(prefKey, prefValue);

        evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    protected void handle2021FindDevice(final byte[] payload) {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

        switch (payload[0]) {
            case FIND_BAND_ACK:
                LOG.info("Band acknowledged find band command");
                return;
            case FIND_PHONE_START:
                LOG.info("Find Phone Start");
                acknowledgeFindPhone(); // FIXME: premature
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case FIND_PHONE_STOP_FROM_BAND:
                LOG.info("Find Phone Stop");
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            default:
                LOG.warn("Unexpected find phone byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021Http(final byte[] payload) {
        switch (payload[0]) {
            case HTTP_CMD_REQUEST:
                int pos = 1;
                final byte requestId = payload[pos++];
                final String method = StringUtils.untilNullTerminator(payload, pos);
                if (method == null) {
                    LOG.error("Failed to decode method from payload");
                    return;
                }
                pos += method.length() + 1;
                final String url = StringUtils.untilNullTerminator(payload, pos);
                if (url == null) {
                    LOG.error("Failed to decode method from payload");
                    return;
                }
                // headers after pos += url.length() + 1;

                LOG.info("Got HTTP {} request: {}", method, url);

                handleUrlRequest(requestId, method, url);
                return;
            default:
                LOG.warn("Unexpected HTTP payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void handleUrlRequest(final byte requestId, final String method, final String urlString) {
        if (!"GET".equals(method)) {
            LOG.error("Unable to handle HTTP method {}", method);
            // TODO: There's probably a "BAD REQUEST" response or similar
            replyHttpNoInternet(requestId);
            return;
        }

        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            LOG.error("Failed to parse url", e);
            replyHttpNoInternet(requestId);
            return;
        }

        final String path = url.getPath();
        final Map<String, String> query = urlQueryParameters(url);

        if (path.startsWith("/weather/")) {
            final Huami2021Weather.Response response = Huami2021Weather.handleHttpRequest(path, query);

            if (response != null) {
                replyHttpSuccess(requestId, response.toJson());
                return;
            }
        }

        LOG.error("Unhandled URL {}", url);
        replyHttpNoInternet(requestId);
    }

    private Map<String, String> urlQueryParameters(final URL url) {
        final Map<String, String> queryParameters = new HashMap<>();
        final String[] pairs = url.getQuery().split("&");
        for (final String pair : pairs) {
            final String[] parts = pair.split("=", 2);
            try {
                final String key = URLDecoder.decode(parts[0], "UTF-8");
                if (parts.length == 2) {
                    queryParameters.put(key, URLDecoder.decode(parts[1], "UTF-8"));
                } else {
                    queryParameters.put(key, "");
                }
            } catch (final Exception e) {
                LOG.error("Failed to decode query", e);
            }
        }
        return queryParameters;
    }

    private void replyHttpNoInternet(final byte requestId) {
        LOG.info("Replying with no internet to http request {}", requestId);

        final byte[] cmd = new byte[]{HTTP_CMD_RESPONSE, requestId, HTTP_RESPONSE_NO_INTERNET, 0x00, 0x00, 0x00, 0x00};

        writeToChunked2021("http reply no internet", Huami2021Service.CHUNKED2021_ENDPOINT_HTTP, cmd, true);
    }

    private void replyHttpSuccess(final byte requestId, final String content) {
        LOG.debug("Replying with success to http request {} with {}", requestId, content);

        final byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buf = ByteBuffer.allocate(8 + contentBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 0x02);
        buf.put(requestId);
        buf.put(HTTP_RESPONSE_SUCCESS);
        buf.put((byte) 0xc8); // ?
        buf.putInt(contentBytes.length);
        buf.put(contentBytes);

        writeToChunked2021("http reply success", Huami2021Service.CHUNKED2021_ENDPOINT_HTTP, buf.array(), true);
    }

    protected void handle2021HeartRate(final byte[] payload) {
        switch (payload[0]) {
            case HEART_RATE_CMD_REALTIME_ACK:
                // what does the status mean? Seems to be 0 on success
                LOG.info("Band acknowledged heart rate command, status = {}", payload[1]);
                return;
            case HEART_RATE_CMD_SLEEP:
                switch (payload[1]) {
                    case HEART_RATE_FALL_ASLEEP:
                        LOG.info("Fell asleep");
                        processDeviceEvent(HuamiDeviceEvent.FELL_ASLEEP);
                        break;
                    case HEART_RATE_WAKE_UP:
                        LOG.info("Woke up");
                        processDeviceEvent(HuamiDeviceEvent.WOKE_UP);
                        break;
                    default:
                        LOG.warn("Unexpected sleep byte {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;
            default:
                LOG.warn("Unexpected heart rate byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021Notifications(final byte[] payload) {
        final GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
        final GBDeviceEventCallControl deviceEvtCallControl = new GBDeviceEventCallControl();

        switch (payload[0]) {
            case NOTIFICATION_CMD_REPLY:
                // TODO make this configurable?
                final int notificationId = BLETypeConversions.toUint32(subarray(payload, 1, 5));
                final Long replyHandle = (Long) mNotificationReplyAction.lookup(notificationId);
                if (replyHandle == null) {
                    LOG.warn("Failed to find reply handle for notification ID {}", notificationId);
                    return;
                }
                final String replyMessage = StringUtils.untilNullTerminator(payload, 5);
                if (replyMessage == null) {
                    LOG.warn("Failed to parse reply message for notification ID {}", notificationId);
                    return;
                }

                LOG.info("Got reply to notification {} with '{}'", notificationId, replyMessage);

                deviceEvtNotificationControl.handle = replyHandle;
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                deviceEvtNotificationControl.reply = replyMessage;
                evaluateGBDeviceEvent(deviceEvtNotificationControl);

                ackNotificationReply(notificationId); // FIXME: premature?
                onDeleteNotification(notificationId); // FIXME: premature?
                return;
            case NOTIFICATION_CMD_DISMISS:
                switch (payload[1]) {
                    case NOTIFICATION_DISMISS_NOTIFICATION:
                        // TODO make this configurable?
                        final int dismissNotificationId = BLETypeConversions.toUint32(subarray(payload, 2, 6));
                        LOG.info("Dismiss notification {}", dismissNotificationId);
                        deviceEvtNotificationControl.handle = dismissNotificationId;
                        deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS;
                        evaluateGBDeviceEvent(deviceEvtNotificationControl);
                        return;
                    case NOTIFICATION_DISMISS_MUTE_CALL:
                        LOG.info("Mute call");
                        deviceEvtCallControl.event = GBDeviceEventCallControl.Event.IGNORE;
                        evaluateGBDeviceEvent(deviceEvtCallControl);
                        return;
                    case NOTIFICATION_DISMISS_REJECT_CALL:
                        LOG.info("Reject call");
                        deviceEvtCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                        evaluateGBDeviceEvent(deviceEvtCallControl);
                        return;
                    default:
                        LOG.warn("Unexpected notification dismiss byte {}", String.format("0x%02x", payload[1]));
                        return;
                }
            case NOTIFICATION_CMD_ICON_REQUEST:
                final String packageName = StringUtils.untilNullTerminator(payload, 1);
                if (packageName == null) {
                    LOG.error("Failed to decode package name from payload");
                    return;
                }
                LOG.info("Got notification icon request for {}", packageName);

                final int expectedLength = packageName.length() + 7;
                if (payload.length != expectedLength) {
                    LOG.error("Unexpected icon request payload length {}, expected {}", payload.length, expectedLength);
                    return;
                }
                int pos = 1 + packageName.length() + 1;
                // payload[pos] = 0x08?
                pos++;
                int width = BLETypeConversions.toUint16(subarray(payload, pos, pos + 2));
                pos += 2;
                int height = BLETypeConversions.toUint16(subarray(payload, pos, pos + 2));
                sendIconForPackage(packageName, width, height);
                return;
            default:
                LOG.warn("Unexpected notification byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void ackNotificationReply(final int notificationId) {
        final ByteBuffer buf = ByteBuffer.allocate(9);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_REPLY_ACK);
        buf.putInt(notificationId);
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?

        writeToChunked2021("ack notification reply", CHUNKED2021_ENDPOINT_NOTIFICATIONS, buf.array(), true);
    }

    // Package names for which icon is being sent
    // FIXME: This only handles 1 icon at a time
    private String queuedIconPackage;
    // Encoded TGA565 bytes
    private byte[] queuedIconBytes;
    // Keep track of the last time we queued an icon, as a failsafe. If somehow we didn't get the ack
    // after 10 seconds, we'll allow another icon to be sent
    private long queuedIconTimeMillis = 0;

    protected void handle2021Icons(final byte[] payload) {
        switch (payload[0]) {
            case ICONS_CMD_SEND_RESPONSE:
                LOG.info("Band acknowledged icon send request: {}", GB.hexdump(payload));
                // FIXME: The bytes probably mean something..
                sendNextQueuedIconData();
                return;
            case ICONS_CMD_DATA_ACK:
                LOG.info("Band acknowledged icon icon data: {}", GB.hexdump(payload));
                // After the icon is sent to the band, we need to ACK it on the notifications
                // FIXME: The bytes probably mean something..
                ackNotificationAfterIconSent();
                return;
            default:
                LOG.warn("Unexpected icons byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void sendNextQueuedIconData() {
        if (queuedIconPackage == null) {
            LOG.error("No queued icon to send");
            return;
        }

        if (queuedIconBytes == null) {
            LOG.error("No icon bytes for {}", queuedIconPackage);
            return;
        }

        LOG.info("Sending icon data for {}", queuedIconPackage);

        // The band always sends a full 8192 chunk, with zeroes at the end if bytes < 8192
        final ByteBuffer buf = ByteBuffer.allocate(10 + queuedIconBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(ICONS_CMD_DATA_SEND);
        buf.put((byte) 0x03); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x08); // ?
        buf.put((byte) 0x17); // ?
        buf.put(queuedIconBytes);

        writeToChunked2021("send icon data", CHUNKED2021_ENDPOINT_ICONS, buf.array(), false);
    }

    private void ackNotificationAfterIconSent() {
        if (queuedIconPackage == null) {
            LOG.error("No queued icon to ack");
            return;
        }

        LOG.info("Acknowledging icon send for {}", queuedIconPackage);

        final ByteBuffer buf = ByteBuffer.allocate(1 + queuedIconPackage.length() + 1 + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_ICON_REQUEST_ACK);
        buf.put(queuedIconPackage.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put((byte) 0x01);

        queuedIconPackage = null;
        queuedIconBytes = null;

        writeToChunked2021("ack icon send", CHUNKED2021_ENDPOINT_NOTIFICATIONS, buf.array(), true);
    }

    private void sendIconForPackage(final String packageName, final int width, final int height) {
        if (getMTU() < 247) {
            LOG.warn("Sending icons requires high MTU, current MTU is {}", getMTU());
            return;
        }

        if (queuedIconPackage != null && System.currentTimeMillis() - queuedIconTimeMillis < 10_000L) {
            LOG.warn("Icon for {} already queued, not sending icon for {}", queuedIconPackage, packageName);
            return;
        }

        final Drawable icon;
        try {
            icon = getContext().getPackageManager().getApplicationIcon(packageName);
        } catch (final PackageManager.NameNotFoundException e) {
            LOG.error("Failed to get icon for {}", packageName, e);
            return;
        }

        final Bitmap bmp = BitmapUtil.toBitmap(icon);

        // The TGA needs to have this ID, or the band does not accept it
        final byte[] tgaId = new byte[46];
        System.arraycopy("SOMH6".getBytes(StandardCharsets.UTF_8), 0, tgaId, 0, 5);

        final byte[] tga565 = BitmapUtil.convertToTgaRGB565(bmp, width, height, tgaId);

        if (tga565.length > 8192) {
            // FIXME: Pretty sure we can't send more than 8KB in a single request,
            // but don't know how it's supposed to be encoded
            LOG.error("TGA output is too large: {}", tga565.length);
            return;
        }

        final String format = "TGA_RGB565_DAVE2D";
        final String url = String.format(
                Locale.ROOT,
                "notification://logo?app_id=%s&width=%d&height=%d&format=%s",
                packageName,
                width,
                height,
                format
        );
        final String filename = String.format("logo_%s.tga", packageName.replace(".", "_"));

        final ByteBuffer buf = ByteBuffer.allocate(2 + url.length() + 1 + filename.length() + 1 + 4 + 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(ICONS_CMD_SEND_REQUEST);
        buf.put((byte) 0x00);
        buf.put(url.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put(filename.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.putInt(tga565.length);
        buf.putInt(CheckSums.getCRC32(tga565));

        LOG.info("Queueing icon for {}", packageName);
        queuedIconPackage = packageName;
        queuedIconBytes = tga565;
        queuedIconTimeMillis = System.currentTimeMillis();

        writeToChunked2021("send icon send request", CHUNKED2021_ENDPOINT_ICONS, buf.array(), false);
    }

    protected void handle2021Reminders(final byte[] payload) {
        switch (payload[0]) {
            case REMINDERS_CMD_CREATE_ACK:
                LOG.info("Reminder create ACK, status = {}", payload[1]);
                return;
            case REMINDERS_CMD_DELETE_ACK:
                LOG.info("Reminder delete ACK, status = {}", payload[1]);
                // status 1 = success
                // status 2 = reminder not found
                return;
            case REMINDERS_CMD_UPDATE_ACK:
                LOG.info("Reminder update ACK, status = {}", payload[1]);
                return;
            case REMINDERS_CMD_RESPONSE:
                LOG.info("Got reminders from band");
                decodeAndUpdateReminders(payload);
                return;
            default:
                LOG.warn("Unexpected reminders payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void decodeAndUpdateReminders(final byte[] payload) {
        final int numReminders = payload[1];

        if (payload.length < 3 + numReminders * 11) {
            LOG.warn("Unexpected payload length of {} for {} reminders", payload.length, numReminders);
            return;
        }

        // Map of alarm position to Reminder, as returned by the band
        final Map<Integer, Reminder> payloadReminders = new HashMap<>();

        int i = 3;
        while (i < payload.length) {
            if (payload.length - i < 11) {
                LOG.error("Not enough bytes remaining to parse a reminder ({})", payload.length - i);
                return;
            }

            final int reminderPosition = payload[i++] & 0xff;
            final int reminderFlags = BLETypeConversions.toUint32(payload, i);
            i += 4;
            final int reminderTimestamp = BLETypeConversions.toUint32(payload, i);
            i += 4;
            i++; // 0 ?
            final Date reminderDate = new Date(reminderTimestamp * 1000L);
            final String reminderText = StringUtils.untilNullTerminator(payload, i);
            if (reminderText == null) {
                LOG.error("Failed to parse reminder text at pos {}", i);
                return;
            }

            i += reminderText.length() + 1;

            LOG.info("Reminder {}, {}, {}, {}", reminderPosition, String.format("0x%04x", reminderFlags), reminderDate, reminderText);
        }
        if (i != payload.length) {
            LOG.error("Unexpected reminders payload trailer, {} bytes were not consumed", payload.length - i);
            return;
        }

        // TODO persist in database. Probably not trivial, because reminderPosition != reminderId
    }

    protected void handle2021CannedMessages(final byte[] payload) {
        switch (payload[0]) {
            case CANNED_MESSAGES_CMD_RESPONSE:
                LOG.info("Canned Messages response");
                decodeAndUpdateCannedMessagesResponse(payload);
                return;
            case CANNED_MESSAGES_CMD_SET_ACK:
                LOG.info("Canned Message set ACK, status = {}", payload[1]);
                return;
            case CANNED_MESSAGES_CMD_DELETE_ACK:
                LOG.info("Canned Message delete ACK, status = {}", payload[1]);
                return;
            case CANNED_MESSAGES_CMD_REPLY_SMS:
                LOG.info("Canned Message SMS reply");
                handleCannedSmsReply(payload);
                return;
            case CANNED_MESSAGES_CMD_REPLY_SMS_CHECK:
                LOG.info("Canned Message reply SMS check");
                final boolean canSendSms;
                // TODO place this behind a setting as well?
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    canSendSms = getContext().checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
                } else {
                    canSendSms = true;
                }
                sendCannedSmsReplyAllow(canSendSms);
                return;
            default:
                LOG.warn("Unexpected canned messages payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void sendCannedSmsReplyAllow(final boolean allowed) {
        LOG.info("Sending SMS reply allowed = {}", allowed);

        writeToChunked2021(
                "allow sms reply",
                CHUNKED2021_ENDPOINT_CANNED_MESSAGES,
                new byte[]{CANNED_MESSAGES_CMD_REPLY_SMS_ALLOW, bool(allowed)},
                false
        );
    }

    private void handleCannedSmsReply(final byte[] payload) {
        final String phoneNumber = StringUtils.untilNullTerminator(payload, 1);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            LOG.warn("No phone number for SMS reply");
            ackCannedSmsReply(false);
            return;
        }

        final int messageLength = payload[phoneNumber.length() + 6] & 0xff;
        if (phoneNumber.length() + 8 + messageLength != payload.length) {
            LOG.warn("Unexpected message or payload lengths ({} / {})", messageLength, payload.length);
            ackCannedSmsReply(false);
            return;
        }

        final String message = new String(payload, phoneNumber.length() + 8, messageLength);
        if (StringUtils.isNullOrEmpty(message)) {
            LOG.warn("No message for SMS reply");
            ackCannedSmsReply(false);
            return;
        }

        LOG.debug("Sending SMS message '{}' to number '{}' and rejecting call", message, phoneNumber);
        final GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
        devEvtNotificationControl.handle = -1;
        devEvtNotificationControl.phoneNumber = phoneNumber;
        devEvtNotificationControl.reply = message;
        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        evaluateGBDeviceEvent(devEvtNotificationControl);

        final GBDeviceEventCallControl rejectCallCmd = new GBDeviceEventCallControl(GBDeviceEventCallControl.Event.REJECT);
        evaluateGBDeviceEvent(rejectCallCmd);

        ackCannedSmsReply(true); // FIXME probably premature
    }

    private void ackCannedSmsReply(final boolean success) {
        LOG.info("Acknowledging SMS reply, success = {}", success);

        writeToChunked2021(
                "ack sms reply",
                CHUNKED2021_ENDPOINT_CANNED_MESSAGES,
                new byte[]{CANNED_MESSAGES_CMD_REPLY_SMS_ACK, bool(success)},
                false
        );
    }

    private void decodeAndUpdateCannedMessagesResponse(final byte[] payload) {
        final int numberMessages = payload[1] & 0xff;

        LOG.info("Got {} canned messages", numberMessages);

        final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        final Map<Integer, String> cannedMessages = new HashMap<>();

        int pos = 3;
        for (int i = 0; i < numberMessages; i++) {
            if (pos + 4 >= payload.length) {
                LOG.warn("Unexpected end of payload while parsing message {} at pos {}", i, pos);
                return;
            }

            final int messageId = BLETypeConversions.toUint32(subarray(payload, pos, pos + 4));
            final int messageLength = payload[pos + 4] & 0xff;

            if (pos + 6 + messageLength > payload.length) {
                LOG.warn("Unexpected end of payload for message of length {} while parsing message {} at pos {}", messageLength, i, pos);
                return;
            }

            final String messageText = new String(subarray(payload, pos + 6, pos + 6 + messageLength));

            LOG.debug("Canned message {}: {}", String.format("0x%x", messageId), messageText);

            final int cannedMessagePrefId = i + 1;
            if (cannedMessagePrefId > 16) {
                LOG.warn("Canned message ID {} is out of range", cannedMessagePrefId);
            } else {
                cannedMessages.put(cannedMessagePrefId, messageText);
            }

            pos += messageLength + 6;
        }

        for (int i = 1; i <= 16; i++) {
            String message = cannedMessages.get(i);
            if (StringUtils.isEmpty(message)) {
                message = null;
            }

            gbDeviceEventUpdatePreferences.withPreference("canned_reply_" + i, message);
        }

        evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
    }

    protected void handle2021UserInfo(final byte[] payload) {
        // TODO handle2021UserInfo
        LOG.warn("Unexpected user info payload byte {}", String.format("0x%02x", payload[0]));
    }

    protected void handle2021Steps(final byte[] payload) {
        switch (payload[0]) {
            case STEPS_CMD_REPLY:
                LOG.info("Got steps reply, status = {}", payload[1]);
                if (payload.length != 15) {
                    LOG.error("Unexpected steps reply payload length {}", payload.length);
                    return;
                }
                handleRealtimeSteps(subarray(payload, 2, 15));
                return;
            case STEPS_CMD_ENABLE_REALTIME_ACK:
                LOG.info("Band acknowledged realtime steps, status = {}, enabled = {}", payload[1], payload[2]);
                return;
            case STEPS_CMD_REALTIME_NOTIFICATION:
                LOG.info("Got steps notification");
                if (payload.length != 14) {
                    LOG.error("Unexpected steps reply payload length {}", payload.length);
                    return;
                }
                handleRealtimeSteps(subarray(payload, 1, 14));
                return;
            default:
                LOG.warn("Unexpected steps payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021VibrationPatterns(final byte[] payload) {
        switch (payload[0]) {
            case VIBRATION_PATTERN_ACK:
                LOG.info("Vibration Patterns ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected Vibration Patterns payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021Battery(final byte[] payload) {
        if (payload[0] != BATTERY_REPLY) {
            LOG.warn("Unexpected battery payload byte {}", String.format("0x%02x", payload[0]));
            return;
        }

        if (payload.length != 21) {
            LOG.warn("Unexpected battery payload length: {}", payload.length);
        }

        final HuamiBatteryInfo batteryInfo = new HuamiBatteryInfo(subarray(payload, 1, payload.length));
        handleGBDeviceEvent(batteryInfo.toDeviceEvent());
    }

    protected void handle2021SilentMode(final byte[] payload) {
        switch (payload[0]) {
            case SILENT_MODE_CMD_NOTIFY_BAND_ACK:
                LOG.info("Band acknowledged current phone silent mode, status = {}", payload[1]);
                return;
            case SILENT_MODE_CMD_QUERY:
                LOG.info("Got silent mode query from band");
                // TODO sendCurrentSilentMode();
                return;
            case SILENT_MODE_CMD_SET:
                LOG.info("Band setting silent mode = {}", payload[1]);
                // TODO ackSilentModeSet();
                // TODO setSilentMode(payload[1] == 0x01);
                // TODO sendCurrentSilentMode();
                return;
            default:
                LOG.warn("Unexpected silent mode payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021Music(final byte[] payload) {
        switch (payload[0]) {
            case MUSIC_CMD_APP_STATE:
                switch (payload[1]) {
                    case MUSIC_APP_OPEN:
                        onMusicAppOpen();
                        break;
                    case MUSIC_APP_CLOSE:
                        onMusicAppClosed();
                        break;
                    default:
                        LOG.warn("Unexpected music app state {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;

            case MUSIC_CMD_BUTTON_PRESS:
                LOG.info("Got music button press");
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (payload[1]) {
                    case MUSIC_BUTTON_PLAY:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case MUSIC_BUTTON_PAUSE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case MUSIC_BUTTON_NEXT:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case MUSIC_BUTTON_PREVIOUS:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case MUSIC_BUTTON_VOLUME_UP:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case MUSIC_BUTTON_VOLUME_DOWN:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    default:
                        LOG.warn("Unexpected music button {}", String.format("0x%02x", payload[1]));
                        return;
                }
                evaluateGBDeviceEvent(deviceEventMusicControl);
                return;
            default:
                LOG.warn("Unexpected music byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private byte bool(final boolean b) {
        return (byte) (b ? 1 : 0);
    }
}
