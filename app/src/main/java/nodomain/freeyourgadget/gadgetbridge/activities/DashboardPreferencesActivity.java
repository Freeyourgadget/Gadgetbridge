/*  Copyright (C) 2024 Arjan Schrijver

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class DashboardPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return DashboardPreferencesFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new DashboardPreferencesFragment();
    }

    public static class DashboardPreferencesFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "DASHBOARD_PREFERENCES_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.dashboard_preferences, rootKey);

            setInputTypeFor("dashboard_widget_today_hr_interval", InputType.TYPE_CLASS_NUMBER);

            final MultiSelectListPreference dashboardDevices = findPreference("dashboard_devices_multiselect");
            if (dashboardDevices != null) {
                List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
                List<String> deviceMACs = new ArrayList<>();
                List<String> deviceNames = new ArrayList<>();
                for (GBDevice dev : devices) {
                    deviceMACs.add(dev.getAddress());
                    deviceNames.add(dev.getAliasOrName());
                }
                dashboardDevices.setEntryValues(deviceMACs.toArray(new String[0]));
                dashboardDevices.setEntries(deviceNames.toArray(new String[0]));
            }
            List<String> dashboardPrefs = Arrays.asList(
                    "dashboard_cards_enabled",
                    "pref_dashboard_widgets_order",
                    "dashboard_widget_today_24h",
                    "dashboard_widget_today_24h_upside_down",
                    "dashboard_widget_today_show_yesterday",
                    "dashboard_widget_today_time_indicator",
                    "dashboard_widget_today_2columns",
                    "dashboard_widget_today_legend",
                    "dashboard_widget_today_hr_interval",
                    "dashboard_widget_goals_2columns",
                    "dashboard_widget_goals_legend",
                    "dashboard_devices_all",
                    "dashboard_devices_multiselect"
            );
            Preference pref;
            for (String dashboardPref : dashboardPrefs) {
                pref = findPreference(dashboardPref);
                if (pref != null) {
                    pref.setOnPreferenceChangeListener((preference, autoExportEnabled) -> {
                        sendDashboardConfigChangedIntent();
                        return true;
                    });
                }
            }
        }

        /**
         * Signal dashboard that its config has changed
         */
        private void sendDashboardConfigChangedIntent() {
            Intent intent = new Intent();
            intent.setAction(DashboardFragment.ACTION_CONFIG_CHANGE);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        }
    }
}
