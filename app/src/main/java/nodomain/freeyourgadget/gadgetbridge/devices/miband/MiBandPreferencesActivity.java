/*  Copyright (C) 2015-2023 Andreas Shimokawa, Carsten Pfeiffer, Christian
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

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ADDRESS;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class MiBandPreferencesActivity extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return MiBandPreferencesFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new MiBandPreferencesFragment();
    }

    public static class MiBandPreferencesFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "MIBAND_PREFERENCES_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.miband_preferences, rootKey);

            for (final NotificationType type : NotificationType.values()) {
                String countPrefKey = "mi_vibration_count_" + type.getGenericType();
                setInputTypeFor(countPrefKey, InputType.TYPE_CLASS_NUMBER);
                String tryPrefKey = "mi_try_" + type.getGenericType();
                final Preference tryPref = findPreference(tryPrefKey);
                if (tryPref != null) {
                    tryPref.setOnPreferenceClickListener(preference -> {
                        tryVibration(type);
                        return true;
                    });
                }
            }

            final Preference developmentMiAddr = findPreference(PREF_MIBAND_ADDRESS);
            if (developmentMiAddr != null) {
                developmentMiAddr.setOnPreferenceChangeListener((preference, newVal) -> {
                    Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                    LocalBroadcastManager.getInstance(requireActivity().getBaseContext()).sendBroadcast(refreshIntent);
                    return true;
                });
            }
        }

        /**
         * delayed execution so that the preferences are applied first
         */
        private void invokeLater(Runnable runnable) {
            getListView().post(runnable);
        }

        private void tryVibration(NotificationType type) {
            NotificationSpec spec = new NotificationSpec();
            spec.type = type;
            GBApplication.deviceService().onNotification(spec);
        }
    }
}
