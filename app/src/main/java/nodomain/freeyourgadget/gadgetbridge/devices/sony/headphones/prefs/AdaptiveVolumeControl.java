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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;

public class AdaptiveVolumeControl {
    private final boolean enabled;

    public AdaptiveVolumeControl(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_ADAPTIVE_VOLUME_CONTROL, enabled);
        }};
    }

    public static AdaptiveVolumeControl fromPreferences(final SharedPreferences prefs) {
        return new AdaptiveVolumeControl(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_ADAPTIVE_VOLUME_CONTROL, true));
    }
}
