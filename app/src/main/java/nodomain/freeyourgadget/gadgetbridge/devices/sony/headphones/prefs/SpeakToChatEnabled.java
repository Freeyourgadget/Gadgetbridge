/*  Copyright (C) 2023-2024 José Rebelo

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

public class SpeakToChatEnabled {
    private final boolean enabled;

    public SpeakToChatEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String toString() {
        return String.format(Locale.getDefault(), "SpeakToChatEnabled{enabled=%s}", enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT, enabled);
        }};
    }

    public static SpeakToChatEnabled fromPreferences(final SharedPreferences prefs) {
        final boolean enabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT, false);

        return new SpeakToChatEnabled(enabled);
    }
}
