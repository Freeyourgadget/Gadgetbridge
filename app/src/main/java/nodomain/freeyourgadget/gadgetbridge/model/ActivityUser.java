/*  Copyright (C) 2016-2020 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Sebastian Kranz

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
    private int activityUserSleepDurationGoal;
    private int activityUserStepsGoal;
    private int activityUserCaloriesBurntGoal;
    private int activityUserDistanceGoalMeters;
    private int activityUserActiveTimeGoalMinutes;
    private int activityUserStepLengthCm;

    private static final String defaultUserName = "gadgetbridge-user";
    public static final int defaultUserGender = GENDER_FEMALE;
    public static final int defaultUserYearOfBirth = 0;
    public static final int defaultUserAge = 0;
    public static final int defaultUserHeightCm = 175;
    public static final int defaultUserWeightKg = 70;
    public static final int defaultUserSleepDurationGoal = 7;
    public static final int defaultUserStepsGoal = 8000;
    public static final int defaultUserCaloriesBurntGoal = 2000;
    public static final int defaultUserDistanceGoalMeters = 5000;
    public static final int defaultUserActiveTimeGoalMinutes = 60;
    public static final int defaultUserStepLengthCm = 0;

    public static final String PREF_USER_NAME = "mi_user_alias";
    public static final String PREF_USER_YEAR_OF_BIRTH = "activity_user_year_of_birth";
    public static final String PREF_USER_GENDER = "activity_user_gender";
    public static final String PREF_USER_HEIGHT_CM = "activity_user_height_cm";
    public static final String PREF_USER_WEIGHT_KG = "activity_user_weight_kg";
    public static final String PREF_USER_SLEEP_DURATION = "activity_user_sleep_duration";
    public static final String PREF_USER_STEPS_GOAL = "fitness_goal"; // FIXME: for compatibility
    public static final String PREF_USER_CALORIES_BURNT = "activity_user_calories_burnt";
    public static final String PREF_USER_DISTANCE_METERS = "activity_user_distance_meters";
    public static final String PREF_USER_ACTIVETIME_MINUTES = "activity_user_activetime_minutes";
    public static final String PREF_USER_STEP_LENGTH_CM = "activity_user_step_length_cm";

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

    /**
     * @return the user defined height or a default value when none is set or the stored
     * value is 0.
     */

    public int getHeightCm() {
        if (activityUserHeightCm < 1) {
            activityUserHeightCm = defaultUserHeightCm;
        }
        return activityUserHeightCm;
    }

    /**
     * @return the user defined step length or the calculated default value when none is set or the stored
     * value is 0.
     */
    public int getStepLengthCm() {
        if (activityUserStepLengthCm < 1) {
            activityUserStepLengthCm = (int) (getHeightCm() * 0.43);
        }
        return activityUserStepLengthCm;
    }

    /**
     * @return the user defined sleep duration or the default value when none is set or the stored
     * value is out of any logical bounds.
     */
    public int getSleepDurationGoal() {
        if (activityUserSleepDurationGoal < 1 || activityUserSleepDurationGoal > 24) {
            activityUserSleepDurationGoal = defaultUserSleepDurationGoal;
        }
        return activityUserSleepDurationGoal;
    }

    public int getStepsGoal() {
        if (activityUserStepsGoal < 1) {
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
        activityUserSleepDurationGoal = prefs.getInt(PREF_USER_SLEEP_DURATION, defaultUserSleepDurationGoal);
        activityUserStepsGoal = prefs.getInt(PREF_USER_STEPS_GOAL, defaultUserStepsGoal);
        activityUserCaloriesBurntGoal = prefs.getInt(PREF_USER_CALORIES_BURNT, defaultUserCaloriesBurntGoal);
        activityUserDistanceGoalMeters = prefs.getInt(PREF_USER_DISTANCE_METERS, defaultUserDistanceGoalMeters);
        activityUserActiveTimeGoalMinutes = prefs.getInt(PREF_USER_ACTIVETIME_MINUTES, defaultUserActiveTimeGoalMinutes);
        activityUserStepLengthCm = prefs.getInt(PREF_USER_STEP_LENGTH_CM, defaultUserStepLengthCm);
    }

    public Date getUserBirthday() {
        Calendar cal = DateTimeUtils.getCalendarUTC();
        cal.set(GregorianCalendar.YEAR, getYearOfBirth());
        return cal.getTime();
    }

    public int getCaloriesBurntGoal()
    {
        if (activityUserCaloriesBurntGoal < 1) {
            activityUserCaloriesBurntGoal = defaultUserCaloriesBurntGoal;
        }
        return activityUserCaloriesBurntGoal;
    }

    public int getDistanceGoalMeters()
    {
        if (activityUserDistanceGoalMeters < 1) {
            activityUserDistanceGoalMeters = defaultUserDistanceGoalMeters;
        }
        return activityUserDistanceGoalMeters;
    }

    public int getActiveTimeGoalMinutes()
    {
        if (activityUserActiveTimeGoalMinutes < 1) {
            activityUserActiveTimeGoalMinutes = defaultUserActiveTimeGoalMinutes;
        }
        return activityUserActiveTimeGoalMinutes;
    }
}
