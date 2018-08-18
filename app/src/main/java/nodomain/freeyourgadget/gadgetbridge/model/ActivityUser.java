/*  Copyright (C) 2016-2018 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * Class holding the common user information needed by most activity trackers
 */
public class ActivityUser {

    public static final int GENDER_FEMALE = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_OTHER = 2;

    private String activityUserName;
    private int activityUserGender;
    private int activityUserYearOfBirth;
    private int activityUserHeightCm;
    private int activityUserWeightKg;
    private int activityUserSleepDuration;
    private int activityUserStepsGoal;

    private static final String defaultUserName = "gadgetbridge-user";
    public static final int defaultUserGender = GENDER_FEMALE;
    public static final int defaultUserYearOfBirth = 0;
    public static final int defaultUserAge = 0;
    public static final int defaultUserHeightCm = 175;
    public static final int defaultUserWeightKg = 70;
    public static final int defaultUserSleepDuration = 7;
    public static final int defaultUserStepsGoal = 8000;

    public static final String PREF_USER_NAME = "mi_user_alias";
    public static final String PREF_USER_YEAR_OF_BIRTH = "activity_user_year_of_birth";
    public static final String PREF_USER_GENDER = "activity_user_gender";
    public static final String PREF_USER_HEIGHT_CM = "activity_user_height_cm";
    public static final String PREF_USER_WEIGHT_KG = "activity_user_weight_kg";
    public static final String PREF_USER_SLEEP_DURATION = "activity_user_sleep_duration";
    public static final String PREF_USER_STEPS_GOAL = "mi_fitness_goal"; // FIXME: for compatibility

    public ActivityUser() {
        fetchPreferences();
    }

    public String getName() {
        return activityUserName;
    }

    public int getWeightKg() {
        return activityUserWeightKg;
    }

    /**
     * @see #GENDER_FEMALE
     * @see #GENDER_MALE
     * @see #GENDER_OTHER
     */
    public int getGender() {
        return activityUserGender;
    }

    public int getYearOfBirth() {
        return activityUserYearOfBirth;
    }

    public int getHeightCm() {
        return activityUserHeightCm;
    }

    /**
     * @return the user defined sleep duration or the default value when none is set or the stored
     * value is out of any logical bounds.
     */
    public int getSleepDuration() {
        if (activityUserSleepDuration < 1 || activityUserSleepDuration > 24) {
            activityUserSleepDuration = defaultUserSleepDuration;
        }
        return activityUserSleepDuration;
    }

    public int getStepsGoal() {
        if (activityUserStepsGoal < 0) {
            activityUserStepsGoal = defaultUserStepsGoal;
        }
        return activityUserStepsGoal;
    }

    public int getAge() {
        int userYear = getYearOfBirth();
        int age = 25;
        if (userYear > 1900) {
            age = Calendar.getInstance().get(Calendar.YEAR) - userYear;
            if (age <= 0) {
                age = 25;
            }
        }
        return age;
    }

    private void fetchPreferences() {
        Prefs prefs = GBApplication.getPrefs();
        activityUserName = prefs.getString(PREF_USER_NAME, defaultUserName);
        activityUserGender = prefs.getInt(PREF_USER_GENDER, defaultUserGender);
        activityUserHeightCm = prefs.getInt(PREF_USER_HEIGHT_CM, defaultUserHeightCm);
        activityUserWeightKg = prefs.getInt(PREF_USER_WEIGHT_KG, defaultUserWeightKg);
        activityUserYearOfBirth = prefs.getInt(PREF_USER_YEAR_OF_BIRTH, defaultUserYearOfBirth);
        activityUserSleepDuration = prefs.getInt(PREF_USER_SLEEP_DURATION, defaultUserSleepDuration);
        activityUserStepsGoal = prefs.getInt(PREF_USER_STEPS_GOAL, defaultUserStepsGoal);
    }

    public Date getUserBirthday() {
        Calendar cal = DateTimeUtils.getCalendarUTC();
        cal.set(GregorianCalendar.YEAR, getYearOfBirth());
        return cal.getTime();
    }
}
