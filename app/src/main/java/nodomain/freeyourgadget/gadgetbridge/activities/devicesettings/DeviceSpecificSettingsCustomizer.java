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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.os.Parcelable;

import androidx.preference.Preference;

import java.util.Set;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A device-specific preference handler, that allows for concrete implementations to customize the preferences in
 * the {@link DeviceSpecificSettingsFragment}.
 */
public interface DeviceSpecificSettingsCustomizer extends Parcelable {
    /**
     * Called when a {@link Preference} changes, not caused by user input (so the preference change listener is not called).
     *
     * @param preference the {@link Preference} preference that changed
     * @param handler    the {@link DeviceSpecificSettingsHandler}
     */
    void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler);

    /**
     * Customize the settings on the {@link DeviceSpecificSettingsFragment}.
     * @param handler the {@link DeviceSpecificSettingsHandler}
     * @param prefs the {@link android.content.SharedPreferences}
     */
    void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs);

    /**
     * Keys of preferences which should print its values as a summary below the preference name.
     */
    Set<String> getPreferenceKeysWithSummary();
}
