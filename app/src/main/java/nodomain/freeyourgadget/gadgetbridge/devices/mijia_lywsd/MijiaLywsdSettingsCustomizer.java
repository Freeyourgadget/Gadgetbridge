/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd;

import android.os.Parcel;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd.MijiaLywsdSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

public class MijiaLywsdSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<MijiaLywsdSettingsCustomizer> CREATOR = new Creator<MijiaLywsdSettingsCustomizer>() {
        @Override
        public MijiaLywsdSettingsCustomizer createFromParcel(final Parcel in) {
            return new MijiaLywsdSettingsCustomizer();
        }

        @Override
        public MijiaLywsdSettingsCustomizer[] newArray(final int size) {
            return new MijiaLywsdSettingsCustomizer[size];
        }
    };

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        String key = preference.getKey();

        if (!key.equals(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER) && !key.equals(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER) &&
            !key.equals(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER) && !key.equals(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER))
            return;

        SeekBarPreference temperatureLower = handler.findPreference(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER);
        SeekBarPreference temperatureUpper = handler.findPreference(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER);
        SeekBarPreference humidityLower = handler.findPreference(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER);
        SeekBarPreference humidityUpper = handler.findPreference(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER);

        if (temperatureLower == null || temperatureUpper == null || humidityLower == null || humidityUpper == null)
            return;

        // Clamp minimum value of upper limit to lower limit
        if (temperatureLower.getValue() > temperatureUpper.getValue())
            temperatureUpper.setValue(temperatureLower.getValue());

        if (humidityLower.getValue() > humidityUpper.getValue())
            humidityUpper.setValue(humidityLower.getValue());
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        Preference comfortLevel = handler.findPreference(PREF_MIJIA_LYWSD_COMFORT_LEVEL);

        if (comfortLevel != null) {
            int length = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_CHARACTERISTIC_LENGTH, 0);

            // Hide comfort level for unknown characteristic length
            comfortLevel.setVisible(length == MijiaLywsdSupport.COMFORT_LEVEL_LENGTH_LYWSD03 ||
                                    length == MijiaLywsdSupport.COMFORT_LEVEL_LENGTH_XMWSDJ04);
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {}
}
