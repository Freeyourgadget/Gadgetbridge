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

public class ButtonModes {
    public enum Mode {
        OFF,
        AMBIENT_SOUND_CONTROL,
        PLAYBACK_CONTROL,
        VOLUME_CONTROL,
        ;
    }

    final Mode left;
    final Mode right;

    public ButtonModes(final Mode left, final Mode right) {
        this.left = left;
        this.right = right;
    }

    public Mode getModeLeft() {
        return left;
    }

    public Mode getModeRight() {
        return right;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_LEFT, left.name().toLowerCase(Locale.getDefault()));
            put(DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_RIGHT, right.name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static ButtonModes fromPreferences(final SharedPreferences prefs) {
        return new ButtonModes(
                Mode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_LEFT, "off").toUpperCase()),
                Mode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_BUTTON_MODE_RIGHT, "off").toUpperCase())
        );
    }
}
