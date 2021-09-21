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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;

import androidx.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_ACTIVETIME_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_SLEEP_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEPS_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEP_LENGTH_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_YEAR_OF_BIRTH;

public class AboutUserPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_user);
        addPreferenceHandlerFor(PREF_USER_NAME);
        addPreferenceHandlerFor(PREF_USER_YEAR_OF_BIRTH);
        addPreferenceHandlerFor(PREF_USER_HEIGHT_CM);
        addPreferenceHandlerFor(PREF_USER_WEIGHT_KG);
        addPreferenceHandlerFor(PREF_USER_GENDER);
        addPreferenceHandlerFor(PREF_USER_STEPS_GOAL);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                PREF_USER_YEAR_OF_BIRTH,
                PREF_USER_HEIGHT_CM,
                PREF_USER_WEIGHT_KG,
                PREF_USER_SLEEP_DURATION,
                PREF_USER_STEPS_GOAL,
                PREF_USER_STEP_LENGTH_CM,
                PREF_USER_ACTIVETIME_MINUTES,
                PREF_USER_CALORIES_BURNT,
                PREF_USER_DISTANCE_METERS,};
    }
}
