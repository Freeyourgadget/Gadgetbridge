/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo, Oleg Vasilev

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import static org.apache.commons.lang3.ArrayUtils.subarray;
import static java.lang.Thread.sleep;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.SUCCESS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint16;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.fromUint8;
import static nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions.mapTimeZone;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_CALORIES;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_FAT_BURN_TIME;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_SLEEP;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_STANDING_TIME;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.FITNESS_GOAL_WEIGHT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.HEART_RATE_ALL_DAY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.LANGUAGE_FOLLOW_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.PASSWORD_ENABLED;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.PASSWORD_TEXT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.SLEEP_HIGH_ACCURACY_MONITORING;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.TEMPERATURE_UNIT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService.ConfigArg.TIME_FORMAT;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSilentMode;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.SleepAsAndroidSender;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Service;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsAgpsInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsGpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiPhoneGpsStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.update.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsFirmwareUpdateOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsAgpsUpdateOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations.ZeppOsGpxRouteUploadOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAgpsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAlarmsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAlexaService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAppsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsCalendarService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsCannedMessagesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsDisplayItemsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsHttpService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLogsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLoyaltyCardService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsMusicService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsNotificationService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsRemindersService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsServicesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsShortcutCardsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsContactsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFtpServerService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsMorningUpdatesService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsPhoneService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWatchfaceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsWifiService;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.SilentMode;

public class ZeppOsSupport extends HuamiSupport implements ZeppOsFileTransferService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsSupport.class);

    // Tracks whether realtime HR monitoring is already started, so we can just
    // send CONTINUE commands
    private boolean heartRateRealtimeStarted;
    private ScheduledExecutorService heartRateRealtimeScheduler;
    // Keep track of whether the rawSensor is enabled
    private boolean rawSensor = false;
    private ScheduledExecutorService rawSensorScheduler;

    // Services
    private final ZeppOsServicesService servicesService = new ZeppOsServicesService(this);
    private final ZeppOsFileTransferService fileTransferService = new ZeppOsFileTransferService(this);
    private final ZeppOsConfigService configService = new ZeppOsConfigService(this);
    private final ZeppOsAgpsService agpsService = new ZeppOsAgpsService(this);
    private final ZeppOsWifiService wifiService = new ZeppOsWifiService(this);
    private final ZeppOsFtpServerService ftpServerService = new ZeppOsFtpServerService(this);
    private final ZeppOsContactsService contactsService = new ZeppOsContactsService(this);
    private final ZeppOsMorningUpdatesService morningUpdatesService = new ZeppOsMorningUpdatesService(this);
    private final ZeppOsPhoneService phoneService = new ZeppOsPhoneService(this);
    private final ZeppOsShortcutCardsService shortcutCardsService = new ZeppOsShortcutCardsService(this);
    private final ZeppOsWatchfaceService watchfaceService = new ZeppOsWatchfaceService(this);
    private final ZeppOsAlarmsService alarmsService = new ZeppOsAlarmsService(this);
    private final ZeppOsCalendarService calendarService = new ZeppOsCalendarService(this);
    private final ZeppOsCannedMessagesService cannedMessagesService = new ZeppOsCannedMessagesService(this);
    private final ZeppOsNotificationService notificationService = new ZeppOsNotificationService(this, fileTransferService);
    private final ZeppOsAlexaService alexaService = new ZeppOsAlexaService(this);
    private final ZeppOsAppsService appsService = new ZeppOsAppsService(this);
    private final ZeppOsLogsService logsService = new ZeppOsLogsService(this);
    private final ZeppOsDisplayItemsService displayItemsService = new ZeppOsDisplayItemsService(this);
    private final ZeppOsHttpService httpService = new ZeppOsHttpService(this);
    private final ZeppOsRemindersService remindersService = new ZeppOsRemindersService(this);
    private final ZeppOsLoyaltyCardService loyaltyCardService = new ZeppOsLoyaltyCardService(this);
    private final ZeppOsMusicService musicService = new ZeppOsMusicService(this);

    private final Set<Short> mSupportedServices = new HashSet<>();
    // FIXME: We need to keep track of which services are encrypted for now, since not all of them were yet migrated to a service
    private final Set<Short> mIsEncrypted = new HashSet<>();
    private final Map<Short, AbstractZeppOsService> mServiceMap = new LinkedHashMap<Short, AbstractZeppOsService>() {{
        put(servicesService.getEndpoint(), servicesService);
        put(fileTransferService.getEndpoint(), fileTransferService);
        put(configService.getEndpoint(), configService);
        put(agpsService.getEndpoint(), agpsService);
        put(wifiService.getEndpoint(), wifiService);
        put(ftpServerService.getEndpoint(), ftpServerService);
        put(contactsService.getEndpoint(), contactsService);
        put(morningUpdatesService.getEndpoint(), morningUpdatesService);
        put(phoneService.getEndpoint(), phoneService);
        put(shortcutCardsService.getEndpoint(), shortcutCardsService);
        put(watchfaceService.getEndpoint(), watchfaceService);
        put(alarmsService.getEndpoint(), alarmsService);
        put(calendarService.getEndpoint(), calendarService);
        put(cannedMessagesService.getEndpoint(), cannedMessagesService);
        put(notificationService.getEndpoint(), notificationService);
        put(alexaService.getEndpoint(), alexaService);
        put(appsService.getEndpoint(), appsService);
        put(logsService.getEndpoint(), logsService);
        put(displayItemsService.getEndpoint(), displayItemsService);
        put(httpService.getEndpoint(), httpService);
        put(remindersService.getEndpoint(), remindersService);
        put(loyaltyCardService.getEndpoint(), loyaltyCardService);
        put(musicService.getEndpoint(), musicService);
    }};

    public ZeppOsSupport() {
        this(LOG);
    }

    public ZeppOsSupport(final Logger logger) {
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

    /**
     * Do not reset the gatt callback implicitly, as that would interrupt operations.
     * See <a href="https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/2912">#2912</a> for more
     * information.
     */
    @Override
    public boolean getImplicitCallbackModify() {
        return false;
    }

    @Override
    public void onSendConfiguration(final String config) {
        final Prefs prefs = getDevicePrefs();

        // FIXME: This should not be handled here
        switch (config) {
            case ActivityUser.PREF_USER_STEPS_GOAL:
            case ActivityUser.PREF_USER_CALORIES_BURNT:
            case ActivityUser.PREF_USER_SLEEP_DURATION:
            case ActivityUser.PREF_USER_GOAL_WEIGHT_KG:
            case ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS:
            case ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES:
                final TransactionBuilder builder = createTransactionBuilder("set fitness goal");
                setFitnessGoal(builder);
                builder.queue(getQueue());
                return;
        }

        // Check if any of the services handles this config
        for (AbstractZeppOsService service : mServiceMap.values()) {
            if (service.onSendConfiguration(config, prefs)) {
                return;
            }
        }

        LOG.warn("Unhandled config {}, will pass to HuamiSupport", config);

        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        setRawSensor(!rawSensor);
    }

    @Override
    protected void acknowledgeFindPhone() {
        LOG.info("Acknowledging find phone");

        final byte[] cmd = new byte[]{FIND_PHONE_ACK, SUCCESS};

        writeToChunked2021("ack find phone", CHUNKED2021_ENDPOINT_FIND_DEVICE, cmd, true);
    }

    protected void stopFindPhone() {
        LOG.info("Stopping find phone");

        writeToChunked2021("found phone", CHUNKED2021_ENDPOINT_FIND_DEVICE, FIND_PHONE_STOP_FROM_PHONE, true);
    }

    @Override
    public void onFindDevice(final boolean start) {
        if (getCoordinator().supportsContinuousFindDevice()) {
            sendFindDeviceCommand(start);
        } else {
            // Vibrate band periodically
            super.onFindDevice(start);
        }
    }

    @Override
    protected void sendFindDeviceCommand(boolean start) {
        final byte findBandCommand = start ? FIND_BAND_START : FIND_BAND_STOP_FROM_PHONE;

        LOG.info("Sending find band {}", start);

        try {
            final TransactionBuilder builder = performInitialized("find huami 2021");
            writeToChunked2021(builder, CHUNKED2021_ENDPOINT_FIND_DEVICE, findBandCommand, true);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("error while sending find Huami 2021 device command", e);
        }
    }

    @Override
    public void onFindPhone(final boolean start) {
        LOG.info("Find phone: {}", start);

        findPhoneStarted = start;

        if (!start) {
            stopFindPhone();
        }
    }

    @Override
    public void onScreenshotReq() {
        appsService.requestScreenshot();
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
    protected ZeppOsSupport sendCalendarEvents(final TransactionBuilder builder) {
        // We have native calendar sync
        CalendarReceiver.forceSync();
        return this;
    }

    @Override
    public void onAddCalendarEvent(final CalendarEventSpec calendarEventSpec) {
        calendarService.addEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(final byte type, final long id) {
        calendarService.deleteEvent(type, id);
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
    protected ZeppOsSupport requestBatteryInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Battery Info");

        writeToChunked2021(builder, CHUNKED2021_ENDPOINT_BATTERY, BATTERY_REQUEST, false);

        return this;
    }

    @Override
    protected ZeppOsSupport setFitnessGoal(final TransactionBuilder builder) {
        final int goalSteps = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        final int goalCalories = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_CALORIES_BURNT, ActivityUser.defaultUserCaloriesBurntGoal);
        final int goalSleep = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_SLEEP_DURATION, ActivityUser.defaultUserSleepDurationGoal);
        final int goalWeight = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_GOAL_WEIGHT_KG, ActivityUser.defaultUserGoalWeightKg);
        final int goalStandingTime = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS, ActivityUser.defaultUserGoalStandingTimeHours);
        final int goalFatBurnTime = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES, ActivityUser.defaultUserFatBurnTimeMinutes);
        LOG.info("Setting Fitness Goals to steps={}, calories={}, sleep={}, weight={}, standingTime={}, fatBurn={}", goalSteps, goalCalories, goalSleep, goalWeight, goalStandingTime, goalFatBurnTime);

        configService.newSetter()
                .setInt(FITNESS_GOAL_STEPS, goalSteps)
                .setShort(FITNESS_GOAL_CALORIES, (short) goalCalories)
                .setShort(FITNESS_GOAL_SLEEP, (short) (goalSleep * 60))
                .setShort(FITNESS_GOAL_WEIGHT, (short) goalWeight)
                .setShort(FITNESS_GOAL_STANDING_TIME, (short) (goalStandingTime))
                .setShort(FITNESS_GOAL_FAT_BURN_TIME, (short) goalFatBurnTime)
                .write(builder);

        return this;
    }

    @Override
    protected ZeppOsSupport setUserInfo(final TransactionBuilder builder) {
        LOG.info("Attempting to set user info...");

        final Prefs prefs = GBApplication.getPrefs();
        final Prefs devicePrefs = getDevicePrefs();

        final String alias = prefs.getString(PREF_USER_NAME, null);
        final ActivityUser activityUser = new ActivityUser();
        final int height = activityUser.getHeightCm();
        final int weight = activityUser.getWeightKg();
        final LocalDate dateOfBirth = activityUser.getDateOfBirth();
        final int birthYear = dateOfBirth.getYear();
        final byte birthMonth = (byte) dateOfBirth.getMonthValue();
        final byte birthDay = (byte) dateOfBirth.getDayOfMonth();
        final String region = devicePrefs.getString(DeviceSettingsPreferenceConst.PREF_DEVICE_REGION, "unknown");

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
            baos.write(USER_INFO_CMD_SET);
            baos.write(new byte[]{0x4f, 0x07, 0x00, 0x00});
            baos.write(fromUint16(birthYear));
            baos.write(birthMonth);
            baos.write(birthDay);
            baos.write(genderByte);
            baos.write(fromUint16(height));
            baos.write(fromUint16(weight * 200));
            baos.write(BLETypeConversions.fromUint64(userid));
            baos.write(region.getBytes(StandardCharsets.UTF_8));
            baos.write(0);
            baos.write(0x09); // TODO ?
            baos.write(alias.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0);

            writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_USER_INFO, baos.toByteArray(), true);
        } catch (final Exception e) {
            LOG.error("Failed to send user info", e);
        }

        return this;
    }

    @Override
    protected ZeppOsSupport setPassword(final TransactionBuilder builder) {
        final boolean passwordEnabled = HuamiCoordinator.getPasswordEnabled(gbDevice.getAddress());
        final String password = HuamiCoordinator.getPassword(gbDevice.getAddress());

        LOG.info("Setting password: {}, {}", passwordEnabled, password);

        if (password == null || password.isEmpty()) {
            LOG.warn("Invalid password: {}", password);
            return this;
        }

        configService.newSetter()
                .setBoolean(PASSWORD_ENABLED, passwordEnabled)
                .setString(PASSWORD_TEXT, password)
                .write(builder);

        return this;
    }

    @Override
    protected void queueAlarm(final Alarm alarm, final TransactionBuilder builder) {
        alarmsService.sendAlarm(alarm, builder);
    }

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        notificationService.setCallState(callSpec);
    }

    @Override
    public void onNotification(final NotificationSpec notificationSpec) {
        notificationService.sendNotification(notificationSpec);
    }

    @Override
    public void onSetReminders(final ArrayList<? extends Reminder> reminders) {
        final TransactionBuilder builder;
        try {
            builder = performInitialized("onSetReminders");
            remindersService.sendReminders(builder, reminders);
            builder.queue(getQueue());
        } catch (final IOException e) {
            LOG.error("Unable to send reminders to device", e);
        }
    }

    @Override
    public void onSetLoyaltyCards(final ArrayList<LoyaltyCard> cards) {
        loyaltyCardService.setCards(cards);
    }

    @Override
    public void onSetContacts(ArrayList<? extends Contact> contacts) {
        //noinspection unchecked
        contactsService.setContacts((List<Contact>) contacts);
    }

    @Override
    protected boolean isWorldClocksEncrypted() {
        return true;
    }

    @Override
    public void onDeleteNotification(final int id) {
        notificationService.deleteNotification(id);
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
        cannedMessagesService.setCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetPhoneVolume(final float volume) {
        musicService.sendVolume(volume);
    }

    @Override
    protected void sendMusicStateToDevice(final MusicSpec musicSpec, final MusicStateSpec musicStateSpec) {
        musicService.sendMusicState(musicSpec, musicStateSpec);
    }

    @Override
    public void onEnableRealtimeSteps(final boolean enable) {
        final byte[] cmd = {STEPS_CMD_ENABLE_REALTIME, bool(enable)};

        writeToChunked2021("toggle realtime steps", CHUNKED2021_ENDPOINT_STEPS, cmd, false);
    }

    @Override
    public UpdateFirmwareOperation createUpdateFirmwareOperation(final Uri uri) {
        throw new UnsupportedOperationException("this method should not be used");
    }

    @Override
    public void onInstallApp(final Uri uri) {
        final ZeppOsAgpsInstallHandler agpsHandler = new ZeppOsAgpsInstallHandler(uri, getContext());
        if (agpsHandler.isValid()) {
            try {
                if (getCoordinator().sendAgpsAsFileTransfer()) {
                    LOG.info("Sending AGPS as file transfer");

                    if (getMTU() == 23) {
                        // AGPS updates without high MTU are too slow and eventually get stuck,
                        // so let's fail right away and inform the user
                        LOG.warn("MTU of {} is too low for AGPS file transfer", getMTU());
                        handleGBDeviceEvent(new GBDeviceEventDisplayMessage(
                                getContext().getString(R.string.updatefirmwareoperation_failed_low_mtu, getMTU()),
                                Toast.LENGTH_LONG,
                                GB.WARN
                        ));
                        return;
                    }

                    new ZeppOsAgpsUpdateOperation(
                            this,
                            agpsHandler.getFile(),
                            agpsService,
                            fileTransferService,
                            configService
                    ).perform();
                } else {
                    LOG.info("Sending AGPS as firmware update");

                    // Write the agps epo update to a temporary file in cache, so we can reuse the firmware update operation
                    final File cacheDir = getContext().getCacheDir();
                    final File agpsCacheDir = new File(cacheDir, "zepp-os-agps");
                    //noinspection ResultOfMethodCallIgnored
                    agpsCacheDir.mkdir();
                    final File uihhFile = new File(agpsCacheDir, "epo-agps.uihh");

                    try (FileOutputStream outputStream = new FileOutputStream(uihhFile)) {
                        outputStream.write(agpsHandler.getFile().getUihhBytes());
                    } catch (final IOException e) {
                        LOG.error("Failed to write agps bytes to temporary uihhFile", e);
                        return;
                    }

                    new ZeppOsFirmwareUpdateOperation(
                            Uri.parse(uihhFile.toURI().toString()),
                            this
                    ).perform();
                }
            } catch (final Exception e) {
                GB.toast(getContext(), "AGPS install error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }

            return;
        }

        final ZeppOsGpxRouteInstallHandler gpxRouteHandler = new ZeppOsGpxRouteInstallHandler(uri, getContext());
        if (gpxRouteHandler.isValid()) {
            try {
                new ZeppOsGpxRouteUploadOperation(
                        this,
                        gpxRouteHandler.getFile(),
                        fileTransferService
                ).perform();
            } catch (final Exception e) {
                GB.toast(getContext(), "Gpx install error: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            }

            return;
        }

        try {
            new ZeppOsFirmwareUpdateOperation(uri, this).perform();
        } catch (final IOException ex) {
            GB.toast(getContext(), "Firmware install error: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onAppInfoReq() {
        // Merge the data from apps and watchfaces
        // This is required because the apps service only knows the versions, not the app type,
        // and the watchface service only knows the app IDs, and not the versions

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        final List<GBDeviceApp> appsFull = new ArrayList<>();

        final Map<UUID, GBDeviceApp> watchfacesById = new HashMap<>();
        final List<GBDeviceApp> watchfaces = watchfaceService.getWatchfaces();
        for (final GBDeviceApp watchface : watchfaces) {
            watchfacesById.put(watchface.getUUID(), watchface);
        }

        final List<GBDeviceApp> apps = appsService.getApps();
        for (final GBDeviceApp app : apps) {
            final GBDeviceApp watchface = watchfacesById.get(app.getUUID());
            if (watchface != null) {
                appsFull.add(new GBDeviceApp(
                        watchface.getUUID(),
                        watchface.getName(),
                        watchface.getCreator(),
                        app.getVersion(),
                        GBDeviceApp.Type.WATCHFACE
                ));
            } else {
                appsFull.add(new GBDeviceApp(
                        app.getUUID(),
                        app.getName(),
                        app.getCreator(),
                        app.getVersion(),
                        GBDeviceApp.Type.APP_GENERIC
                ));
            }
        }

        appInfoCmd.apps = appsFull.toArray(new GBDeviceApp[0]);
        evaluateGBDeviceEvent(appInfoCmd);
    }

    @Override
    public void onAppStart(final UUID uuid, final boolean start) {
        if (start) {
            // This actually also starts apps...
            watchfaceService.setWatchface(uuid);
        }
    }

    @Override
    public void onAppDelete(final UUID uuid) {
        appsService.deleteApp(uuid);
    }

    @Override
    protected ZeppOsSupport setHeartrateSleepSupport(final TransactionBuilder builder) {
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(gbDevice.getAddress());

        configService.newSetter()
                .setBoolean(SLEEP_HIGH_ACCURACY_MONITORING, enableHrSleepSupport)
                .write(builder);

        return this;
    }

    @Override
    public byte[] getTimeBytes(final Calendar calendar, final TimeUnit precision) {
        final byte[] bytes = BLETypeConversions.shortCalendarToRawBytes(calendar);

        if (precision != TimeUnit.MINUTES && precision != TimeUnit.SECONDS) {
            throw new IllegalArgumentException("Unsupported precision, only MINUTES and SECONDS are supported");
        }
        final byte seconds = precision == TimeUnit.SECONDS ? fromUint8(calendar.get(Calendar.SECOND)) : 0;
        final byte tz = BLETypeConversions.mapTimeZone(calendar, BLETypeConversions.TZ_FLAG_INCLUDE_DST_IN_TZ);
        return BLETypeConversions.join(bytes, new byte[]{seconds, tz});
    }

    @Override
    public ZeppOsSupport setCurrentTimeWithService(TransactionBuilder builder) {
        // It seems that the format sent to the Current Time characteristic changed in newer devices
        // to kind-of match the GATT spec, but it doesn't quite respect it?
        // - 11 bytes get sent instead of 10 (extra byte at the end for the offset in quarter-hours?)
        // - Day of week starts at 0
        // Otherwise, the command gets rejected with an "Out of Range" error and init fails.

        final Calendar timestamp = createCalendar();
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
    public HuamiSupport enableNotifications(final TransactionBuilder builder, final boolean enable) {
        builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ), enable);
        return this;
    }

    @Override
    public ZeppOsSupport enableFurtherNotifications(final TransactionBuilder builder,
                                                    final boolean enable) {
        // Nothing to do here, they are already enabled from enableNotifications
        return this;
    }

    @Override
    protected HuamiSupport setHeartrateMeasurementInterval(final TransactionBuilder builder, final int minutes) {
        configService.newSetter()
                .setByte(HEART_RATE_ALL_DAY_MONITORING, (byte) minutes)
                .write(builder);

        return this;
    }

    @Override
    protected boolean supportsDeviceDefaultVibrationProfiles() {
        return true;
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
        buf.put((byte) (profile != null ? 1 : 0)); // 1 for custom, 0 for device default
        buf.put((byte) (test ? 1 : 0));
        buf.put((byte) (onOff.size() / 2));

        for (Short time : onOff) {
            buf.putShort(time);
        }

        writeToChunked2021(builder, Huami2021Service.CHUNKED2021_ENDPOINT_VIBRATION_PATTERNS, buf.array(), true);
    }

    @Override
    public void onSendWeather(final ArrayList<WeatherSpec> weatherSpecs) {
        final WeatherSpec weatherSpec = weatherSpecs.get(0);

        // Weather is not sent directly to the bands, they send HTTP requests for each location.
        // When we have a weather update, set the default location to that location on the band.
        // TODO: Support for multiple weather locations

        final String locationKey = "1.234,-5.678,xiaomi_accu:" + System.currentTimeMillis(); // dummy
        final String locationName = weatherSpec.location;

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(Huami2021Service.WEATHER_CMD_SET_DEFAULT_LOCATION);
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
    public void onSleepAsAndroidAction(String action, Bundle extras) {
        // Validate if our device can work with an action
        try {
            sleepAsAndroidSender.validateAction(action);
        } catch (UnsupportedOperationException e) {
            return;
        }

        // Consult the SleepAsAndroid documentation for a set of actions and their extra
        // https://docs.sleep.urbandroid.org/devs/wearable_api.html
        switch (action) {
            case SleepAsAndroidAction.CHECK_CONNECTED:
                sleepAsAndroidSender.confirmConnected();
                break;
            // Received when the app starts sleep tracking
            case SleepAsAndroidAction.START_TRACKING:
                enableRealtimeHeartRateMeasurement(true);
                enableRawSensor(true);
                sleepAsAndroidSender.startTracking();
                break;
            // Received when the app stops sleep tracking
            case SleepAsAndroidAction.STOP_TRACKING:
                enableRealtimeHeartRateMeasurement(false);
                enableRawSensor(false);
                sleepAsAndroidSender.stopTracking();
                break;
            // Received when the app pauses sleep tracking
//            case SleepAsAndroidAction.SET_PAUSE:
//                long pauseTimestamp = extras.getLong("TIMESTAMP");
//                long delay = pauseTimestamp > 0 ? pauseTimestamp - System.currentTimeMillis() : 0;
//                setRawSensor(delay > 0);
//                enableRealtimeSamplesTimer(delay > 0);
//                sleepAsAndroidSender.pauseTracking(delay);
//                break;
            // Same as above but controlled by a boolean value
            case SleepAsAndroidAction.SET_SUSPENDED:
                boolean suspended = extras.getBoolean("SUSPENDED", false);
                setRawSensor(!suspended);
                enableRealtimeSamplesTimer(!suspended);
                sleepAsAndroidSender.pauseTracking(suspended);
                break;
            // Received when the app changes the batch size for the movement data
            case SleepAsAndroidAction.SET_BATCH_SIZE:
                long batchSize = extras.getLong("SIZE", 12L);
                sleepAsAndroidSender.setBatchSize(batchSize);
                break;
            // Received when the app requests the wearable to vibrate
            case SleepAsAndroidAction.HINT:
                int repeat = extras.getInt("REPEAT");
                for (int i = 0; i < repeat; i++) {
                    sendFindDeviceCommand(true);
                    try {
                        sleep(500);
                        sendFindDeviceCommand(false);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            // Received when the app sends a notificaation
            case SleepAsAndroidAction.SHOW_NOTIFICATION:
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.title = extras.getString("TITLE");
                notificationSpec.body = extras.getString("BODY");
                notificationService.sendNotification(notificationSpec);
                break;
            // Received when the app updates an alarm (Snoozing included too)
            // It's better to use SleepAsAndroidAction.START_ALARM and .STOP_ALARM where possible to have more control over the alarm.
            // Using .UPDATE_ALARM will let Gadgetbridge know when an alarm was set but not when it was dismissed.
            case SleepAsAndroidAction.UPDATE_ALARM:
                long alarmTimestamp = extras.getLong("TIMESTAMP");

                // Sets the alarm at a giver hour and minute
                // Snoozing from the app will create a new alarm in the future
                setSleepAsAndroidAlarm(alarmTimestamp);
                break;
            // Received when an app alarm is stopped
            case SleepAsAndroidAction.STOP_ALARM:
                // Manually stop an alarm
                break;
            // Received when an app alarm starts
            case SleepAsAndroidAction.START_ALARM:
                // Manually start an alarm
                break;
            default:
                LOG.warn("Received unsupported " + action);
                break;
        }
    }

    private void setSleepAsAndroidAlarm(long alarmTimestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Timestamp(alarmTimestamp).getTime());
        Alarm alarm = AlarmUtils.createSingleShot(SleepAsAndroidSender.getAlarmSlot(), false, false, calendar);
        ArrayList<Alarm> alarms = new ArrayList<>(1);
        alarms.add(alarm);

        GBApplication.deviceService(gbDevice).onSetAlarms(alarms);
    }

    private ScheduledExecutorService startRealtimeHeartRateMeasurement() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (heartRateRealtimeStarted) {
                    onEnableRealtimeHeartRateMeasurement(true);
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return service;
    }

    private void stopRealtimeHeartRateMeasurement() {
        if (heartRateRealtimeScheduler != null) {
            heartRateRealtimeScheduler.shutdown();
            heartRateRealtimeScheduler = null;
        }
    }

    private void enableRealtimeHeartRateMeasurement(boolean enable) {
        onEnableRealtimeHeartRateMeasurement(enable);
        if (enable) {
            heartRateRealtimeScheduler = startRealtimeHeartRateMeasurement();
        }
        else {
            stopRealtimeHeartRateMeasurement();
        }

    }

    private void stopRawSensors() {
        if (rawSensorScheduler != null) {
            rawSensorScheduler.shutdown();
            rawSensorScheduler = null;
        }
    }

    private ScheduledExecutorService startRawSensors() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (rawSensor) {
                    setRawSensor(true);
                }
            }
        }, 0, 10000, TimeUnit.MILLISECONDS);
        return service;
    }

    private void enableRawSensor(boolean enable) {
        setRawSensor(enable);
        if (enable) {
            rawSensorScheduler = startRawSensors();
        }
        else {
            stopRawSensors();
        }

    }

    @Override
    protected ZeppOsSupport setTimeFormat(final TransactionBuilder builder) {
        final String timeFormat = getDevicePrefs().getTimeFormat();

        // FIXME: This "works", but the band does not update when the setting changes, so we don't do anything
        //noinspection ConstantValue
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

        configService.newSetter()
                .setByte(TIME_FORMAT, timeFormatByte)
                .write(builder);

        return this;
    }

    @Override
    protected ZeppOsSupport setDistanceUnit(final TransactionBuilder builder) {
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

        configService.newSetter()
                .setByte(TEMPERATURE_UNIT, unitByte)
                .write(builder);

        return this;
    }

    @Override
    protected ZeppOsSupport setLanguage(final TransactionBuilder builder) {
        final String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress())
                .getString("language", "auto");

        LOG.info("Setting device language to {}", localeString);

        configService.newSetter()
                .setByte(LANGUAGE, getLanguageId())
                .setBoolean(LANGUAGE_FOLLOW_PHONE, localeString.equals("auto"))
                .write(builder);

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
    public void writeToChunked2021(final TransactionBuilder builder, final short endpoint, final byte[] data, final boolean encryptIgnored) {
        // Ensure communication for all services contains the encrypted flag reported by the service, since not all
        // watches have the same services encrypted (eg. #3308).
        huami2021ChunkedEncoder.write(builder, endpoint, data, force2021Protocol(), mIsEncrypted.contains(endpoint));
    }

    @Override
    public void writeToConfiguration(final TransactionBuilder builder, final byte[] data) {
        LOG.warn("writeToConfiguration is not supported");
    }

    @Override
    protected ZeppOsSupport requestGPSVersion(final TransactionBuilder builder) {
        LOG.warn("Request GPS version not implemented");
        return this;
    }

    public void requestDisplayItems(final TransactionBuilder builder) {
        displayItemsService.requestItems(builder, ZeppOsDisplayItemsService.DISPLAY_ITEMS_MENU);
    }

    public void requestApps(final TransactionBuilder builder) {
        appsService.requestApps(builder);
    }

    public void requestWatchfaces(final TransactionBuilder builder) {
        watchfaceService.requestWatchfaces(builder);
        watchfaceService.requestCurrentWatchface(builder);
    }

    protected void requestMTU(final TransactionBuilder builder) {
        writeToChunked2021(
                builder,
                CHUNKED2021_ENDPOINT_CONNECTION,
                CONNECTION_CMD_MTU_REQUEST,
                false
        );
    }

    @Override
    public void phase2Initialize(final TransactionBuilder builder) {
        LOG.info("2021 phase2Initialize...");
        requestMTU(builder);
        requestBatteryInfo(builder);

        final GBDeviceEventUpdatePreferences evt = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.WIFI_HOTSPOT_STATUS, null)
                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_ADDRESS, null)
                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_USERNAME, null)
                .withPreference(DeviceSettingsPreferenceConst.FTP_SERVER_STATUS, null);
        evaluateGBDeviceEvent(evt);
    }

    @Override
    public void phase3Initialize(final TransactionBuilder builder) {
        LOG.info("2021 phase3Initialize...");

        // Make sure that performInitialized is not called accidentally in here
        // (eg. by creating a new TransactionBuilder).
        // In those cases, the device will be initialized twice, which will change the shared
        // session key during these requests and decrypting messages will fail

        // In here, we only request the list of supported services - they will all be initialized in
        // initializeServices below
        mSupportedServices.clear();
        mIsEncrypted.clear();
        servicesService.requestServices(builder);
    }

    @Override
    @Deprecated
    public HuamiFWHelper createFWHelper(final Uri uri, final Context context) throws IOException {
        throw new UnsupportedOperationException("This function should not be used for Zepp OS devices");
    }

    public void addSupportedService(final short endpoint, final boolean encrypted) {
        mSupportedServices.add(endpoint);
        if (encrypted) {
            mIsEncrypted.add(endpoint);
        }
    }

    public void initializeServices() {
        LOG.info("2021 initializeServices...");

        try {
            final TransactionBuilder builder = createTransactionBuilder("initialize services");

            // At this point we got the service list from phase 3, so we know which
            // services are supported, and whether they are encrypted or not

            final ZeppOsCoordinator coordinator = getCoordinator();

            // TODO move this to a service
            setUserInfo(builder);

            // TODO move this to a service
            for (final HuamiVibrationPatternNotificationType type : coordinator.getVibrationPatternNotificationTypes(gbDevice)) {
                // FIXME: Can we read these from the band?
                final String typeKey = type.name().toLowerCase(Locale.ROOT);
                setVibrationPattern(builder, HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey);
            }

            // TODO move these to a service
            cannedMessagesService.requestCannedMessages(builder);
            alarmsService.requestAlarms(builder);

            for (AbstractZeppOsService service : mServiceMap.values()) {
                if (mSupportedServices.contains(service.getEndpoint())) {
                    // Only initialize supported services
                    service.initialize(builder);
                }
            }

            if (coordinator.supportsBluetoothPhoneCalls(gbDevice)) {
                phoneService.requestCapabilities(builder);
                phoneService.requestEnabled(builder);
            }

            builder.queue(getQueue());
        } catch (Exception e) {
            LOG.error("failed initializing device", e);
        }
    }

    @Nullable
    public AbstractZeppOsService getService(final short endpoint) {
        return mServiceMap.get(endpoint);
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
    protected ZeppOsCoordinator getCoordinator() {
        return (ZeppOsCoordinator) gbDevice.getDeviceCoordinator();
    }

    @Override
    protected void setRawSensor(final boolean enable) {
        LOG.info("Set raw sensor to {}", enable);
        rawSensor = enable;

        try {
            final TransactionBuilder builder = performInitialized("set raw sensor");
            if (enable) {
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL), Huami2021Service.CMD_RAW_SENSOR_START_1);
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL), Huami2021Service.CMD_RAW_SENSOR_START_2);
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL), Huami2021Service.CMD_RAW_SENSOR_START_3);
            } else {
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL), Huami2021Service.CMD_RAW_SENSOR_STOP);
            }
            builder.notify(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA), enable);
            builder.queue(getQueue());
        } catch (final IOException e) {
            LOG.error("Unable to set raw sensor", e);
        }
    }

    @Override
    protected void handleRawSensorData(final byte[] value) {
        // The g values seem to vary between -4100 and 4100, so we scale them
        final float scaleFactor = 4100f;
        final float gravity = -9.81f;

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        final byte type = buf.get();
        final int index = buf.get() & 0xff; // always incrementing, for each type

        if (type == 0x00) {
            // g-sensor x y z values, per second
            if ((value.length - 2) % 6 != 0) {
                LOG.warn("Raw sensor value for type 0 not divisible by 6");
                return;
            }

            for (int i = 2; i < value.length; i += 6) {
                final int x = (BLETypeConversions.toUint16(value, i) << 16) >> 16;
                final int y = (BLETypeConversions.toUint16(value, i + 2) << 16) >> 16;
                final int z = (BLETypeConversions.toUint16(value, i + 4) << 16) >> 16;

                final float gx = (x * gravity) / scaleFactor;
                final float gy = (y * gravity) / scaleFactor;
                final float gz = (z * gravity) / scaleFactor;
                sleepAsAndroidSender.onAccelChanged(gx, gy, gz);

                LOG.info("Raw sensor g: x={} y={} z={}", gx, gy, gz);
            }
        } else if (type == 0x01) {
            // TODO not sure what this is?
            if ((value.length - 2) % 4 != 0) {
                LOG.warn("Raw sensor value for type 1 not divisible by 4");
                return;
            }

            for (int i = 2; i < value.length; i += 4) {
                int val = BLETypeConversions.toUint32(value, i);
                LOG.info("Raw sensor 1: {}", val);
            }
        } else if (type == 0x07) {
            // Timestamp for the targetType, sent in intervals of ~10 seconds
            final int targetType = buf.get() & 0xff;
            final long tsMillis = buf.getLong();
            LOG.debug("Raw sensor timestamp for type={} index={}: {}", targetType, index, new Date(tsMillis));
        } else {
            LOG.warn("Unknown raw sensor type: {}", GB.hexdump(value));
        }
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3.equals(characteristicUUID)) {
            fileTransferService.onCharacteristicChanged(characteristic.getValue());
            return true;
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void handle2021Payload(final short type, final byte[] payload) {
        if (payload == null || payload.length == 0) {
            LOG.warn("Empty or null payload for {}", String.format("0x%04x", type));
            return;
        }

        final AbstractZeppOsService service = mServiceMap.get(type);
        if (service != null) {
            service.handlePayload(payload);
            return;
        }

        // TODO: Move these services to dedicated classes, so they get the encryption correctly
        switch (type) {
            case CHUNKED2021_ENDPOINT_AUTH:
                LOG.warn("Unexpected auth payload {}", GB.hexdump(payload));
                return;
            case CHUNKED2021_ENDPOINT_COMPAT:
                LOG.warn("Unexpected compat payload {}", GB.hexdump(payload));
                return;
            case CHUNKED2021_ENDPOINT_WEATHER:
                handle2021Weather(payload);
                return;
            case CHUNKED2021_ENDPOINT_WORKOUT:
                handle2021Workout(payload);
                return;
            case CHUNKED2021_ENDPOINT_FIND_DEVICE:
                handle2021FindDevice(payload);
                return;
            case CHUNKED2021_ENDPOINT_HEARTRATE:
                handle2021HeartRate(payload);
                return;
            case CHUNKED2021_ENDPOINT_CONNECTION:
                handle2021Connection(payload);
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
            default:
                LOG.warn("Unhandled 2021 payload {}", String.format("0x%04x", type));
        }
    }

    protected void handle2021Workout(final byte[] payload) {
        switch (payload[0]) {
            case WORKOUT_CMD_APP_OPEN:
                final ZeppOsActivityType activityType = ZeppOsActivityType.fromCode(payload[3]);
                final boolean workoutNeedsGps = (payload[2] == 1);
                final ActivityKind activityKind;

                if (activityType == null) {
                    LOG.warn("Unknown workout activity type {}", String.format("0x%x", payload[3]));
                    activityKind = ActivityKind.UNKNOWN;
                } else {
                    activityKind = activityType.toActivityKind();
                }

                LOG.info("Workout starting on band: {}, needs gps = {}", activityType, workoutNeedsGps);

                onWorkoutOpen(workoutNeedsGps, activityKind);
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

    /**
     * A handler to schedule the find phone event.
     */
    private final Handler findPhoneHandler = new Handler();
    private boolean findPhoneStarted;

    protected void handle2021FindDevice(final byte[] payload) {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();

        switch (payload[0]) {
            case FIND_BAND_ACK:
                LOG.info("Band acknowledged find band command");
                return;
            case FIND_PHONE_START:
                LOG.info("Find Phone Start");
                acknowledgeFindPhone(); // FIXME: Premature, but the band will only send the mode after we ack

                // Delay the find phone start, because we might get the FIND_PHONE_MODE
                findPhoneHandler.postDelayed(() -> {
                    findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                    evaluateGBDeviceEvent(findPhoneEvent);
                }, 1500);

                break;
            case FIND_BAND_STOP_FROM_BAND:
                LOG.info("Find Band Stop from Band");
                break;
            case FIND_PHONE_STOP_FROM_BAND:
                LOG.info("Find Phone Stop");
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            case FIND_PHONE_MODE:
                findPhoneHandler.removeCallbacksAndMessages(null);

                final int mode = payload[1] & 0xff; // 0 to only vibrate, 1 to ring
                LOG.info("Find Phone Mode: {}", mode);
                if (findPhoneStarted) {
                    // Already started, just change the mode
                    findPhoneEvent.event = mode == 1 ? GBDeviceEventFindPhone.Event.RING : GBDeviceEventFindPhone.Event.VIBRATE;
                } else {
                    findPhoneEvent.event = mode == 1 ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.START_VIBRATE;
                }
                evaluateGBDeviceEvent(findPhoneEvent);
                break;
            default:
                LOG.warn("Unexpected find phone byte {}", String.format("0x%02x", payload[0]));
        }
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

    protected void handle2021Weather(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case WEATHER_CMD_DEFAULT_LOCATION_ACK:
                LOG.info("Weather default location ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected weather byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021Connection(final byte[] payload) {
        switch (payload[0]) {
            case CONNECTION_CMD_MTU_RESPONSE:
                final int mtu = BLETypeConversions.toUint16(payload, 1) + 3;
                LOG.info("Device announced MTU change: {}", mtu);
                setMtu(mtu);
                return;
            case CONNECTION_CMD_UNKNOWN_3:
                // Some ping? Band sometimes sends 0x03, phone replies with 0x04
                LOG.info("Got unknown 3, replying with unknown 4");
                writeToChunked2021("respond connection unknown 4", CHUNKED2021_ENDPOINT_CONNECTION, CONNECTION_CMD_UNKNOWN_4, false);
                return;
        }

        LOG.warn("Unexpected connection payload byte {}", String.format("0x%02x", payload[0]));
    }

    protected void handle2021UserInfo(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case USER_INFO_CMD_SET_ACK:
                LOG.info("Got user info set ack, status = {}", payload[1]);
                return;
        }

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
                    LOG.error("Unexpected realtime notification payload length {}", payload.length);
                    return;
                }
                handleRealtimeSteps(subarray(payload, 1, 14));
                return;
            default:
                LOG.warn("Unexpected steps payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    protected void handle2021VibrationPatterns(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
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
                sendPhoneSilentMode(SilentMode.isPhoneInSilenceMode(getDevice().getAddress()));
                return;
            case SILENT_MODE_CMD_SET:
                LOG.info("Band setting silent mode = {}", payload[1]);
                final boolean silentModeEnabled = (payload[1] == 1);
                ackSilentModeSet();
                sendPhoneSilentMode(silentModeEnabled);
                evaluateGBDeviceEvent(new GBDeviceEventSilentMode(silentModeEnabled));
                return;
            default:
                LOG.warn("Unexpected silent mode payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void ackSilentModeSet() {
        writeToChunked2021(
                "ack silent mode set",
                CHUNKED2021_ENDPOINT_SILENT_MODE,
                new byte[]{SILENT_MODE_CMD_ACK, 0x01},
                false
        );
    }

    private void sendPhoneSilentMode(final boolean enabled) {
        writeToChunked2021(
                "send phone silent mode to band",
                CHUNKED2021_ENDPOINT_SILENT_MODE,
                new byte[]{SILENT_MODE_CMD_NOTIFY_BAND, bool(enabled)},
                false
        );
    }

    @Override
    public void onFileUploadFinish(final boolean success) {
        LOG.warn("Unexpected file upload finish: {}", success);
    }

    @Override
    public void onFileUploadProgress(final int progress) {
        LOG.warn("Unexpected file upload progress: {}", progress);
    }

    @Override
    public void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
        LOG.info("File received: url={} filename={} length={}", url, filename, data.length);

        if (filename.startsWith("screenshot-")) {
            GBDeviceEventScreenshot gbDeviceEventScreenshot = new GBDeviceEventScreenshot(data);
            evaluateGBDeviceEvent(gbDeviceEventScreenshot);
            return;
        }

        final String fileDownloadsDir = "zepp-os-received-files";
        final File targetFile;
        try {
            final String validFilename = FileUtils.makeValidFileName(filename);
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), fileDownloadsDir);
            //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
            targetFile = new File(targetFolder, validFilename);
        } catch (final IOException e) {
            LOG.error("Failed create folder to save file", e);
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), fileDownloadsDir);
            //noinspection ResultOfMethodCallIgnored
            targetFolder.mkdirs();
            outputStream.write(data);
        } catch (final IOException e) {
            LOG.error("Failed to save file bytes", e);
        }
    }

    private byte bool(final boolean b) {
        return (byte) (b ? 1 : 0);
    }
}
