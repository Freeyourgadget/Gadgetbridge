package nodomain.freeyourgadget.gadgetbridge;

import android.content.Intent;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.miband.MiBandPreferencesActivity;

public class SettingsActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Preference pref = (Preference) findPreference("notifications_generic");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(enableIntent);
                return true;
            }
        });
        pref = (Preference) findPreference("pref_key_miband");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, MiBandPreferencesActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

        final Preference developmentMiaddr = findPreference(GB.PREF_DEVELOPMENT_MIBAND_ADDRESS);
        developmentMiaddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[] { GB.PREF_DEVELOPMENT_MIBAND_ADDRESS };
    }
}
