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

import androidx.annotation.NonNull;
import androidx.preference.Preference;

/**
 * A device-specific preference handler, that allows for {@link nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator}s to register
 * their own preferences dynamically.
 */
public interface DeviceSpecificSettingsHandler {
    /**
     * Finds a preference with the given key. Returns null if the preference is not found.
     *
     * @param preferenceKey the preference key.
     * @return the preference, if found.
     */
    <T extends Preference> T findPreference(@NonNull CharSequence preferenceKey);

    /**
     * Adds a preference handler for a preference key. This handler sends the preference to the device on change.
     *
     * @param preferenceKey the preference key.
     */
    void addPreferenceHandlerFor(final String preferenceKey);

    /**
     * Notify the device that a preference changed.
     *
     * @param preferenceKey the preference key.
     */
    void notifyPreferenceChanged(final String preferenceKey);

    /**
     * Adds a preference handler for a preference key. On change, this handler calls the provided extra listener, and then sends the preference to the device.
     *
     * @param preferenceKey the preference key.
     * @param extraListener the extra listener.
     */
    void addPreferenceHandlerFor(final String preferenceKey, Preference.OnPreferenceChangeListener extraListener);

    /**
     * Sets the input type flags for an EditText preference.
     *
     * @param preferenceKey the preference key.
     * @param editTypeFlags the edit type {@link android.text.InputType} flags.
     */
    void setInputTypeFor(final String preferenceKey, final int editTypeFlags);
}
