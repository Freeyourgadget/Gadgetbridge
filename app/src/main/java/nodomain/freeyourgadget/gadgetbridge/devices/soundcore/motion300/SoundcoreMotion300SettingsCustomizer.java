/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.devices.soundcore.motion300;

import android.os.Parcel;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.motion300.SoundcoreMotion300Protocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

public class SoundcoreMotion300SettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<SoundcoreMotion300SettingsCustomizer> CREATOR = new Creator<SoundcoreMotion300SettingsCustomizer>() {
        @Override
        public SoundcoreMotion300SettingsCustomizer createFromParcel(final Parcel in) {
            return new SoundcoreMotion300SettingsCustomizer();
        }

        @Override
        public SoundcoreMotion300SettingsCustomizer[] newArray(final int size) {
            return new SoundcoreMotion300SettingsCustomizer[size];
        }
    };

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        if (!preference.getKey().equals(PREF_SOUNDCORE_EQUALIZER_PRESET))
            return;

        CharSequence preset = ((ListPreference)preference).getEntry();

        if (preset == null)
            return;

        Preference pref = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_CUSTOM);
        boolean customEnabled = preset.toString().equals(handler.getContext().getString(R.string.custom));

        if (pref != null)
            pref.setEnabled(customEnabled);
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        ListPreference equalizerDirection = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_DIRECTION);

        if (equalizerDirection != null) {
            boolean enabled = prefs.getBoolean(PREF_SOUNDCORE_ADAPTIVE_DIRECTION, true);

            equalizerDirection.setVisible(enabled);
        }

        Preference equalizerReset = handler.findPreference(PREF_SOUNDCORE_EQUALIZER_RESET);

        if (equalizerReset != null)
            equalizerReset.setOnPreferenceClickListener(pref -> resetEqualizer(handler));
    }

    private boolean resetEqualizer(final DeviceSpecificSettingsHandler handler) {
        // Reset all bands to default settings
        for (int i = 0; i < SoundcoreMotion300Protocol.EQUALIZER_PREFS_FREQ.length; i++) {
            ListPreference prefFreq = handler.findPreference(SoundcoreMotion300Protocol.EQUALIZER_PREFS_FREQ[i]);
            SeekBarPreference prefValue = handler.findPreference(SoundcoreMotion300Protocol.EQUALIZER_PREFS_VALUE[i]);

            // Neutral configuration
            prefValue.setValue(60);

            // Default configuration: 80 Hz, 150 Hz, 300 Hz, 600 Hz, 1.2 kHz, 2.5 kHz, 5 kHz, 9 kHz, 13 kHz
            if (i < 7)
                prefFreq.setValue("7");
            else if (i == 7)
                prefFreq.setValue("6");
            else if (i == 8)
                prefFreq.setValue("1");
        }

        // Send updated equalizer configuration
        handler.notifyPreferenceChanged(PREF_SOUNDCORE_EQUALIZER_BAND9_FREQ);

        return true;
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
    public void writeToParcel(final Parcel dest, final int flags) {}
}
