package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;

public class SettingsActivity extends AbstractSettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Preference pref = findPreference("notifications_generic");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(enableIntent);
                return true;
            }
        });
        pref = findPreference("pref_key_miband");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, MiBandPreferencesActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

        pref = findPreference("pref_key_blacklist");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, AppBlacklistActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

        final Preference pebbleEmuAddr = findPreference("pebble_emu_addr");
        pebbleEmuAddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        final Preference pebbleEmuPort = findPreference("pebble_emu_port");
        pebbleEmuPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
        return new String[]{
                "pebble_emu_addr",
                "pebble_emu_port"
        };
    }

}
