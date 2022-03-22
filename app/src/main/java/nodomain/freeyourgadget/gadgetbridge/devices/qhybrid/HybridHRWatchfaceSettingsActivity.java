/*  Copyright (C) 2021 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;

public class HybridHRWatchfaceSettingsActivity extends AbstractSettingsActivity {
    static HybridHRWatchfaceSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            settings = (HybridHRWatchfaceSettings) bundle.getSerializable("watchfaceSettings");
        } else {
            throw new IllegalArgumentException("Must provide a settings object when invoking this activity");
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content, new HybridHRWatchfaceSettingsFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        // Hardware back button
        Intent output = new Intent();
        output.putExtra("watchfaceSettings", settings);
        setResult(RESULT_OK, output);
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Action bar back button
            Intent output = new Intent();
            output.putExtra("watchfaceSettings", settings);
            setResult(RESULT_OK, output);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class HybridHRWatchfaceSettingsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fossil_hr_watchface_settings);

            EditTextPreference refresh_full = (EditTextPreference) findPreference("pref_hybridhr_watchface_refresh_full");
            refresh_full.setOnPreferenceChangeListener(new PreferenceChangeListener());
            refresh_full.setText(Integer.toString(settings.getDisplayTimeoutFull()));
            refresh_full.setSummary(Integer.toString(settings.getDisplayTimeoutFull()));

            EditTextPreference refresh_partial = (EditTextPreference) findPreference("pref_hybridhr_watchface_refresh_partial");
            refresh_partial.setOnPreferenceChangeListener(new PreferenceChangeListener());
            refresh_partial.setText(Integer.toString(settings.getDisplayTimeoutPartial()));
            refresh_partial.setSummary(Integer.toString(settings.getDisplayTimeoutPartial()));

            SwitchPreference wrist_flick_relative = (SwitchPreference) findPreference("pref_hybridhr_watchface_wrist_flick_relative");
            wrist_flick_relative.setOnPreferenceChangeListener(new PreferenceChangeListener());
            wrist_flick_relative.setChecked(settings.isWristFlickHandsMoveRelative());

            EditTextPreference wrist_flick_hour_hand = (EditTextPreference) findPreference("pref_hybridhr_watchface_wrist_flick_hour_hand");
            wrist_flick_hour_hand.setOnPreferenceChangeListener(new PreferenceChangeListener());
            wrist_flick_hour_hand.setText(Integer.toString(settings.getWristFlickMoveHour()));
            wrist_flick_hour_hand.setSummary(Integer.toString(settings.getWristFlickMoveHour()));

            EditTextPreference wrist_flick_minute_hand = (EditTextPreference) findPreference("pref_hybridhr_watchface_wrist_flick_minute_hand");
            wrist_flick_minute_hand.setOnPreferenceChangeListener(new PreferenceChangeListener());
            wrist_flick_minute_hand.setText(Integer.toString(settings.getWristFlickMoveMinute()));
            wrist_flick_minute_hand.setSummary(Integer.toString(settings.getWristFlickMoveMinute()));

            EditTextPreference wrist_flick_duration = (EditTextPreference) findPreference("pref_hybridhr_watchface_wrist_flick_duration");
            wrist_flick_duration.setOnPreferenceChangeListener(new PreferenceChangeListener());
            wrist_flick_duration.setText(Integer.toString(settings.getWristFlickDuration()));
            wrist_flick_duration.setSummary(Integer.toString(settings.getWristFlickDuration()));

            SwitchPreference power_saving_display = (SwitchPreference) findPreference("pref_hybridhr_watchface_power_saving_display");
            power_saving_display.setOnPreferenceChangeListener(new PreferenceChangeListener());
            power_saving_display.setChecked(settings.getPowersaveDisplay());

            SwitchPreference power_saving_hands = (SwitchPreference) findPreference("pref_hybridhr_watchface_power_saving_hands");
            power_saving_hands.setOnPreferenceChangeListener(new PreferenceChangeListener());
            power_saving_hands.setChecked(settings.getPowersaveHands());

            SwitchPreference light_up_on_notification = (SwitchPreference) findPreference("pref_hybridhr_watchface_light_up_on_notification");
            light_up_on_notification.setOnPreferenceChangeListener(new PreferenceChangeListener());
            light_up_on_notification.setChecked(settings.getLightUpOnNotification());
        }

        private static class PreferenceChangeListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                switch (preference.getKey()) {
                    case "pref_hybridhr_watchface_refresh_full":
                        settings.setDisplayTimeoutFull(Integer.parseInt(newValue.toString()));
                        preference.setSummary(newValue.toString());
                        break;
                    case "pref_hybridhr_watchface_refresh_partial":
                        settings.setDisplayTimeoutPartial(Integer.parseInt(newValue.toString()));
                        preference.setSummary(newValue.toString());
                        break;
                    case "pref_hybridhr_watchface_wrist_flick_relative":
                        settings.setWristFlickHandsMoveRelative((boolean) newValue);
                        break;
                    case "pref_hybridhr_watchface_wrist_flick_hour_hand":
                        settings.setWristFlickMoveHour(Integer.parseInt(newValue.toString()));
                        preference.setSummary(newValue.toString());
                        break;
                    case "pref_hybridhr_watchface_wrist_flick_minute_hand":
                        settings.setWristFlickMoveMinute(Integer.parseInt(newValue.toString()));
                        preference.setSummary(newValue.toString());
                        break;
                    case "pref_hybridhr_watchface_light_up_on_notification":
                        settings.setLightUpOnNotification((boolean) newValue);
                        break;
                    case "pref_hybridhr_watchface_wrist_flick_duration":
                        settings.setWristFlickDuration(Integer.parseInt(newValue.toString()));
                        preference.setSummary(newValue.toString());
                        break;
                    case "pref_hybridhr_watchface_power_saving_display":
                        settings.setPowersaveDisplay((boolean) newValue);
                        break;
                    case "pref_hybridhr_watchface_power_saving_hands":
                        settings.setPowersaveHands((boolean) newValue);
                        break;
                }
                return true;
            }
        }
    }
}
