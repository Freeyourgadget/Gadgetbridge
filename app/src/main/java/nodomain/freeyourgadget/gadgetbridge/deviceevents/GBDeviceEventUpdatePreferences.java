/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GBDeviceEventUpdatePreferences extends GBDeviceEvent {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceEventUpdatePreferences.class);

    public final Map<String, Object> preferences;

    public GBDeviceEventUpdatePreferences() {
        this.preferences = new HashMap<>();
    }

    public GBDeviceEventUpdatePreferences(final Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public GBDeviceEventUpdatePreferences(final String key, final Object value) {
        this.preferences = new HashMap<>();
        this.preferences.put(key, value);
    }

    public GBDeviceEventUpdatePreferences withPreference(final String key, final Object value) {
        this.preferences.put(key, value);

        return this;
    }

    public GBDeviceEventUpdatePreferences withPreferences(final Map<String, Object> preferences) {
        this.preferences.putAll(preferences);

        return this;
    }

    /**
     * Update a {@link SharedPreferences} instance with the preferences in the event.
     *
     * @param prefs the SharedPreferences object to update.
     */
    public void update(final SharedPreferences prefs) {
        final SharedPreferences.Editor editor = prefs.edit();

        for (String key : preferences.keySet()) {
            final Object value = preferences.get(key);

            LOG.debug("Updating {} = {}", key, value);

            if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Set) {
                editor.putStringSet(key, (Set) value);
            } else {
                LOG.warn("Unknown preference value type {} for {}", value.getClass(), key);
            }
        }

        editor.apply();
    }
}
