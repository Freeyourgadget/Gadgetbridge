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

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class NotificationManagementActivity extends AbstractSettingsActivity {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManagementActivity.class);
    private static final int RINGTONE_REQUEST_CODE = 4712;
    private static final String DEFAULT_RINGTONE_URI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_preferences);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Prefs prefs = GBApplication.getPrefs();
        Preference pref = findPreference("notifications_generic");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(enableIntent);
                return true;
            }
        });


        pref = findPreference(GBPrefs.PING_TONE);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Set Ping tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                startActivityForResult(intent, RINGTONE_REQUEST_CODE);
                return true;
            }
        });
        pref.setSummary(RingtoneManager.getRingtone(this, Uri.parse(prefs.getString(GBPrefs.PING_TONE, DEFAULT_RINGTONE_URI))).getTitle(this));

        pref = findPreference("pref_key_blacklist");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(NotificationManagementActivity.this, AppBlacklistActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

        if (!GBApplication.isRunningMarshmallowOrLater()) {
            pref = findPreference("notification_filter");
            PreferenceCategory category = (PreferenceCategory) findPreference("pref_key_notifications");
            category.removePreference(pref);
        }

        if (GBApplication.isRunningTenOrLater()) {
            pref = findPreference("minimize_priority");
            PreferenceCategory category = (PreferenceCategory) findPreference("pref_key_notifications");
            category.removePreference(pref);
        }
    }


    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RINGTONE_REQUEST_CODE && intent != null) {
            if (intent.getExtras().getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) != null) {
                Uri uri = intent.getExtras().getParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                Ringtone r = RingtoneManager.getRingtone(this, uri);
                findPreference(GBPrefs.PING_TONE).setSummary(r.toString());

                PreferenceManager
                        .getDefaultSharedPreferences(this)
                        .edit()
                        .putString(GBPrefs.PING_TONE, uri.toString())
                        .apply();
                findPreference(GBPrefs.PING_TONE).setSummary(r.getTitle(this));
            }
        }
    }


}
