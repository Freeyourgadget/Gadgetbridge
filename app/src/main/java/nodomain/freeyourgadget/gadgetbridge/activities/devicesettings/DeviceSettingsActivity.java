/*  Copyright (C) 2019-2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class DeviceSettingsActivity extends AbstractGBActivity implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceSettingsActivity.class);
    public static final String MENU_ENTRY_POINT = "MENU_ENTRY_POINT";

    GBDevice device;
    MENU_ENTRY_POINTS menu_entry;

    public enum MENU_ENTRY_POINTS {
        DEVICE_SETTINGS,
        AUTH_SETTINGS,
        APPLICATION_SETTINGS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        menu_entry = (MENU_ENTRY_POINTS) getIntent().getSerializableExtra(MENU_ENTRY_POINT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);
        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(DeviceSpecificSettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = DeviceSpecificSettingsFragment.newInstance(device, menu_entry);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, DeviceSpecificSettingsFragment.FRAGMENT_TAG)
                    .commit();

        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen preferenceScreen) {
        final PreferenceFragmentCompat fragment = DeviceSpecificSettingsFragment.newInstance(device, menu_entry);
        Bundle args = fragment.getArguments();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment, preferenceScreen.getKey())
                .addToBackStack(preferenceScreen.getKey())
                .commit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Simulate a back press, so that we don't actually exit the activity when
                // in a nested PreferenceScreen
                this.onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
