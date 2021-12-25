/*  Copyright (C) 2015-2020 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniel Dakhno, Daniele Gobbetti, Felix Konstantin Maurer, José Rebelo,
    Martin, Normano64, Pavel Elagin, Sebastian Kranz, vanous

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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.ConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.zetime.ZeTimePreferenceActivity;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SettingsActivity extends AbstractSettingsActivity {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsActivity.class);

    public static final String PREF_MEASUREMENT_SYSTEM = "measurement_system";

    private static final int FILE_REQUEST_CODE = 4711;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Prefs prefs = GBApplication.getPrefs();
        Preference pref = findPreference("pref_category_activity_personal");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, AboutUserPreferencesActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });


        pref = findPreference("pref_charts");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, ChartsPreferencesActivity.class);
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

        pref = findPreference("pref_key_qhybrid");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, ConfigActivity.class));
                return true;
            }
        });

        pref = findPreference("pref_key_zetime");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, ZeTimePreferenceActivity.class);
                startActivity(enableIntent);
                return true;
            }
        });

        pref = findPreference("pref_key_blacklist_calendars");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent enableIntent = new Intent(SettingsActivity.this, CalBlacklistActivity.class);
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



        pref = findPreference("language");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                String newLang = newVal.toString();
                try {
                    GBApplication.setLanguage(newLang);
                    recreate();
                } catch (Exception ex) {
                    GB.toast(getApplicationContext(),
                            "Error setting language: " + ex.getLocalizedMessage(),
                            Toast.LENGTH_LONG,
                            GB.ERROR,
                            ex);
                }
                return true;
            }

        });

        final Preference unit = findPreference(PREF_MEASUREMENT_SYSTEM);
        unit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSendConfiguration(PREF_MEASUREMENT_SYSTEM);
                    }
                });
                preference.setSummary(newVal.toString());
                return true;
            }
        });

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
                        setLocationPreferences(location);
                    } else {
                        locationManager.requestSingleUpdate(provider, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                setLocationPreferences(location);
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {
                                LOG.info("provider status changed to " + status + " (" + provider + ")");
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                                LOG.info("provider enabled (" + provider + ")");
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                                LOG.info("provider disabled (" + provider + ")");
                                GB.toast(SettingsActivity.this, getString(R.string.toast_enable_networklocationprovider), 3000, 0);
                            }
                        }, null);
                    }
                } else {
                    LOG.warn("No location provider found, did you deny location permission?");
                }
                return true;
            }
        });

        pref = findPreference("weather_city");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                // reset city id and force a new lookup
                GBApplication.getPrefs().getPreferences().edit().putString("weather_cityid", null).apply();
                preference.setSummary(newVal.toString());
                Intent intent = new Intent("GB_UPDATE_WEATHER");
                intent.setPackage(BuildConfig.APPLICATION_ID);
                sendBroadcast(intent);
                return true;
            }
        });


        pref = findPreference(GBPrefs.AUTO_EXPORT_LOCATION);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.setType("application/x-sqlite3");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                String title = getApplicationContext().getString(R.string.choose_auto_export_location);
                startActivityForResult(Intent.createChooser(i, title), FILE_REQUEST_CODE);
                return true;
            }
        });
        pref.setSummary(getAutoExportLocationSummary());

        pref = findPreference(GBPrefs.AUTO_EXPORT_INTERVAL);
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object autoExportInterval) {
                String summary = String.format(
                        getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                        Integer.valueOf((String) autoExportInterval));
                preference.setSummary(summary);
                boolean auto_export_enabled = GBApplication.getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
                PeriodicExporter.sheduleAlarm(getApplicationContext(), Integer.valueOf((String) autoExportInterval), auto_export_enabled);
                return true;
            }
        });
        int autoExportInterval = GBApplication.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
        String summary = String.format(
                getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                (int) autoExportInterval);
        pref.setSummary(summary);

        findPreference(GBPrefs.AUTO_EXPORT_ENABLED).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object autoExportEnabled) {
                int autoExportInterval = GBApplication.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                PeriodicExporter.sheduleAlarm(getApplicationContext(), autoExportInterval, (boolean) autoExportEnabled);
                return true;
            }
        });

        pref = findPreference("auto_fetch_interval_limit");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object autoFetchInterval) {
                String summary = String.format(
                        getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                        Integer.valueOf((String) autoFetchInterval));
                preference.setSummary(summary);
                return true;
            }
        });

        int autoFetchInterval = GBApplication.getPrefs().getInt("auto_fetch_interval_limit", 0);
        summary = String.format(
                getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                autoFetchInterval);
        pref.setSummary(summary);

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
        Set<String> existingNames = new HashSet<>();
        for (ResolveInfo resolveInfo : mediaReceivers) {
            newEntries[i] = resolveInfo.activityInfo.loadLabel(pm) + " (" + resolveInfo.activityInfo.packageName + ")";
            if (existingNames.contains(newEntries[i].toString().trim())) {
                newEntries[i] = resolveInfo.activityInfo.loadLabel(pm) + " (" + resolveInfo.activityInfo.name + ")";
            } else {
                existingNames.add(newEntries[i].toString().trim());
            }
            newValues[i] = resolveInfo.activityInfo.packageName;
            i++;
        }

        final ListPreference audioPlayer = (ListPreference) findPreference("audio_player");
        audioPlayer.setEntries(newEntries);
        audioPlayer.setEntryValues(newValues);
        audioPlayer.setDefaultValue(newValues[0]);

        final Preference theme = (ListPreference) findPreference("pref_key_theme");
        final Preference amoled_black = findPreference("pref_key_theme_amoled_black");

        String selectedTheme = prefs.getString("pref_key_theme", SettingsActivity.this.getString(R.string.pref_theme_value_system));
        if (selectedTheme.equals("light"))
            amoled_black.setEnabled(false);
        else
            amoled_black.setEnabled(true);

        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newVal) {
                final String val = newVal.toString();
                if (val.equals("light"))
                    amoled_black.setEnabled(false);
                else
                    amoled_black.setEnabled(true);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_REQUEST_CODE && intent != null) {
            Uri uri = intent.getData();
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            PreferenceManager
                    .getDefaultSharedPreferences(this)
                    .edit()
                    .putString(GBPrefs.AUTO_EXPORT_LOCATION, uri.toString())
                    .apply();
            String summary = getAutoExportLocationSummary();
            findPreference(GBPrefs.AUTO_EXPORT_LOCATION).setSummary(summary);
            boolean autoExportEnabled = GBApplication
                    .getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
            int autoExportPeriod = GBApplication
                    .getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
            PeriodicExporter.sheduleAlarm(getApplicationContext(), autoExportPeriod, autoExportEnabled);
        }
    }

    /*
    Either returns the file path of the selected document, or the display name, or an empty string
     */
    public String getAutoExportLocationSummary() {
        String autoExportLocation = GBApplication.getPrefs().getString(GBPrefs.AUTO_EXPORT_LOCATION, null);
        if (autoExportLocation == null) {
            return "";
        }
        Uri uri = Uri.parse(autoExportLocation);
        try {
            return AndroidUtils.getFilePath(getApplicationContext(), uri);
        } catch (IllegalArgumentException e) {
            try {
                Cursor cursor = getContentResolver().query(
                        uri,
                        new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                        null, null, null, null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                }
            } catch (Exception fdfsdfds) {
                LOG.warn("fuck");
            }
        }
        return "";
    }

    /*
     * delayed execution so that the preferences are applied first
     */
    private void invokeLater(Runnable runnable) {
        getListView().post(runnable);
    }

    @Override
    protected String[] getPreferenceKeysWithSummary() {
        return new String[]{
                "pebble_emu_addr",
                "pebble_emu_port",
                "pebble_reconnect_attempts",
                "location_latitude",
                "location_longitude",
                "weather_city",
        };
    }

    private void setLocationPreferences(Location location) {
        String latitude = String.format(Locale.US, "%.6g", location.getLatitude());
        String longitude = String.format(Locale.US, "%.6g", location.getLongitude());
        LOG.info("got location. Lat: " + latitude + " Lng: " + longitude);
        GB.toast(SettingsActivity.this, getString(R.string.toast_aqurired_networklocation), 2000, 0);
        EditTextPreference pref_latitude = (EditTextPreference) findPreference("location_latitude");
        EditTextPreference pref_longitude = (EditTextPreference) findPreference("location_longitude");
        pref_latitude.setText(latitude);
        pref_longitude.setText(longitude);
        pref_latitude.setSummary(latitude);
        pref_longitude.setSummary(longitude);
    }
}
