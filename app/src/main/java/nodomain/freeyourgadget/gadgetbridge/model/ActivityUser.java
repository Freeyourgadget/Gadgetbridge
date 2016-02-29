package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

/**
 * Class holding the common user information needed by most activity trackers
 */
public class ActivityUser {

    private Integer activityUserGender;
    private Integer activityUserYearOfBirth;
    private Integer activityUserHeightCm;
    private Integer activityUserWeightKg;

    public static final int defaultUserGender = 0;
    public static final int defaultUserYearOfBirth = 0;
    public static final int defaultUserAge = 0;
    public static final int defaultUserHeightCm = 175;
    public static final int defaultUserWeightKg = 70;

    public static final String PREF_USER_YEAR_OF_BIRTH = "activity_user_year_of_birth";
    public static final String PREF_USER_GENDER = "activity_user_gender";
    public static final String PREF_USER_HEIGHT_CM = "activity_user_height_cm";
    public static final String PREF_USER_WEIGHT_KG = "activity_user_weight_kg";

    public int getActivityUserWeightKg() {
        if (activityUserWeightKg == null) {
            fetchPreferences();
        }
        return activityUserWeightKg;
    }

    public int getActivityUserGender() {
        if (activityUserGender == null) {
            fetchPreferences();
        }
        return activityUserGender;
    }

    public int getActivityUserYearOfBirth() {
        if (activityUserYearOfBirth == null) {
            fetchPreferences();
        }
        return activityUserYearOfBirth;
    }

    public int getActivityUserHeightCm() {
        if (activityUserHeightCm == null) {
            fetchPreferences();
        }
        return activityUserHeightCm;
    }

    public int getActivityUserAge() {
        int userYear = getActivityUserYearOfBirth();
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        activityUserGender = Integer.parseInt(prefs.getString(PREF_USER_GENDER, Integer.toString(defaultUserGender)));
        activityUserHeightCm = Integer.parseInt(prefs.getString(PREF_USER_HEIGHT_CM, Integer.toString(defaultUserHeightCm)));
        activityUserWeightKg = Integer.parseInt(prefs.getString(PREF_USER_WEIGHT_KG, Integer.toString(defaultUserWeightKg)));
        activityUserYearOfBirth = Integer.parseInt(prefs.getString(PREF_USER_YEAR_OF_BIRTH, Integer.toString(defaultUserYearOfBirth)));
    }
}
