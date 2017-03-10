/*  Copyright (C) 2016-2017 Carsten Pfeiffer, JohnnySun

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.Set;

/**
 * Wraps SharedPreferences to avoid ClassCastExceptions and others.
 */
public class Prefs {
    private static final String TAG = "Prefs";
    // DO NOT use slf4j logger here, this would break its configuration via GBApplication
//    private static final Logger LOG = LoggerFactory.getLogger(Prefs.class);

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

    /**
     * Returns the preference saved under the given key as an integer value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as an integer value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
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

    /**
     * Returns the preference saved under the given key as a long value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a long value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
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

    /**
     * Returns the preference saved under the given key as a float value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a float value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
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

    /**
     * Returns the preference saved under the given key as a boolean value.
     * Note that it is irrelevant whether the preference value was actually
     * saved as a boolean value or a string value.
     * @param key the preference key
     * @param defaultValue the default value to return if the preference value is unset
     * @return the saved preference value or the given defaultValue
     */
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
        Log.e(TAG, "Error reading preference value: " + key + "; returning default value", ex); // log the first exception
    }

    /**
     * Access to the underlying SharedPreferences, typically only used for editing values.
     * @return the underlying SharedPreferences object.
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }
}
