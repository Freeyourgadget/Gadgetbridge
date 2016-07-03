package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_SLEEP_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_YEAR_OF_BIRTH;

public class SettingsActivity extends AbstractSettingsActivity {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsActivity.class);

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
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        pref = findPreference("pebble_emu_port");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshIntent);
                preference.setSummary(newVal.toString());
                return true;
            }

        });

        pref = findPreference("log_to_file");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                boolean doEnable = Boolean.TRUE.equals(newVal);
                try {
                    if (doEnable) {
                        FileUtils.getExternalFilesDir(); // ensures that it is created
                    }
                    GBApplication.setupLogging(doEnable);
                } catch (IOException ex) {
                    GB.toast(getApplicationContext(),
                            getString(R.string.error_creating_directory_for_logfiles, ex.getLocalizedMessage()),
                            Toast.LENGTH_LONG,
                            GB.ERROR,
                            ex);
                }
                return true;
            }

        });

        if (!GBApplication.isRunningMarshmallowOrLater()) {
            pref = findPreference("notification_filter");
            PreferenceCategory category = (PreferenceCategory) findPreference("pref_key_notifications");
            category.removePreference(pref);
        }

        pref = findPreference("location_aquire");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                }

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, false);
                if (provider != null) {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null) {
                        String latitude = String.format(Locale.US, "%.6g", location.getLatitude());
                        String longitude = String.format(Locale.US, "%.6g", location.getLongitude());
                        LOG.info("got location. Lat: " + latitude + " Lng: " + longitude);
                        EditTextPreference pref_latitude = (EditTextPreference) findPreference("location_latitude");
                        EditTextPreference pref_longitude = (EditTextPreference) findPreference("location_longitude");
                        pref_latitude.setText(latitude);
                        pref_longitude.setText(longitude);
                        pref_latitude.setSummary(latitude);
                        pref_longitude.setSummary(longitude);
                    } else {
                        GB.toast(SettingsActivity.this, "no last known position", 3000, 0);
                    }
                } else {
                    LOG.warn("No location provider found, did you deny location permission?");
                }
                return true;
            }
        });

        pref = findPreference("canned_messages_dismisscall_send");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Prefs prefs = GBApplication.getPrefs();
                ArrayList<String> messages = new ArrayList<>();
                for (int i = 1; i <= 16; i++) {
                    String message = prefs.getString("canned_message_dismisscall_" + i, null);
                    if (message != null && !message.equals("")) {
                        messages.add(message);
                    }
                }
                CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                cannedMessagesSpec.type = CannedMessagesSpec.TYPE_MISSEDCALLS;
                cannedMessagesSpec.cannedMessages = messages.toArray(new String[messages.size()]);
                GBApplication.deviceService().onSetCannedMessages(cannedMessagesSpec);
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
                "pebble_emu_addr",
                "pebble_emu_port",
                "pebble_reconnect_attempts",
                "location_latitude",
                "location_longitude",
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
                "canned_message_dismisscall_1",
                "canned_message_dismisscall_2",
                "canned_message_dismisscall_3",
                "canned_message_dismisscall_4",
                "canned_message_dismisscall_5",
                "canned_message_dismisscall_6",
                "canned_message_dismisscall_7",
                "canned_message_dismisscall_8",
                "canned_message_dismisscall_9",
                "canned_message_dismisscall_10",
                "canned_message_dismisscall_11",
                "canned_message_dismisscall_12",
                "canned_message_dismisscall_13",
                "canned_message_dismisscall_14",
                "canned_message_dismisscall_15",
                "canned_message_dismisscall_16",
                PREF_USER_YEAR_OF_BIRTH,
                PREF_USER_HEIGHT_CM,
                PREF_USER_WEIGHT_KG,
                PREF_USER_SLEEP_DURATION,
        };
    }

}
