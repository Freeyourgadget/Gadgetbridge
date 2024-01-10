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

public enum EqualizerPreset {
    OFF((byte) 0x00),
    BRIGHT((byte) 0x10),
    EXCITED((byte) 0x11),
    MELLOW((byte) 0x12),
    RELAXED((byte) 0x13),
    VOCAL((byte) 0x14),
    TREBLE_BOOST((byte) 0x15),
    BASS_BOOST((byte) 0x16),
    SPEECH((byte) 0x17),
    MANUAL((byte) 0xa0),
    CUSTOM_1((byte) 0xa1),
    CUSTOM_2((byte) 0xa2);

    private final byte code;

    EqualizerPreset(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return this.code;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static EqualizerPreset fromCode(final byte code) {
        for (EqualizerPreset value : EqualizerPreset.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }

        return null;
    }

    public static EqualizerPreset fromPreferences(final SharedPreferences prefs) {
        return EqualizerPreset.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE, "off").toUpperCase());
    }
}
