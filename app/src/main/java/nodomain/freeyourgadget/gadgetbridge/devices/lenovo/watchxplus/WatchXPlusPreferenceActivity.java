/*  Copyright (C) 2018-2019 Sebastian Kranz

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

package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import android.os.Bundle;
import android.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class WatchXPlusPreferenceActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.watchxplus_preferences);

        // notifications
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_REPEAT);
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_CONTINIOUS);
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_MISSED_CALL);
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_MISSED_CALL_REPEAT);
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_BUTTON_REJECT);
        //addPreferenceHandlerFor(WatchXPlusConstants.PREF_SHAKE_REJECT);

        // settings
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_POWER_MODE);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_WXP_LANGUAGE);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_LONGSIT_PERIOD);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_LONGSIT_SWITCH);
        // calibration
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_ALTITUDE);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_BP_CAL_LOW);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_BP_CAL_HIGH);
        addPreferenceHandlerFor(WatchXPlusConstants.PREF_BP_CAL_SWITCH);

    }

    private void addPreferenceHandlerFor(final String preferenceKey) {
        Preference pref = findPreference(preferenceKey);
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override public boolean onPreferenceChange(Preference preference, Object newVal) {
                GBApplication.deviceService().onSendConfiguration(preferenceKey);
                return true;
            }
        });
    }
}
