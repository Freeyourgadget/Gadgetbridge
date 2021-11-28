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
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_PRESET_CUSTOM_1;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_PRESET_CUSTOM_2;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_PRESET_MANUAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;

public class SonySettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler) {
        // Only enable the focus on voice check and voice level slider if the ambient sound control mode is ambient sound

        final ListPreference ambientSoundControl = handler.findPreference(PREF_SONY_AMBIENT_SOUND_CONTROL);
        if (ambientSoundControl != null) {
            final Preference focusOnVoice = handler.findPreference(PREF_SONY_FOCUS_VOICE);
            final Preference ambientSoundLevel = handler.findPreference(PREF_SONY_AMBIENT_SOUND_LEVEL);

            final Preference.OnPreferenceChangeListener ambientSoundControlPrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    boolean isAmbientSoundEnabled = AmbientSoundControl.AMBIENT_SOUND.name().toLowerCase(Locale.ROOT).equals(newVal);
                    focusOnVoice.setEnabled(isAmbientSoundEnabled);
                    ambientSoundLevel.setEnabled(isAmbientSoundEnabled);

                    return true;
                }
            };

            ambientSoundControlPrefListener.onPreferenceChange(ambientSoundControl, ambientSoundControl.getValue());
            handler.addPreferenceHandlerFor(PREF_SONY_AMBIENT_SOUND_CONTROL, ambientSoundControlPrefListener);
        }

        // Make the sound position and surround mode settings mutually exclusive

        final ListPreference soundPositionPref = handler.findPreference(PREF_SONY_SOUND_POSITION);
        final ListPreference surroundModePref = handler.findPreference(PREF_SONY_SURROUND_MODE);

        if (soundPositionPref != null && surroundModePref != null) {
            final Preference.OnPreferenceChangeListener soundPositionPrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    SoundPosition soundPosition = SoundPosition.valueOf(newVal.toString().toUpperCase(Locale.ROOT));
                    surroundModePref.setEnabled(SoundPosition.OFF.equals(soundPosition));

                    return true;
                }
            };

            final Preference.OnPreferenceChangeListener surroundModePrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    SurroundMode surroundMode = SurroundMode.valueOf(newVal.toString().toUpperCase(Locale.ROOT));
                    soundPositionPref.setEnabled(SurroundMode.OFF.equals(surroundMode));

                    return true;
                }
            };

            soundPositionPrefListener.onPreferenceChange(soundPositionPref, soundPositionPref.getValue());
            surroundModePrefListener.onPreferenceChange(surroundModePref, surroundModePref.getValue());
            handler.addPreferenceHandlerFor(PREF_SONY_SOUND_POSITION, soundPositionPrefListener);
            handler.addPreferenceHandlerFor(PREF_SONY_SURROUND_MODE, surroundModePrefListener);
        }

        // Only enable the equalizer preset if the corresponding mode is selected

        final ListPreference equalizerModePref = handler.findPreference(PREF_SONY_EQUALIZER_MODE);

        if (equalizerModePref != null) {
            handler.addPreferenceHandlerFor(PREF_SONY_EQUALIZER_MODE);

            final Preference presetManual = handler.findPreference(PREF_SONY_EQUALIZER_PRESET_MANUAL);
            final Preference presetCustom1 = handler.findPreference(PREF_SONY_EQUALIZER_PRESET_CUSTOM_1);
            final Preference presetCustom2 = handler.findPreference(PREF_SONY_EQUALIZER_PRESET_CUSTOM_2);

            final Preference.OnPreferenceChangeListener equalizerModePrefListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newVal) {
                    final EqualizerPreset equalizerPreset = EqualizerPreset.valueOf(newVal.toString().toUpperCase(Locale.ROOT));

                    presetManual.setEnabled(EqualizerPreset.MANUAL.equals(equalizerPreset));
                    presetCustom1.setEnabled(EqualizerPreset.CUSTOM_1.equals(equalizerPreset));
                    presetCustom2.setEnabled(EqualizerPreset.CUSTOM_2.equals(equalizerPreset));

                    return true;
                }
            };

            equalizerModePrefListener.onPreferenceChange(equalizerModePref, equalizerModePref.getValue());
            handler.addPreferenceHandlerFor(PREF_SONY_EQUALIZER_MODE, equalizerModePrefListener);
        }
    }
}
