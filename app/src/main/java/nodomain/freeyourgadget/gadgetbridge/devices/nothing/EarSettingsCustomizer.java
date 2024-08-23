/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import android.os.Parcel;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<EarSettingsCustomizer> CREATOR = new Creator<EarSettingsCustomizer>() {
        @Override
        public EarSettingsCustomizer createFromParcel(final Parcel in) {
            return new EarSettingsCustomizer();
        }

        @Override
        public EarSettingsCustomizer[] newArray(final int size) {
            return new EarSettingsCustomizer[size];
        }
    };

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final AbstractEarCoordinator earCoordinator = (AbstractEarCoordinator) handler.getDevice().getDeviceCoordinator();

        if (!earCoordinator.supportsLightAncAndTransparency()) {
            // If light anc and transparency is not supported, remove the values from the preference
            final Preference audioModePref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_NOTHING_EAR1_AUDIOMODE);

            if (audioModePref != null) {
                final CharSequence[] originalEntries = ((ListPreference) audioModePref).getEntries();
                final CharSequence[] originalEntryValues = ((ListPreference) audioModePref).getEntryValues();

                final List<CharSequence> entries = new ArrayList<>();
                final List<CharSequence> entryValues = new ArrayList<>();

                for (int i = 0; i < originalEntries.length; i++) {
                    if ("anc".equals(originalEntryValues[i].toString()) || "off".equals(originalEntryValues[i].toString())) {
                        entries.add(originalEntries[i]);
                        entryValues.add(originalEntryValues[i]);
                    }
                }

                ((ListPreference) audioModePref).setEntries(entries.toArray(new CharSequence[0]));
                ((ListPreference) audioModePref).setEntryValues(entryValues.toArray(new CharSequence[0]));
            }
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
    }
}
