/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;

public class AmbientSoundControl {
    public enum Mode {
        OFF,
        NOISE_CANCELLING,
        WIND_NOISE_REDUCTION,
        AMBIENT_SOUND
    }

    private final Mode mode;
    private final boolean focusOnVoice;
    private final int ambientSound;

    public AmbientSoundControl(final Mode mode, final boolean focusOnVoice, final int ambientSound) {
        if (ambientSound < 0 || ambientSound > 20) {
            throw new IllegalArgumentException(String.format("Level must be between 0 and 20 (was %d)", ambientSound));
        }

        this.mode = mode;
        this.focusOnVoice = focusOnVoice;
        this.ambientSound = ambientSound;
    }

    public String toString() {
        return String.format(Locale.getDefault(), "AmbientSoundControl{mode=%s, focusOnVoice=%s, ambientSound=%d}", mode, focusOnVoice, ambientSound);
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isFocusOnVoice() {
        return focusOnVoice;
    }

    public int getAmbientSound() {
        return ambientSound;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL, mode.name().toLowerCase(Locale.getDefault()));

            if (AmbientSoundControl.Mode.AMBIENT_SOUND.equals(mode)) {
                // Only use the ambient sound levels and focus on voice if we're on ambient sound mode,
                // to prevent overriding the user settings
                put(DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE, focusOnVoice);
                // Level is offset by 1 because we can't configure the SeekBarPreference min level on the current api level
                put(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL, ambientSound - 1);
            }
        }};
    }

    public static AmbientSoundControl fromPreferences(final SharedPreferences prefs) {
        final String soundControl = prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL, "noise_cancelling");
        final boolean focusVoice = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE, false);
        // Level is offset by 1 because we can't configure the SeekBarPreference min level on the current api level
        final int ambientSound = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL, 0) + 1;

        return new AmbientSoundControl(AmbientSoundControl.Mode.valueOf(soundControl.toUpperCase()), focusVoice, ambientSound);
    }
}
