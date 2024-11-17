package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps.GarminAgpsStatus;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class GarminSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(GarminSettingsCustomizer.class);

    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final Preference realtimeSettings = handler.findPreference(GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS);
        if (realtimeSettings != null) {
            realtimeSettings.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(handler.getContext(), GarminRealtimeSettingsActivity.class);
                intent.putExtra(GBDevice.EXTRA_DEVICE, handler.getDevice());
                handler.getContext().startActivity(intent);
                return true;
            });
        }

        final PreferenceCategory prefAgpsHeader = handler.findPreference(DeviceSettingsPreferenceConst.PREF_HEADER_AGPS);
        if (prefAgpsHeader != null) {
            final List<String> urls = prefs.getList(GarminPreferences.PREF_AGPS_KNOWN_URLS, Collections.emptyList(), "\n");
            if (urls.isEmpty()) {
                return;
            }

            final String currentFolder = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");

            final Preference prefFolder = handler.findPreference(GarminPreferences.PREF_GARMIN_AGPS_FOLDER);
            final ActivityResultLauncher<Uri> agpsFolderChooser = handler.registerForActivityResult(
                    new ActivityResultContracts.OpenDocumentTree(),
                    localUri -> {
                        LOG.info("Garmin agps folder: {}", localUri);
                        if (localUri != null) {
                            handler.getContext().getContentResolver().takePersistableUriPermission(localUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            prefs.getPreferences().edit()
                                    .putString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, localUri.toString())
                                    .apply();
                            prefFolder.setSummary(localUri.toString());

                            for (final String url : urls) {
                                updateAgpsStatus(handler, prefs, url);
                            }
                        }
                    }
            );
            prefFolder.setOnPreferenceClickListener(preference -> {
                agpsFolderChooser.launch(null);
                return true;
            });
            prefFolder.setSummary(currentFolder);
            prefAgpsHeader.addPreference(prefFolder);

            int i = 0;
            for (final String url : urls) {
                i++;

                final Preference prefHeader = new PreferenceCategory(handler.getContext());
                prefHeader.setKey("pref_agps_url_header_" + i);
                prefHeader.setIconSpaceReserved(false);
                prefHeader.setTitle(handler.getContext().getString(R.string.garmin_agps_url_i, i));
                prefAgpsHeader.addPreference(prefHeader);

                final Preference prefUrl = new Preference(handler.getContext());
                prefUrl.setOnPreferenceClickListener(preference -> {
                    final ClipboardManager clipboard = (ClipboardManager) handler.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    final ClipData clip = ClipData.newPlainText(handler.getContext().getString(R.string.url), url);
                    clipboard.setPrimaryClip(clip);
                    toast(handler.getContext(), handler.getContext().getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT, GB.INFO);
                    return true;
                });
                prefUrl.setKey("pref_garmin_agps_url_" + i);
                prefUrl.setIcon(R.drawable.ic_link);
                prefUrl.setTitle(R.string.url);
                prefUrl.setSummary(url);
                prefAgpsHeader.addPreference(prefUrl);

                final Preference prefLocalFile = new Preference(handler.getContext());
                prefLocalFile.setOnPreferenceClickListener(preference -> {
                    selectAgpsFile(handler, prefs, url, prefLocalFile);
                    return true;
                });
                prefLocalFile.setKey(GarminPreferences.agpsFilename(url));
                prefLocalFile.setIcon(R.drawable.ic_file_open);
                prefLocalFile.setTitle(R.string.garmin_agps_local_file);
                prefLocalFile.setSummary(prefs.getString(GarminPreferences.agpsFilename(url), ""));
                prefAgpsHeader.addPreference(prefLocalFile);

                final Preference prefStatus = new Preference(handler.getContext());
                prefStatus.setKey(GarminPreferences.agpsStatus(url));
                prefStatus.setIcon(R.drawable.ic_health);
                prefStatus.setTitle(R.string.status);
                prefAgpsHeader.addPreference(prefStatus);
                updateAgpsStatus(handler, prefs, url);

                final Preference prefUpdateTime = new Preference(handler.getContext());
                prefUpdateTime.setKey(GarminPreferences.agpsUpdateTime(url));
                prefUpdateTime.setIcon(R.drawable.ic_calendar_today);
                prefUpdateTime.setTitle(R.string.pref_agps_update_time);
                final long ts = prefs.getLong(GarminPreferences.agpsUpdateTime(url), 0L);
                if (ts > 0) {
                    prefUpdateTime.setSummary(String.format("%s (%s)",
                            SDF.format(new Date(ts)),
                            DateTimeUtils.formatDurationHoursMinutes(System.currentTimeMillis() - ts, TimeUnit.MILLISECONDS)
                    ));
                } else {
                    prefUpdateTime.setSummary(handler.getContext().getString(R.string.unknown));
                }
                prefAgpsHeader.addPreference(prefUpdateTime);
            }
        }
    }

    private void selectAgpsFile(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String url, final Preference prefLocalFile) {
        final String currentFolder = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");

        final String folderUri = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            GB.toast(handler.getContext().getString(R.string.no_folder_selected), Toast.LENGTH_SHORT, GB.INFO);
            return;
        }

        final DocumentFile folder = DocumentFile.fromTreeUri(handler.getContext(), Uri.parse(currentFolder));
        if (folder == null || folder.listFiles().length == 0) {
            GB.toast(handler.getContext().getString(R.string.folder_is_empty), Toast.LENGTH_SHORT, GB.INFO);
            return;
        }

        final DocumentFile[] documentFiles = folder.listFiles();
        final String[] files = new String[documentFiles.length + 1];
        files[0] = handler.getContext().getString(R.string.none);
        final String selectedFile = prefs.getString(GarminPreferences.agpsFilename(url), "");
        int checkedItem = 0;
        for (int j = 0; j < documentFiles.length; j++) {
            files[j + 1] = documentFiles[j].getName();
            if (selectedFile.equals(files[j + 1])) {
                checkedItem = j + 1;
            }
        }

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(handler.getContext());
        builder.setTitle(R.string.garmin_agps_local_file);

        final AtomicInteger selectedIdx = new AtomicInteger(0);
        builder.setSingleChoiceItems(files, checkedItem, (dialog, which) -> {
            selectedIdx.set(which);
        });
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            final String selectedFilename = selectedIdx.get() > 0 ? files[selectedIdx.get()] : null;
            prefs.getPreferences().edit()
                    .putString(GarminPreferences.agpsFilename(url), selectedFilename)
                    .apply();
            prefLocalFile.setSummary(selectedFilename);
            updateAgpsStatus(handler, prefs, url);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void updateAgpsStatus(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String url) {
        final Preference prefStatus = handler.findPreference(GarminPreferences.agpsStatus(url));

        final String filename = prefs.getString(GarminPreferences.agpsFilename(url), "");
        if (filename.isEmpty()) {
            prefStatus.setSummary("");
            return;
        }
        final String folderUri = prefs.getString(GarminPreferences.PREF_GARMIN_AGPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            prefStatus.setSummary("");
            return;
        }
        final DocumentFile folder = DocumentFile.fromTreeUri(handler.getContext(), Uri.parse(folderUri));
        if (folder == null) {
            prefStatus.setSummary("");
            return;
        }
        final GarminAgpsStatus agpsStatus;
        final DocumentFile localFile = folder.findFile(filename);
        if (localFile != null && localFile.isFile() && localFile.canRead()) {
            if (localFile.lastModified() < prefs.getLong(GarminPreferences.agpsUpdateTime(url), 0L)) {
                agpsStatus = GarminAgpsStatus.CURRENT;
            } else {
                agpsStatus = GarminAgpsStatus.PENDING;
            }
        } else {
            agpsStatus = GarminAgpsStatus.MISSING;
        }
        prefStatus.setSummary(handler.getContext().getString(agpsStatus.getText()));
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    public static final Creator<GarminSettingsCustomizer> CREATOR = new Creator<GarminSettingsCustomizer>() {
        @Override
        public GarminSettingsCustomizer createFromParcel(final Parcel in) {
            return new GarminSettingsCustomizer();
        }

        @Override
        public GarminSettingsCustomizer[] newArray(final int size) {
            return new GarminSettingsCustomizer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}
