/*  Copyright (C) 2018-2021 Andreas Shimokawa, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;

public class ZeTimePreferenceActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return ZeTimePreferencesFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new ZeTimePreferencesFragment();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GBApplication.deviceService().onReadConfiguration("do_it");
    }

    public static class ZeTimePreferencesFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "ZETIME_PREFERENCES_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.zetime_preferences, rootKey);

            final Preference heartrateMeasurementInterval = findPreference(ZeTimeConstants.PREF_ZETIME_HEARTRATE_INTERVAL);
            if (heartrateMeasurementInterval != null) {
                heartrateMeasurementInterval.setOnPreferenceChangeListener((preference, newVal) -> {
                    GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));
                    return true;
                });
            }

            setInputTypeFor(ZeTimeConstants.PREF_SCREENTIME, InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("fitness_goal", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("activity_user_sleep_duration", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("activity_user_calories_burnt", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("activity_user_distance_meters", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("activity_user_activetime_minutes", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("inactivity_warnings_threshold", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("alarm_max_heart_rate", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("alarm_min_heart_rate", InputType.TYPE_CLASS_NUMBER);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_SCREENTIME);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_ANALOG_MODE);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_ACTIVITY_TRACKING);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_HANDMOVE_DISPLAY);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_CALORIES_TYPE);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_DATE_FORMAT);

            addPreferenceHandlerFor(ZeTimeConstants.PREF_SMS_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_ANTI_LOSS_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_CALENDAR_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_CALL_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_MISSED_CALL_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_EMAIL_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_INACTIVITY_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_LOW_POWER_SIGNALING);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_SOCIAL_SIGNALING);


            addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_HEARTRATE_ALARM);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MAX_HEARTRATE);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_ZETIME_MIN_HEARTRATE);


            addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_SLEEP_GOAL);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_CALORIES_GOAL);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_DISTANCE_GOAL);
            addPreferenceHandlerFor(ZeTimeConstants.PREF_USER_ACTIVETIME_GOAL);
        }

        private void addPreferenceHandlerFor(final String preferenceKey) {
            final Preference pref = findPreference(preferenceKey);
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    notifyPreferenceChanged(preferenceKey);
                    return true;
                });
            }
        }

        /*
         * delayed execution so that the preferences are applied first
         */
        private void invokeLater(Runnable runnable) {
            getListView().post(runnable);
        }

        /*
         * delayed execution so that the preferences are applied first
         */
        public void notifyPreferenceChanged(final String preferenceKey) {
            invokeLater(() -> GBApplication.deviceService().onSendConfiguration(preferenceKey));
        }
    }
}
