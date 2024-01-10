/*  Copyright (C) 2022-2024 José Rebelo

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

public enum AmbientSoundControlButtonMode {
    NC_AS_OFF((byte) 0x01),
    NC_AS((byte) 0x02),
    NC_OFF((byte) 0x03),
    AS_OFF((byte) 0x04),
    ;

    private final byte code;

    AmbientSoundControlButtonMode(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return this.code;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static AmbientSoundControlButtonMode fromCode(final byte code) {
        for (AmbientSoundControlButtonMode value : AmbientSoundControlButtonMode.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }

        return null;
    }

    public static AmbientSoundControlButtonMode fromPreferences(final SharedPreferences prefs) {
        return AmbientSoundControlButtonMode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE, "nc_as_off").toUpperCase());
    }
}
