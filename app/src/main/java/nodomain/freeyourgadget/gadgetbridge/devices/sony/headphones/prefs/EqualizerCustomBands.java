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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;

public class EqualizerCustomBands {
    private final List<Integer> bands;
    private final int bass;

    public EqualizerCustomBands(final List<Integer> bands, final int bass) {
        if (bands.size() != 5) {
            throw new IllegalArgumentException("Equalizer needs exactly 5 bands");
        }

        for (final Integer band : bands) {
            if (band < -10 || band > 10) {
                throw new IllegalArgumentException(String.format("Bands should be between -10 and 10, got %d", band));
            }
        }

        if (bass < -10 || bass > 10) {
            throw new IllegalArgumentException(String.format("Clear Bass value shoulud be between -10 and 10, got %d", bass));
        }

        this.bands = bands;
        this.bass = bass;
    }

    public List<Integer> getBands() {
        return bands;
    }

    public int getBass() {
        return bass;
    }

    public String toString() {
        return String.format(Locale.ROOT, "EqualizerCustomBands{clearBass=%d, bands=%s}", bass, bands);
    }

    public Map<String, Object> toPreferences() {
        return new HashMap<String, Object>() {{
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_400, bands.get(0) + 10);
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_1000, bands.get(1) + 10);
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_2500, bands.get(2) + 10);
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_6300, bands.get(3) + 10);
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_16000, bands.get(4) + 10);
            put(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BASS, bass + 10);
            ;
        }};
    }

    public static EqualizerCustomBands fromPreferences(final SharedPreferences prefs) {
        int band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_400, 10) - 10;
        int band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_1000, 10) - 10;
        int band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_2500, 10) - 10;
        int band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_6300, 10) - 10;
        int band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BAND_16000, 10) - 10;
        int bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_BASS, 10) - 10;

        return new EqualizerCustomBands(Arrays.asList(band1, band2, band3, band4, band5), bass);
    }
}
