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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;

public class XiaomiHealthService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiHealthService.class);

    public static final int COMMAND_TYPE = 8;

    private static final int CMD_SET_USER_INFO = 0;

    private static final int GENDER_MALE = 1;
    private static final int GENDER_FEMALE = 2;

    public XiaomiHealthService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        // TODO
        LOG.warn("Unhandled health command");
    }

    @Override
    public void initialize(TransactionBuilder builder) {
        setUserInfo(builder);
    }

    public void setUserInfo(final TransactionBuilder builder) {
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
        // TODO
    }

    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        // TODO
    }

    public void onEnableRealtimeSteps(final boolean enable) {
        // TODO
    }
}
