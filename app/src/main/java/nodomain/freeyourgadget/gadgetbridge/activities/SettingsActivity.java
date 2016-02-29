package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_SLEEP_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_YEAR_OF_BIRTH;

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

        pref = findPreference("pebble_emu_addr");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        pref = findPreference("pebble_emu_port");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        // Get all receivers of Media Buttons
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> mediaReceivers = pm.queryBroadcastReceivers(mediaButtonIntent,
                PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);


        CharSequence[] newEntries = new CharSequence[mediaReceivers.size() + 1];
        CharSequence[] newValues = new CharSequence[mediaReceivers.size() + 1];
        newEntries[0] = getString(R.string.pref_default);
        newValues[0] = "default";

        int i = 1;
        for (ResolveInfo resolveInfo : mediaReceivers) {
            newEntries[i] = resolveInfo.activityInfo.loadLabel(pm);
            newValues[i] = resolveInfo.activityInfo.packageName;
            i++;
        }

        final ListPreference audioPlayer = (ListPreference) findPreference("audio_player");
        audioPlayer.setEntries(newEntries);
        audioPlayer.setEntryValues(newValues);
        audioPlayer.setDefaultValue(newValues[0]);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                "audio_player",
                "notification_mode_calls",
                "notification_mode_sms",
                "notification_mode_k9mail",
                "pebble_activitytracker",
                "pebble_emu_addr",
                "pebble_emu_port",
                "pebble_reconnect_attempts",
                "canned_reply_suffix",
                "canned_reply_1",
                "canned_reply_2",
                "canned_reply_3",
                "canned_reply_4",
                "canned_reply_5",
                "canned_reply_6",
                "canned_reply_7",
                "canned_reply_8",
                "canned_reply_9",
                "canned_reply_10",
                "canned_reply_11",
                "canned_reply_12",
                "canned_reply_13",
                "canned_reply_14",
                "canned_reply_15",
                "canned_reply_16",
                PREF_USER_YEAR_OF_BIRTH,
                PREF_USER_GENDER,
                PREF_USER_HEIGHT_CM,
                PREF_USER_WEIGHT_KG,
                PREF_USER_SLEEP_DURATION,
        };
    }

}
