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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.hidePrefIfNoneVisible;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils.populateOrHideListPreference;

import android.os.Parcel;

import androidx.preference.Preference;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference activityMonitoringPref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ACTIVITY_MONITORING);
        if (activityMonitoringPref != null) {
            activityMonitoringPref.setVisible(false);
        }

        final Preference hrAlertActivePref = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_ACTIVE_HIGH_THRESHOLD);
        if (hrAlertActivePref != null) {
            hrAlertActivePref.setVisible(false);
        }

        populateOrHideListPreference(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, handler, prefs);

        hidePrefIfNoneVisible(handler, DeviceSettingsPreferenceConst.PREF_HEADER_DISPLAY, Arrays.asList(
                HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE,
                DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD
        ));
        hidePrefIfNoneVisible(handler, "pref_header_other", Arrays.asList(
                "pref_contacts",
                "camera_remote",
                "screen_events_forwarding",
                "phone_silent_mode"
        ));
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<XiaomiSettingsCustomizer> CREATOR = new Creator<XiaomiSettingsCustomizer>() {
        @Override
        public XiaomiSettingsCustomizer createFromParcel(final Parcel in) {
            return new XiaomiSettingsCustomizer();
        }

        @Override
        public XiaomiSettingsCustomizer[] newArray(final int size) {
            return new XiaomiSettingsCustomizer[size];
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
