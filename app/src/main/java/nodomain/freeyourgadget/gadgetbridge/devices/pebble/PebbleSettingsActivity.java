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
package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;

public class PebbleSettingsActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return PebbleSettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new PebbleSettingsFragment();
    }

    public static class PebbleSettingsFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "PEBBLE_SETTINGS_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.pebble_preferences, rootKey);

            setInputTypeFor("pebble_mtu_limit", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("pebble_emu_port", InputType.TYPE_CLASS_NUMBER);

            final Preference pebbleEmuAddrPref = findPreference("pebble_emu_addr");
            if (pebbleEmuAddrPref != null) {
                pebbleEmuAddrPref.setOnPreferenceChangeListener((preference, newVal) -> {
                    Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                    LocalBroadcastManager.getInstance(requireContext().getApplicationContext()).sendBroadcast(refreshIntent);
                    return true;
                });
            }

            final Preference pebbleEmuPort = findPreference("pebble_emu_port");
            if (pebbleEmuPort != null) {
                pebbleEmuPort.setOnPreferenceChangeListener((preference, newVal) -> {
                    Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                    LocalBroadcastManager.getInstance(requireContext().getApplicationContext()).sendBroadcast(refreshIntent);
                    return true;
                });
            }
        }
    }
}
