/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiHealthService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiHealthService.class);

    public static final int COMMAND_TYPE = 8;

    private static final int CMD_SET_USER_INFO = 0;
    private static final int CMD_ACTIVITY_FETCH_1 = 1;
    private static final int CMD_ACTIVITY_FETCH_2 = 2;
    private static final int CMD_CONFIG_SPO2_GET = 8;
    private static final int CMD_CONFIG_SPO2_SET = 9;
    private static final int CMD_CONFIG_HEART_RATE_GET = 10;
    private static final int CMD_CONFIG_HEART_RATE_SET = 11;
    private static final int CMD_CONFIG_STANDING_REMINDER_GET = 12;
    private static final int CMD_CONFIG_STANDING_REMINDER_SET = 13;
    private static final int CMD_CONFIG_STRESS_GET = 14;
    private static final int CMD_CONFIG_STRESS_SET = 15;
    private static final int CMD_REALTIME_STATS_START = 45;
    private static final int CMD_REALTIME_STATS_STOP = 46;
    private static final int CMD_REALTIME_STATS_EVENT = 47;

    private static final int GENDER_MALE = 1;
    private static final int GENDER_FEMALE = 2;

    private boolean realtimeStarted = false;
    private boolean realtimeOneShot = false;
    private int previousSteps = -1;

    public XiaomiHealthService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_SET_USER_INFO:
                LOG.debug("Got user info set ack, status={}", cmd.getStatus());
                return;
            case CMD_ACTIVITY_FETCH_1:
            case CMD_ACTIVITY_FETCH_2:
                handleActivityFetchResponse(cmd.getHealth().getActivityRecordIds().toByteArray());
                return;
            case CMD_CONFIG_SPO2_GET:
                handleSpo2Config(cmd.getHealth().getSpo2());
                return;
            case CMD_CONFIG_HEART_RATE_GET:
                handleHeartRateConfig(cmd.getHealth().getHeartRate());
                return;
            case CMD_CONFIG_STANDING_REMINDER_GET:
                handleStandingReminderConfig(cmd.getHealth().getStandingReminder());
                return;
            case CMD_CONFIG_STRESS_GET:
                handleStressConfig(cmd.getHealth().getStress());
                return;
            case CMD_REALTIME_STATS_EVENT:
                handleRealtimeStats(cmd.getHealth().getRealTimeStats());
                return;
        }

        LOG.warn("Unknown health command {}", cmd.getSubtype());
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        setUserInfo(builder);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_CONFIG_SPO2_GET);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_CONFIG_HEART_RATE_GET);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_CONFIG_STANDING_REMINDER_GET);
        getSupport().sendCommand(builder, COMMAND_TYPE, CMD_CONFIG_STRESS_GET);
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        final TransactionBuilder builder = getSupport().createTransactionBuilder("set " + config);

        switch (config) {
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
            case ActivityUser.PREF_USER_GENDER:
            case ActivityUser.PREF_USER_CALORIES_BURNT:
            case ActivityUser.PREF_USER_STEPS_GOAL:
            case ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS:
            case ActivityUser.PREF_USER_ACTIVETIME_MINUTES:
                setUserInfo(builder);
                builder.queue(getSupport().getQueue());
                return true;
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ENABLED:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD:
                setHeartRateConfig(builder);
                return true;
            case DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING:
            case DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD:
                setSpo2Config(builder);
                return true;
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_END:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START:
            case DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END:
                setStandingReminderConfig(builder);
                return true;
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING:
            case DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_RELAXATION_REMINDER:
                setStressConfig(builder);
                return true;
        }

        return false;
    }

    public void setUserInfo(final TransactionBuilder builder) {
        LOG.debug("Setting user info");

        final ActivityUser activityUser = new ActivityUser();
        final int birthYear = activityUser.getYearOfBirth();
        final byte birthMonth = 7; // not in user attributes
        final byte birthDay = 1; // not in user attributes

        final int genderInt = activityUser.getGender() != ActivityUser.GENDER_FEMALE ? GENDER_MALE : GENDER_FEMALE;  // TODO other gender?

        final Calendar now = GregorianCalendar.getInstance();
        final int age = now.get(Calendar.YEAR) - birthYear;
        // Compute the approximate max heart rate from the user age
        // TODO max heart rate should be input by the user
        int maxHeartRate = (int) Math.round(age <= 40 ? 220 - age : 207 - 0.7 * age);
        if (maxHeartRate < 100 || maxHeartRate > 220) {
            maxHeartRate = 175;
        }

        final XiaomiProto.UserInfo userInfo = XiaomiProto.UserInfo.newBuilder()
                .setHeight(activityUser.getHeightCm())
                .setWeight(activityUser.getWeightKg())
                .setBirthday(Integer.parseInt(String.format(Locale.ROOT, "%02d%02d%02d", birthYear, birthMonth, birthDay)))
                .setGender(genderInt)
                .setMaxHeartRate(maxHeartRate)
                .setGoalCalories(activityUser.getCaloriesBurntGoal())
                .setGoalSteps(activityUser.getStepsGoal())
                .setGoalStanding(activityUser.getStandingTimeGoalHours())
                .setGoalMoving(activityUser.getActiveTimeGoalMinutes())
                .build();

        final XiaomiProto.Health health = XiaomiProto.Health.newBuilder()
                .setUserInfo(userInfo)
                .build();

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SET_USER_INFO)
                        .setHealth(health)
                        .build()
        );
    }

    private void handleSpo2Config(final XiaomiProto.SpO2 spo2) {
        LOG.debug("Got SpO2 config");

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING, spo2.getAllDayTracking())
                .withPreference(
                        DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD,
                        String.valueOf(spo2.getAlarmLow().getAlarmLowEnabled() ? spo2.getAlarmLow().getAlarmLowThreshold() : 0)
                );

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setSpo2Config(final TransactionBuilder builder) {
        LOG.debug("Set SpO2 config");

        final Prefs prefs = getDevicePrefs();
        final boolean allDayMonitoring = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING, false);
        final int lowAlertThreshold = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SPO2_LOW_ALERT_THRESHOLD, 0);

        final XiaomiProto.Spo2AlarmLow.Builder spo2alarmLowBuilder = XiaomiProto.Spo2AlarmLow.newBuilder()
                .setAlarmLowEnabled(lowAlertThreshold != 0);

        if (lowAlertThreshold != 0) {
            spo2alarmLowBuilder.setAlarmLowThreshold(lowAlertThreshold);
        }

        final XiaomiProto.SpO2.Builder spo2 = XiaomiProto.SpO2.newBuilder()
                .setUnknown1(1)
                .setAllDayTracking(allDayMonitoring)
                .setAlarmLow(spo2alarmLowBuilder);

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CONFIG_SPO2_SET)
                        .setHealth(XiaomiProto.Health.newBuilder().setSpo2(spo2))
                        .build()
        );
    }

    private void handleHeartRateConfig(final XiaomiProto.HeartRate heartRate) {
        LOG.debug("Got heart rate config");

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        if (heartRate.getDisabled()) {
            eventUpdatePreferences.withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL, "0");
        } else if (heartRate.getInterval() == 0) {
            // smart
            eventUpdatePreferences.withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL, "-1");
        } else {
            eventUpdatePreferences.withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL, String.valueOf(heartRate.getInterval()));
        }

        eventUpdatePreferences.withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION, heartRate.getAdvancedMonitoring().getEnabled());
        eventUpdatePreferences.withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING, heartRate.getBreathingScore() == 1);

        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD,
                String.valueOf(heartRate.getAlarmHighEnabled() ? heartRate.getAlarmHighThreshold() : 0)
        );

        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD,
                String.valueOf(heartRate.getHeartRateAlarmLow().getAlarmLowEnabled() ? heartRate.getHeartRateAlarmLow().getAlarmLowThreshold() : 0)
        );

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setHeartRateConfig(final TransactionBuilder builder) {
        final Prefs prefs = getDevicePrefs();

        final boolean sleepDetection = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION, false);
        final boolean sleepBreathingQuality = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING, false);
        final int intervalMin = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL, 0);
        final int alertHigh = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD, 0);
        final int alertLow = prefs.getInt(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD, 0);

        final XiaomiProto.HeartRate.Builder heartRate = XiaomiProto.HeartRate.newBuilder()
                .setDisabled(intervalMin == 0)
                .setInterval(Math.max(intervalMin, 0)) // smart will be -1 from pref
                .setAdvancedMonitoring(XiaomiProto.AdvancedMonitoring.newBuilder()
                        .setEnabled(sleepDetection))
                .setBreathingScore(sleepBreathingQuality ? 1 : 2)
                .setAlarmHighEnabled(alertHigh > 0)
                .setAlarmHighThreshold(alertHigh)
                .setHeartRateAlarmLow(XiaomiProto.HeartRateAlarmLow.newBuilder()
                        .setAlarmLowEnabled(alertLow > 0)
                        .setAlarmLowThreshold(alertLow))
                .setUnknown7(1);

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CONFIG_HEART_RATE_SET)
                        .setHealth(XiaomiProto.Health.newBuilder().setHeartRate(heartRate))
                        .build()
        );
    }

    private void handleStandingReminderConfig(final XiaomiProto.StandingReminder standingReminder) {
        LOG.debug("Got standing reminder config");

        final String start = XiaomiPreferences.prefFromHourMin(standingReminder.getStart());
        final String end = XiaomiPreferences.prefFromHourMin(standingReminder.getEnd());
        final String dndStart = XiaomiPreferences.prefFromHourMin(standingReminder.getDndStart());
        final String dndEnd = XiaomiPreferences.prefFromHourMin(standingReminder.getDndEnd());

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, standingReminder.getEnabled())
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, start)
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, end)
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, standingReminder.getDnd())
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START, dndStart)
                .withPreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END, dndEnd);

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setStandingReminderConfig(final TransactionBuilder builder) {
        LOG.debug("Set standing reminder config");

        final Prefs prefs = getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false);
        final Date start = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "06:00");
        final Date end = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "22:00");
        final boolean dnd = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, false);
        final Date dndStart = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_START, "12:00");
        final Date dndEnd = prefs.getTimePreference(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND_END, "14:00");

        final XiaomiProto.StandingReminder standingReminder = XiaomiProto.StandingReminder.newBuilder()
                .setEnabled(enabled)
                .setStart(XiaomiPreferences.prefToHourMin(start))
                .setEnd(XiaomiPreferences.prefToHourMin(end))
                .setDnd(dnd)
                .setDndStart(XiaomiPreferences.prefToHourMin(dndStart))
                .setDndEnd(XiaomiPreferences.prefToHourMin(dndEnd))
                .build();

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CONFIG_STANDING_REMINDER_SET)
                        .setHealth(XiaomiProto.Health.newBuilder().setStandingReminder(standingReminder))
                        .build()
        );
    }

    private void handleStressConfig(final XiaomiProto.Stress stress) {
        LOG.debug("Got stress config");

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING, stress.getAllDayTracking())
                .withPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_RELAXATION_REMINDER, stress.getRelaxReminder().getEnabled());

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setStressConfig(final TransactionBuilder builder) {
        LOG.debug("Set stress config");

        final Prefs prefs = getDevicePrefs();
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING, false);
        final boolean relaxReminder = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_RELAXATION_REMINDER, false);

        final XiaomiProto.Stress.Builder stress = XiaomiProto.Stress.newBuilder()
                .setAllDayTracking(enabled)
                .setRelaxReminder(XiaomiProto.RelaxReminder.newBuilder().setEnabled(relaxReminder).setUnknown2(0));

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CONFIG_STRESS_SET)
                        .setHealth(XiaomiProto.Health.newBuilder().setStress(stress))
                        .build()
        );
    }

    public void onFetchRecordedData(final int dataTypes) {
        LOG.debug("Fetch recorded data: {}", dataTypes);
        final TransactionBuilder builder = getSupport().createTransactionBuilder("fetch recorded data step 1");

        getSupport().sendCommand(
                builder,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_ACTIVITY_FETCH_1)
                        .setHealth(XiaomiProto.Health.newBuilder().setActivitySyncRequest1(
                                // TODO official app sends 0, but doesn't work every time?
                                XiaomiProto.ActivitySyncRequest1.newBuilder().setUnknown1(1)
                        ))
                        .build()
        );

        // TODO we need to wait for the reply from the previous before sending this one?
        //getSupport().sendCommand(
        //        builder,
        //        XiaomiProto.Command.newBuilder()
        //                .setType(COMMAND_TYPE)
        //                .setSubtype(CMD_ACTIVITY_FETCH_2)
        //                .build()
        //);

        builder.queue(getSupport().getQueue());
    }

    public void handleActivityFetchResponse(final byte[] recordIds) {
        if ((recordIds.length % 7) != 0) {
            LOG.warn("recordIds {} length = {}, not a multiple of 7, can't parse", GB.hexdump(recordIds), recordIds.length);
            return;

        }

        LOG.debug("Got {} record IDs", recordIds.length / 7);

        final ByteBuffer buf = ByteBuffer.wrap(recordIds).order(ByteOrder.LITTLE_ENDIAN);

        while (buf.position() < buf.limit()) {
            final int ts = buf.getInt();
            final int tz = buf.get(); // 15 min blocks
            final int version = buf.get();
            final int flags = buf.get();
            // bit 0 is type - 0 activity, 1 sports
            final int type = (flags >> 7) & 1;
            // bit 1 to 6 bits are subtype
            //   for activity: activity, sleep, measurements, etc
            //   for workout: workout type (8 freestyle)
            final int subtype = (flags & 127) >> 2;
            // bit 6 and 7 - 0/1 - summary vs details
            final int detailType = flags & 3;
            LOG.debug(
                    "Activity Record: ts = {}, tz = {}, flags = {}, type = {}, subtype = {}, detailType = {}, version = {}",
                    ts, tz, String.format("0x%02X", flags), type, subtype, detailType, version
            );
        }
    }

    public void onHeartRateTest() {
        LOG.debug("Trigger heart rate one-shot test");

        realtimeStarted = true;
        realtimeOneShot = true;

        getSupport().sendCommand(
                "heart rate test",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_REALTIME_STATS_START)
                        .build()
        );
    }

    public void enableRealtimeStats(final boolean enable) {
        LOG.debug("Enable realtime stats: {}", enable);

        if (realtimeStarted == enable) {
            // same state, ignore
            return;
        }

        realtimeStarted = enable;
        realtimeOneShot = false;
        previousSteps = -1;

        getSupport().sendCommand(
                "realtime data",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(enable ? CMD_REALTIME_STATS_START : CMD_REALTIME_STATS_STOP)
                        .build()
        );
    }

    private void handleRealtimeStats(final XiaomiProto.RealTimeStats realTimeStats) {
        LOG.debug("Got realtime stats");

        if (!realtimeOneShot && !realtimeStarted) {
            // Failsafe in case it gets out of sync, stop it
            enableRealtimeStats(false);
            return;
        }

        if (realtimeOneShot) {
            if (realTimeStats.getHeartRate() <= 10) {
                return;
            }
            enableRealtimeStats(false);
        }

        if (previousSteps == -1) {
            previousSteps = realTimeStats.getSteps();
        }

        final HuamiExtendedActivitySample sample;
        try (final DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();

            final GBDevice gbDevice = getSupport().getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final int ts = (int) (System.currentTimeMillis() / 1000);
            final XiaomiSampleProvider provider = new XiaomiSampleProvider(gbDevice, session);
            sample = provider.createActivitySample();

            sample.setDeviceId(device.getId());
            sample.setUserId(user.getId());
            sample.setTimestamp(ts);
            sample.setHeartRate(realTimeStats.getHeartRate());
            sample.setSteps(realTimeStats.getSteps() - previousSteps);
            sample.setRawKind(ActivityKind.TYPE_UNKNOWN);
            sample.setHeartRate(realTimeStats.getHeartRate());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setRawKind(ActivityKind.TYPE_UNKNOWN);
        } catch (final Exception e) {
            LOG.error("Error creating activity sample", e);
            return;
        }

        previousSteps = realTimeStats.getSteps();

        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
        LocalBroadcastManager.getInstance(getSupport().getContext()).sendBroadcast(intent);
    }
}
