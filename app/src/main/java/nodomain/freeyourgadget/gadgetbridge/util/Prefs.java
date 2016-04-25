package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Wraps SharedPreferences to avoid ClassCastExceptions and others.
 */
public class Prefs {
    private static final Logger LOG = LoggerFactory.getLogger(Prefs.class);

    private final SharedPreferences preferences;

    public Prefs(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public String getString(String key, String defaultValue) {
        String value = preferences.getString(key, defaultValue);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        Set<String> value = preferences.getStringSet(key, defaultValue);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return preferences.getInt(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Integer.parseInt(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return preferences.getLong(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Long.parseLong(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    public float getFloat(String key, float defaultValue) {
        try {
            return preferences.getFloat(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Float.parseFloat(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (Exception ex) {
            try {
                String value = preferences.getString(key, String.valueOf(defaultValue));
                if ("".equals(value)) {
                    return defaultValue;
                }
                return Boolean.parseBoolean(value);
            } catch (Exception ex2) {
                logReadError(key, ex);
                return defaultValue;
            }
        }
    }

    private void logReadError(String key, Exception ex) {
        LOG.error("Error reading preference value: " + key + "; returning default value", ex); // log the first exception
    }

    /**
     * Access to the underlying SharedPreferences, typically only used for editing values.
     * @return the underlying SharedPreferences object.
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }
}
