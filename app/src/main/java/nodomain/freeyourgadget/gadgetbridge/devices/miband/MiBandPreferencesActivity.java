/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Christian
    Fischer, Daniele Gobbetti, José Rebelo, Szymon Tomasz Stefanek

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_ALARM_CLOCK;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_INCOMING_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ADDRESS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefKey;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_NAME;

public class MiBandPreferencesActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.miband_preferences);

        addTryListeners();

        Prefs prefs = GBApplication.getPrefs();

        final Preference fitnessGoal = findPreference(ActivityUser.PREF_USER_STEPS_GOAL);
        fitnessGoal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(ActivityUser.PREF_USER_STEPS_GOAL);
                    }
                });
                return true;
            }
        });
    }

    /**
     * delayed execution so that the preferences are applied first
      */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final Preference developmentMiaddr = findPreference(PREF_MIBAND_ADDRESS);
        developmentMiaddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });
    }

    private void addTryListeners() {
        for (final NotificationType type : NotificationType.values()) {
            String prefKey = "mi_try_" + type.getGenericType();
            final Preference tryPref = findPreference(prefKey);
            if (tryPref != null) {
                tryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        tryVibration(type);
                        return true;
                    }
                });
            } else {
                GB.toast(getBaseContext(), "Unable to find preference key: " + prefKey + ", trying the vibration won't work", Toast.LENGTH_LONG, GB.WARN);
            }
        }
    }

    private void tryVibration(NotificationType type) {
        NotificationSpec spec = new NotificationSpec();
        spec.type = type;
        GBApplication.deviceService().onNotification(spec);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        Set<String> prefKeys = new HashSet<>();
        prefKeys.add(PREF_USER_NAME);
        prefKeys.add(PREF_MIBAND_ADDRESS);
        prefKeys.add(ActivityUser.PREF_USER_STEPS_GOAL);
        prefKeys.add(PREF_INACTIVITY_THRESHOLD);
        prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_ALARM_CLOCK));
        prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, ORIGIN_INCOMING_CALL));

        for (NotificationType type : NotificationType.values()) {
            String key = type.getGenericType();
            prefKeys.add(getNotificationPrefKey(VIBRATION_COUNT, key));
        }

        return prefKeys.toArray(new String[0]);
    }
}
