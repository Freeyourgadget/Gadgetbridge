/*  Copyright (C) 2015-2023 Andreas Shimokawa, Carsten Pfeiffer, Lem Dulfo,
    vanous, José Rebelo

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

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractPreferenceFragment;

public class DiscoveryPairingPreferenceActivity extends AbstractGBActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);

        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(DiscoveryPairingPreferenceFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = new DiscoveryPairingPreferenceFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, DiscoveryPairingPreferenceFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    public static class DiscoveryPairingPreferenceFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "DISCOVERY_PAIRING_PREFERENCES_FRAGMENT";

        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            addPreferencesFromResource(R.xml.discovery_pairing_preferences);

        }
    }
}
