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
package nodomain.freeyourgadget.gadgetbridge.activities.discovery;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;

public class DiscoveryPairingPreferenceActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return DiscoveryPairingPreferenceFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new DiscoveryPairingPreferenceFragment();
    }

    public static class DiscoveryPairingPreferenceFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "DISCOVERY_PAIRING_PREFERENCES_FRAGMENT";

        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.discovery_pairing_preferences, rootKey);
        }
    }
}
