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

public enum SurroundMode {
    OFF((byte) 0x00),
    ARENA((byte) 0x02),
    CLUB((byte) 0x04),
    OUTDOOR_STAGE((byte) 0x01),
    CONCERT_HALL((byte) 0x03);

    private final byte code;

    SurroundMode(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return this.code;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE, name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static SurroundMode fromPreferences(final SharedPreferences prefs) {
        return SurroundMode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE, "off").toUpperCase());
    }
}
