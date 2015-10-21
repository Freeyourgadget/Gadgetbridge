package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MiBandConst {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandConst.class);

    public static final String PREF_USER_ALIAS = "mi_user_alias";
    public static final String PREF_USER_YEAR_OF_BIRTH = "mi_user_year_of_birth";
    public static final String PREF_USER_GENDER = "mi_user_gender";
    public static final String PREF_USER_HEIGHT_CM = "mi_user_height_cm";
    public static final String PREF_USER_WEIGHT_KG = "mi_user_weight_kg";
    public static final String PREF_MIBAND_WEARSIDE = "mi_wearside";
    public static final String PREF_MIBAND_ADDRESS = "development_miaddr";  // FIXME: should be prefixed mi_
    public static final String PREF_MIBAND_ALARMS = "mi_alarms";
    public static final String PREF_MIBAND_FITNESS_GOAL = "mi_fitness_goal";
    public static final String PREF_MIBAND_DONT_ACK_TRANSFER = "mi_dont_ack_transfer";


    public static final String ORIGIN_SMS = "sms";
    public static final String ORIGIN_INCOMING_CALL = "incoming_call";
    public static final String ORIGIN_K9MAIL = "k9mail";
    public static final String ORIGIN_PEBBLEMSG = "pebblemsg";
    public static final String ORIGIN_GENERIC = "generic";

    public static int getNotificationPrefIntValue(String pref, String origin, SharedPreferences prefs, int defaultValue) {
        String key = getNotificationPrefKey(pref, origin);
        String value = null;
        try {
            value = prefs.getString(key, String.valueOf(defaultValue));
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            LOG.error("Error converting preference value to int: " + key + ": " + value);
            return defaultValue;
        }
    }

    public static String getNotificationPrefStringValue(String pref, String origin, SharedPreferences prefs, String defaultValue) {
        String key = getNotificationPrefKey(pref, origin);
        return prefs.getString(key, defaultValue);
    }

    public static String getNotificationPrefKey(String pref, String origin) {
        return pref + '_' + origin;
    }

    public static final String VIBRATION_PROFILE = "mi_vibration_profile";
    public static final String VIBRATION_COUNT = "mi_vibration_count";
    public static final String VIBRATION_DURATION = "mi_vibration_duration";
    public static final String VIBRATION_PAUSE = "mi_vibration_pause";
    public static final String FLASH_COUNT = "mi_flash_count";
    public static final String FLASH_DURATION = "mi_flash_duration";
    public static final String FLASH_PAUSE = "mi_flash_pause";
    public static final String FLASH_COLOUR = "mi_flash_colour";
    public static final String FLASH_ORIGINAL_COLOUR = "mi_flash_original_colour";

    public static final String DEFAULT_VALUE_VIBRATION_PROFILE = "short";
    public static final int DEFAULT_VALUE_VIBRATION_COUNT = 3;
    public static final int DEFAULT_VALUE_VIBRATION_DURATION = 500; // ms
    public static final int DEFAULT_VALUE_VIBRATION_PAUSE = 500; // ms
    public static final int DEFAULT_VALUE_FLASH_COUNT = 10; // ms
    public static final int DEFAULT_VALUE_FLASH_DURATION = 500; // ms
    public static final int DEFAULT_VALUE_FLASH_PAUSE = 500; // ms
    public static final int DEFAULT_VALUE_FLASH_COLOUR = 1; // TODO: colour!
    public static final int DEFAULT_VALUE_FLASH_ORIGINAL_COLOUR = 1; // TODO: colour!
}
