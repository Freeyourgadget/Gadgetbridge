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

public class SpeakToChatConfig {
    public enum Sensitivity {
        AUTO(0x00),
        HIGH(0x01),
        LOW(0x02),
        ;

        private final byte code;

        Sensitivity(final int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return this.code;
        }

        public static Sensitivity fromCode(final byte b) {
            for (Sensitivity value : Sensitivity.values()) {
                if (value.getCode() == b) {
                    return value;
                }
            }

            return null;
        }
    }

    public enum Timeout {
        SHORT(0x00),
        STANDARD(0x01),
        LONG(0x02),
        OFF(0x03),
        ;

        private final byte code;

        Timeout(final int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return this.code;
        }

        public static Timeout fromCode(final byte b) {
            for (Timeout value : Timeout.values()) {
                if (value.getCode() == b) {
                    return value;
                }
            }

            return null;
        }
    }

    private final boolean focusOnVoice;
    private final Sensitivity sensitivity;
    private final Timeout timeout;

    public SpeakToChatConfig(final boolean focusOnVoice, final Sensitivity sensitivity, final Timeout timeout) {
        this.focusOnVoice = focusOnVoice;
        this.sensitivity = sensitivity;
        this.timeout = timeout;
    }

    public boolean isFocusOnVoice() {
        return focusOnVoice;
    }

    public Sensitivity getSensitivity() {
        return sensitivity;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public String toString() {
        return String.format(Locale.getDefault(), "SpeakToChatConfig{focusOnVoice=%s, sensitivity=%s, timeout=%s}", focusOnVoice, sensitivity, timeout);
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE, focusOnVoice);
            put(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY, sensitivity.name().toLowerCase(Locale.getDefault()));
            put(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_TIMEOUT, timeout.name().toLowerCase(Locale.getDefault()));
        }};
    }

    public static SpeakToChatConfig fromPreferences(final SharedPreferences prefs) {
        final boolean focusOnVoice = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_FOCUS_ON_VOICE, false);
        final Sensitivity sensitivity = Sensitivity.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_SENSITIVITY, "auto").toUpperCase());
        final Timeout timeout = Timeout.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SPEAK_TO_CHAT_TIMEOUT, "standard").toUpperCase());

        return new SpeakToChatConfig(focusOnVoice, sensitivity, timeout);
    }
}
