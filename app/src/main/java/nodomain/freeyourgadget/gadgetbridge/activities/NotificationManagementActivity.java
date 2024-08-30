/*  Copyright (C) 2021-2024 José Rebelo, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class NotificationManagementActivity extends AbstractSettingsActivityV2 {
    private static final int RINGTONE_REQUEST_CODE = 4712;
    private static final String DEFAULT_RINGTONE_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString();

    @Override
    protected String fragmentTag() {
        return NotificationPreferencesFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new NotificationPreferencesFragment();
    }

    public static class NotificationPreferencesFragment extends AbstractPreferenceFragment {
        private static final Logger LOG = LoggerFactory.getLogger(NotificationPreferencesFragment.class);

        static final String FRAGMENT_TAG = "NOTIFICATION_PREFERENCES_FRAGMENT";

        @Override
        protected void onSharedPreferenceChanged(final Preference preference) {
            if (GBPrefs.PING_TONE.equals(preference.getKey())) {
                try {
                    final Prefs prefs = GBApplication.getPrefs();
                    // This fails on some ROMs. The actual implementation falls-back to an internal ping tone
                    preference.setSummary(RingtoneManager.getRingtone(requireContext(), Uri.parse(prefs.getString(GBPrefs.PING_TONE, DEFAULT_RINGTONE_URI))).getTitle(requireContext()));
                } catch (final Exception e) {
                    LOG.error("Failed to find the configured ping ringtone", e);
                    preference.setSummary("-");
                }
            }
        }

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.notifications_preferences, rootKey);

            Preference pref = findPreference("notifications_generic");
            pref.setOnPreferenceClickListener(preference -> {
                final Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(enableIntent);
                return true;
            });

            pref = findPreference(GBPrefs.PING_TONE);
            pref.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Set Ping tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                startActivityForResult(intent, RINGTONE_REQUEST_CODE);
                return true;
            });

            pref = findPreference("pref_key_blacklist");
            pref.setOnPreferenceClickListener(preference -> {
                final Intent enableIntent = new Intent(requireContext(), AppBlacklistActivity.class);
                startActivity(enableIntent);
                return true;
            });

            pref = findPreference("notifications_settings");
            pref.setOnPreferenceClickListener(preference -> {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                    LOG.warn("This preference should not be displayed in Android < O");
                    return true;
                }

                final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
                //This could open notification channel settings, if needed...:
                //Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                //intent.putExtra(Settings.EXTRA_CHANNEL_ID, GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
                startActivity(intent);
                return true;
            });

            final PreferenceCategory notificationsCategory = findPreference("pref_key_notifications");

            if (!GBApplication.isRunningMarshmallowOrLater()) {
                pref = findPreference("notification_filter");
                notificationsCategory.removePreference(pref);
            }

            if (GBApplication.isRunningTenOrLater()) {
                pref = findPreference("minimize_priority");
                notificationsCategory.removePreference(pref);
            }

            if (!GBApplication.isRunningOreoOrLater()) {
                pref = findPreference("notifications_settings");
                notificationsCategory.removePreference(pref);
            }
        }

        // TODO: Migrate this to ActivityResultContract
        @Override
        public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
            if (requestCode == RINGTONE_REQUEST_CODE && intent != null) {
                if (intent.getExtras().getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) != null) {
                    final Uri uri = intent.getExtras().getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    GBApplication.getPrefs()
                            .getPreferences()
                            .edit()
                            .putString(GBPrefs.PING_TONE, uri.toString())
                            .apply();
                }
            }
        }
    }
}
