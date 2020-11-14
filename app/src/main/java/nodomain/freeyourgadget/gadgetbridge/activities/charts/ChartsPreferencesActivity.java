/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Lem Dulfo,
    vanous

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AboutUserPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ChartsPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.charts_preferences);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Preference pref = findPreference("pref_category_activity_personal");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(ChartsPreferencesActivity.this, AboutUserPreferencesActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

    }


    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                "chart_list_min_session_length",
                "chart_list_max_idle_phase_length",
                "chart_list_min_steps_per_minute",
                "chart_list_min_steps_per_minute_for_run",
                GBPrefs.CHART_MAX_HEART_RATE,
                GBPrefs.CHART_MIN_HEART_RATE,
        };
    }

}
