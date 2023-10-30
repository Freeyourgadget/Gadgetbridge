/*  Copyright (C) 2015-2023 0nse, Andreas Shimokawa, Carsten Pfeiffer,
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryPairingPreferenceActivity;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.ConfigActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.zetime.ZeTimePreferenceActivity;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SettingsActivity extends AbstractSettingsActivityV2 {
    public static final String PREF_MEASUREMENT_SYSTEM = "measurement_system";

    @Override
    protected String fragmentTag() {
        return SettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new SettingsFragment();
    }

    public static class SettingsFragment extends AbstractPreferenceFragment {
        private static final Logger LOG = LoggerFactory.getLogger(SettingsActivity.class);

        static final String FRAGMENT_TAG = "SETTINGS_FRAGMENT";

        private static final int EXPORT_LOCATION_FILE_REQUEST_CODE = 4711;
        private EditText fitnessAppEditText = null;
        private int fitnessAppSelectionListSpinnerFirstRun = 0;

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            setInputTypeFor("rtl_max_line_length", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("location_latitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            setInputTypeFor("location_longitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            setInputTypeFor("auto_export_interval", InputType.TYPE_CLASS_NUMBER);
            setInputTypeFor("auto_fetch_interval_limit", InputType.TYPE_CLASS_NUMBER);

            Prefs prefs = GBApplication.getPrefs();
            Preference pref = findPreference("pref_category_activity_personal");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), AboutUserPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_charts");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), ChartsPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_key_miband");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), MiBandPreferencesActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_key_qhybrid");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireContext(), ConfigActivity.class));
                    return true;
                });
            }

            pref = findPreference("pref_key_pebble");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), PebbleSettingsActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_key_hplus");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), HPlusSettingsActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("pref_key_zetime");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), ZeTimePreferenceActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            pref = findPreference("log_to_file");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    boolean doEnable = Boolean.TRUE.equals(newVal);
                    try {
                        if (doEnable) {
                            FileUtils.getExternalFilesDir(); // ensures that it is created
                        }
                        GBApplication.setupLogging(doEnable);
                    } catch (IOException ex) {
                        GB.toast(requireContext().getApplicationContext(),
                                getString(R.string.error_creating_directory_for_logfiles, ex.getLocalizedMessage()),
                                Toast.LENGTH_LONG,
                                GB.ERROR,
                                ex);
                    }
                    return true;
                });

                // If we didn't manage to initialize file logging, disable the preference
                if (!GBApplication.getLogging().isFileLoggerInitialized()) {
                    pref.setEnabled(false);
                    pref.setSummary(R.string.pref_write_logfiles_not_available);
                }
            }

            pref = findPreference("cache_weather");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    boolean doEnable = Boolean.TRUE.equals(newVal);

                    Weather.getInstance().setCacheFile(requireContext().getCacheDir(), doEnable);

                    return true;
                });
            }

            pref = findPreference("language");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    String newLang = newVal.toString();
                    try {
                        GBApplication.setLanguage(newLang);
                        requireActivity().recreate();
                    } catch (Exception ex) {
                        GB.toast(requireContext().getApplicationContext(),
                                "Error setting language: " + ex.getLocalizedMessage(),
                                Toast.LENGTH_LONG,
                                GB.ERROR,
                                ex);
                    }
                    return true;
                });
            }

            final Preference unit = findPreference(PREF_MEASUREMENT_SYSTEM);
            if (unit != null) {
                unit.setOnPreferenceChangeListener((preference, newVal) -> {
                    invokeLater(() -> GBApplication.deviceService().onSendConfiguration(PREF_MEASUREMENT_SYSTEM));
                    return true;
                });
            }

            pref = findPreference("location_aquire");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    if (ActivityCompat.checkSelfPermission(requireContext().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    }

                    LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
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
                                    GB.toast(requireContext(), getString(R.string.toast_enable_networklocationprovider), 3000, 0);
                                }
                            }, null);
                        }
                    } else {
                        LOG.warn("No location provider found, did you deny location permission?");
                    }
                    return true;
                });
            }

            pref = findPreference("weather_city");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newVal) -> {
                    // reset city id and force a new lookup
                    GBApplication.getPrefs().getPreferences().edit().putString("weather_cityid", null).apply();
                    Intent intent = new Intent("GB_UPDATE_WEATHER");
                    intent.setPackage(BuildConfig.APPLICATION_ID);
                    requireContext().sendBroadcast(intent);
                    return true;
                });
            }


            pref = findPreference(GBPrefs.AUTO_EXPORT_LOCATION);
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.setType("application/x-sqlite3");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE, "Gadgetbridge.db");
                    i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    String title = requireContext().getApplicationContext().getString(R.string.choose_auto_export_location);
                    startActivityForResult(Intent.createChooser(i, title), EXPORT_LOCATION_FILE_REQUEST_CODE);
                    return true;
                });
                pref.setSummary(getAutoExportLocationSummary());
            }

            pref = findPreference(GBPrefs.AUTO_EXPORT_INTERVAL);
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, autoExportInterval) -> {
                    String summary = String.format(
                            requireContext().getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                            Integer.valueOf((String) autoExportInterval));
                    preference.setSummary(summary);
                    boolean auto_export_enabled = GBApplication.getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
                    PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), Integer.valueOf((String) autoExportInterval), auto_export_enabled);
                    return true;
                });
                int autoExportInterval = GBApplication.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                String summary = String.format(
                        requireContext().getApplicationContext().getString(R.string.pref_summary_auto_export_interval),
                        autoExportInterval);
                pref.setSummary(summary);
            }

            pref = findPreference(GBPrefs.AUTO_EXPORT_ENABLED);
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, autoExportEnabled) -> {
                    int autoExportInterval = GBApplication.getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                    PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), autoExportInterval, (boolean) autoExportEnabled);
                    return true;
                });
            }

            pref = findPreference("auto_fetch_interval_limit");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, autoFetchInterval) -> {
                    String summary = String.format(
                            requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                            Integer.valueOf((String) autoFetchInterval));
                    preference.setSummary(summary);
                    return true;
                });

                int autoFetchInterval = GBApplication.getPrefs().getInt("auto_fetch_interval_limit", 0);
                String summary = String.format(
                        requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                        autoFetchInterval);
                pref.setSummary(summary);
            }

            final ListPreference audioPlayer = findPreference("audio_player");
            if (audioPlayer != null) {
                // Get all receivers of Media Buttons
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);

                PackageManager pm = requireContext().getPackageManager();
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

                audioPlayer.setEntries(newEntries);
                audioPlayer.setEntryValues(newValues);
                audioPlayer.setDefaultValue(newValues[0]);
            }

            final Preference theme = findPreference("pref_key_theme");
            final Preference amoled_black = findPreference("pref_key_theme_amoled_black");

            if (amoled_black != null) {
                String selectedTheme = prefs.getString("pref_key_theme", requireContext().getString(R.string.pref_theme_value_system));
                if (selectedTheme.equals("light"))
                    amoled_black.setEnabled(false);
                else
                    amoled_black.setEnabled(true);
                amoled_black.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        sendThemeChangeIntent();
                        return true;
                    }
                });
            }

            if (theme != null) {
                theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        final String val = newVal.toString();
                        if (amoled_black != null) {
                            if (val.equals("light"))
                                amoled_black.setEnabled(false);
                            else
                                amoled_black.setEnabled(true);
                        }
                        // Warn user if dynamic colors are not available
                        if (val.equals(requireContext().getString(R.string.pref_theme_value_dynamic)) && !DynamicColors.isDynamicColorAvailable()) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.pref_theme_dynamic_colors_not_available_warning)
                                    .setIcon(R.drawable.ic_warning)
                                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                                        sendThemeChangeIntent();
                                    })
                                    .show();
                        } else {
                            sendThemeChangeIntent();
                        }
                        return true;
                    }
                });
            }

            pref = findPreference("pref_discovery_pairing");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Intent enableIntent = new Intent(requireContext(), DiscoveryPairingPreferenceActivity.class);
                    startActivity(enableIntent);
                    return true;
                });
            }

            //fitness app (OpenTracks) package name selection for OpenTracks observer
            pref = findPreference("pref_key_opentracks_packagename");
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    final LinearLayout outerLayout = new LinearLayout(requireContext());
                    outerLayout.setOrientation(LinearLayout.VERTICAL);
                    final LinearLayout innerLayout = new LinearLayout(requireContext());
                    innerLayout.setOrientation(LinearLayout.HORIZONTAL);
                    innerLayout.setPadding(20, 0, 20, 0);
                    final Spinner selectionListSpinner = new Spinner(requireContext());
                    String[] appListArray = getResources().getStringArray(R.array.fitness_tracking_apps_package_names);
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, appListArray);
                    selectionListSpinner.setAdapter(spinnerArrayAdapter);
                    fitnessAppSelectionListSpinnerFirstRun = 0;
                    addListenerOnSpinnerDeviceSelection(selectionListSpinner);
                    Prefs prefs1 = GBApplication.getPrefs();
                    String packageName = prefs1.getString("opentracks_packagename", "de.dennisguse.opentracks");
                    // Set the spinner to the selected package name by default
                    for (int i = 0; i < appListArray.length; i++) {
                        if (appListArray[i].equals(packageName)) {
                            selectionListSpinner.setSelection(i);
                            break;
                        }
                    }
                    fitnessAppEditText = new EditText(requireContext());
                    fitnessAppEditText.setText(packageName);
                    innerLayout.addView(fitnessAppEditText);
                    outerLayout.addView(selectionListSpinner);
                    outerLayout.addView(innerLayout);

                    new MaterialAlertDialogBuilder(requireContext())
                            .setCancelable(true)
                            .setTitle(R.string.pref_title_opentracks_packagename)
                            .setView(outerLayout)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                SharedPreferences.Editor editor = GBApplication.getPrefs().getPreferences().edit();
                                editor.putString("opentracks_packagename", fitnessAppEditText.getText().toString());
                                editor.apply();
                            })
                            .setNegativeButton(R.string.Cancel, (dialog, which) -> {})
                            .show();
                    return false;
                });
            }
        }

        private void addListenerOnSpinnerDeviceSelection(Spinner spinner) {
            spinner.setOnItemSelectedListener(new CustomOnDeviceSelectedListener());
        }

        public class CustomOnDeviceSelectedListener implements AdapterView.OnItemSelectedListener {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (++fitnessAppSelectionListSpinnerFirstRun > 1) { //this prevents the setText to be set when spinner just is being initialized
                    fitnessAppEditText.setText(parent.getItemAtPosition(pos).toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            if (requestCode == EXPORT_LOCATION_FILE_REQUEST_CODE && intent != null) {
                Uri uri = intent.getData();
                requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                GBApplication.getPrefs().getPreferences()
                        .edit()
                        .putString(GBPrefs.AUTO_EXPORT_LOCATION, uri.toString())
                        .apply();
                String summary = getAutoExportLocationSummary();
                findPreference(GBPrefs.AUTO_EXPORT_LOCATION).setSummary(summary);
                boolean autoExportEnabled = GBApplication
                        .getPrefs().getBoolean(GBPrefs.AUTO_EXPORT_ENABLED, false);
                int autoExportPeriod = GBApplication
                        .getPrefs().getInt(GBPrefs.AUTO_EXPORT_INTERVAL, 0);
                PeriodicExporter.scheduleAlarm(requireContext().getApplicationContext(), autoExportPeriod, autoExportEnabled);
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
                return AndroidUtils.getFilePath(requireContext().getApplicationContext(), uri);
            } catch (IllegalArgumentException e) {
                try {
                    Cursor cursor = requireContext().getContentResolver().query(
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

        private void setLocationPreferences(Location location) {
            String latitude = String.format(Locale.US, "%.6g", location.getLatitude());
            String longitude = String.format(Locale.US, "%.6g", location.getLongitude());
            LOG.info("got location. Lat: " + latitude + " Lng: " + longitude);
            GB.toast(requireContext(), getString(R.string.toast_aqurired_networklocation), 2000, 0);
            GBApplication.getPrefs().getPreferences()
                    .edit()
                    .putString("location_latitude", latitude)
                    .putString("location_longitude", longitude)
                    .apply();
        }

        /**
         * Signal running activities that the theme has changed
         */
        private void sendThemeChangeIntent() {
            Intent intent = new Intent();
            intent.setAction(GBApplication.ACTION_THEME_CHANGE);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        }
    }
}
