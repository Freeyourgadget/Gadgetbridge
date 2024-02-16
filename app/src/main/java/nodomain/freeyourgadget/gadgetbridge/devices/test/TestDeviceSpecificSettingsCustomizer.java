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
package nodomain.freeyourgadget.gadgetbridge.devices.test;

import android.os.Parcel;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class TestDeviceSpecificSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        if (TestDeviceConst.PREF_TEST_FEATURES.equals(preference.getKey())) {
            handler.getDevice().sendDeviceUpdateIntent(handler.getContext());
        }
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs) {
        final Preference pref = handler.findPreference(TestDeviceConst.PREF_TEST_FEATURES);
        if (pref == null) {
            return;
        }

        // Populate the preference directly from the enum
        final CharSequence[] entries = new CharSequence[TestFeature.values().length];
        final CharSequence[] values = new CharSequence[TestFeature.values().length];
        for (int i = 0; i < TestFeature.values().length; i++) {
            entries[i] = TestFeature.values()[i].name();
            values[i] = TestFeature.values()[i].name();
        }
        if (pref instanceof MultiSelectListPreference) {
            ((MultiSelectListPreference) pref).setEntries(entries);
            ((MultiSelectListPreference) pref).setEntryValues(values);
        }
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<TestDeviceSpecificSettingsCustomizer> CREATOR = new Creator<TestDeviceSpecificSettingsCustomizer>() {
        @Override
        public TestDeviceSpecificSettingsCustomizer createFromParcel(final Parcel in) {
            return new TestDeviceSpecificSettingsCustomizer();
        }

        @Override
        public TestDeviceSpecificSettingsCustomizer[] newArray(final int size) {
            return new TestDeviceSpecificSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
    }
}
