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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiHealthService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiHealthService.class);

    public static final int COMMAND_TYPE = 8;

    private static final int CMD_SET_USER_INFO = 0;
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
            case CMD_REALTIME_STATS_EVENT:
                handleRealtimeStats(cmd.getHealth().getRealTimeStats());
                return;
        }

        LOG.warn("Unknown health command {}", cmd.getSubtype());
    }

    @Override
    public void initialize(TransactionBuilder builder) {
        setUserInfo(builder);
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case ActivityUser.PREF_USER_HEIGHT_CM:
            case ActivityUser.PREF_USER_WEIGHT_KG:
            case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
            case ActivityUser.PREF_USER_GENDER:
            case ActivityUser.PREF_USER_CALORIES_BURNT:
            case ActivityUser.PREF_USER_STEPS_GOAL:
            case ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS:
            case ActivityUser.PREF_USER_ACTIVETIME_MINUTES:
                final TransactionBuilder builder = getSupport().createTransactionBuilder("set user info");
                setUserInfo(builder);
                builder.queue(getSupport().getQueue());
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
            maxHeartRate  = 175;
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
