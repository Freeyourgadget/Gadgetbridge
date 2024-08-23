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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CmfWatchProSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final String[] prefsToHide = new String[]{
                "pref_key_header_heartrate_sleep",
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_USE_FOR_SLEEP_DETECTION,
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_SLEEP_BREATHING_QUALITY_MONITORING,
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_ACTIVITY_MONITORING,
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_RELAXATION_REMINDER,
                DeviceSettingsPreferenceConst.PREF_INACTIVITY_START,
                DeviceSettingsPreferenceConst.PREF_INACTIVITY_END,
        };

        for (final String prefKey : prefsToHide) {
            final Preference pref = handler.findPreference(prefKey);
            if (pref != null) {
                pref.setVisible(false);
            }
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<CmfWatchProSettingsCustomizer> CREATOR = new Creator<CmfWatchProSettingsCustomizer>() {
        @Override
        public CmfWatchProSettingsCustomizer createFromParcel(final Parcel in) {
            return new CmfWatchProSettingsCustomizer();
        }

        @Override
        public CmfWatchProSettingsCustomizer[] newArray(final int size) {
            return new CmfWatchProSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
    }
}
