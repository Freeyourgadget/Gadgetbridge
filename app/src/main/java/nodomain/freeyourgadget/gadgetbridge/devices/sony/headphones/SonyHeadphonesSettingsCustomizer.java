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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;

public class SonyHeadphonesSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler) {
        // Only enable the focus on voice check and voice level slider if the ambient sound control mode is ambient sound

        final ListPreference ambientSoundControl = handler.findPreference(PREF_SONY_AMBIENT_SOUND_CONTROL);
        if (ambientSoundControl != null) {
            final Preference focusOnVoice = handler.findPreference(PREF_SONY_FOCUS_VOICE);
            final Preference ambientSoundLevel = handler.findPreference(PREF_SONY_AMBIENT_SOUND_LEVEL);

            final Preference.OnPreferenceChangeListener ambientSoundControlPrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    boolean isAmbientSoundEnabled = AmbientSoundControl.Mode.AMBIENT_SOUND.name().toLowerCase(Locale.getDefault()).equals(newVal);
                    focusOnVoice.setEnabled(isAmbientSoundEnabled);
                    ambientSoundLevel.setEnabled(isAmbientSoundEnabled);

                    return true;
                }
            };

            ambientSoundControlPrefListener.onPreferenceChange(ambientSoundControl, ambientSoundControl.getValue());
            handler.addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL, ambientSoundControlPrefListener);
        }
    }
}
