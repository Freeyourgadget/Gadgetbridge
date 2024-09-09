/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class ZipBackupImportJob extends AbstractZipBackupJob {
    private static final Logger LOG = LoggerFactory.getLogger(ZipBackupImportJob.class);

    private final Uri mUri;
    private final byte[] copyBuffer = new byte[8192];

    public ZipBackupImportJob(final Context context, final ZipBackupCallback callback, final Uri uri) {
        super(context, callback);
        this.mUri = uri;
    }

    @Override
    public void run() {
        try {
            updateProgress(0, R.string.backup_restore_importing_loading);

            // Load zip to temporary file so we can seek
            LOG.debug("Getting zip file from {}", mUri);
            final ZipFile zipFile = getZipFromUri(getContext(), mUri);

            if (isAborted()) return;

            // Validate file
            updateProgress(10, R.string.backup_restore_importing_validating);
            validateBackupFile(zipFile);

            LOG.debug("Valid zip file: {}", mUri);

            if (isAborted()) return;

            final List<ZipEntry> externalFiles = new ArrayList<>();
            final List<ZipEntry> devicePreferences = new ArrayList<>();

            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && !isAborted()) {
                final ZipEntry zipEntry = entries.nextElement();

                if (zipEntry.getName().startsWith(EXTERNAL_FILES_FOLDER + "/")) {
                    if (zipEntry.getName().endsWith(".log") || zipEntry.getName().endsWith(".log.zip")) {
                        continue;
                    }

                    externalFiles.add(zipEntry);
                } else if (zipEntry.getName().startsWith("preferences/device_")) {
                    devicePreferences.add(zipEntry);
                }
            }

            LOG.debug("Got {} external files, {} device preferences", externalFiles.size(), devicePreferences.size());

            // Restore external files
            final File externalFilesDir = FileUtils.getExternalFilesDir();
            final List<String> failedFiles = new ArrayList<>();

            for (int i = 0; i < externalFiles.size() && !isAborted(); i++) {
                final ZipEntry externalFile = externalFiles.get(i);
                final File targetExternalFile = new File(externalFilesDir, externalFile.getName().replaceFirst(EXTERNAL_FILES_FOLDER + "/", ""));
                final File parentFile = targetExternalFile.getParentFile();
                if (parentFile == null) {
                    LOG.warn("Parent file for {} is null", targetExternalFile);
                } else {
                    if (!parentFile.exists()) {
                        if (!parentFile.mkdirs()) {
                            LOG.warn("Failed to create parent dirs for {}", targetExternalFile);
                        }
                    }
                }

                try (InputStream inputStream = zipFile.getInputStream(externalFile);
                     FileOutputStream fout = new FileOutputStream(targetExternalFile)) {
                    while (inputStream.available() > 0) {
                        final int bytes = inputStream.read(copyBuffer);
                        fout.write(copyBuffer, 0, bytes);
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to restore file {}", externalFile);
                    failedFiles.add(externalFile.getName());
                }

                // 10% to 75%
                final int progress = (int) (10 + 65 * (i / (float) externalFiles.size()));
                updateProgress(progress, R.string.backup_restore_importing_files_i_of_n, i + 1, externalFiles.size());
            }

            if (isAborted()) return;

            // Restore database
            LOG.debug("Importing database");
            updateProgress(75, R.string.backup_restore_importing_database);
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                final DBHelper helper = new DBHelper(getContext());
                final SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                try (InputStream databaseInputStream = zipFile.getInputStream(zipFile.getEntry(DATABASE_FILENAME))) {
                    helper.importDB(dbHandler, databaseInputStream);
                    helper.validateDB(sqLiteOpenHelper);
                }
            }

            if (isAborted()) return;

            // Restore preferences
            LOG.debug("Importing global preferences");
            updateProgress(85, R.string.backup_restore_importing_preferences);
            try (InputStream globalPrefsInputStream = zipFile.getInputStream(zipFile.getEntry(PREFS_GLOBAL_FILENAME))) {
                final SharedPreferences globalPreferences = GBApplication.getPrefs().getPreferences();

                final JsonBackupPreferences jsonBackupPreferences = JsonBackupPreferences.fromJson(globalPrefsInputStream);
                if (!jsonBackupPreferences.importInto(globalPreferences)) {
                    LOG.warn("Global preferences were not commited");
                }
            }

            if (isAborted()) return;

            // At this point we already restored the db, so we can list the devices from there
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                final List<Device> activeDevices = DBHelper.getActiveDevices(dbHandler.getDaoSession());
                for (Device dbDevice : activeDevices) {
                    LOG.debug("Importing device preferences for {}", dbDevice.getIdentifier());
                    final SharedPreferences devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                    if (devicePrefs != null && !isAborted()) {
                        final ZipEntry devicePrefsZipEntry = zipFile.getEntry(String.format(Locale.ROOT, PREFS_DEVICE_FILENAME, dbDevice.getIdentifier()));
                        if (devicePrefsZipEntry == null) {
                            continue;
                        }
                        try (InputStream devicePrefsInputStream = zipFile.getInputStream(devicePrefsZipEntry)) {
                            final JsonBackupPreferences jsonBackupPreferences = JsonBackupPreferences.fromJson(devicePrefsInputStream);
                            if (!jsonBackupPreferences.importInto(devicePrefs)) {
                                LOG.warn("Device preferences for {} were not commited", dbDevice.getIdentifier());
                            }
                        }
                    }
                }
            }

            if (isAborted()) return;

            LOG.info("Import complete");

            if (!failedFiles.isEmpty()) {
                final String failedFilesListMessage = "- " + String.join("\n- ", failedFiles);
                onSuccess(getContext().getString(R.string.backup_restore_warning_files, failedFiles.size(), failedFilesListMessage));
            } else {
                onSuccess(null);
            }
        } catch (final Exception e) {
            LOG.error("Import failed", e);
            onFailure(e.getLocalizedMessage());
        }
    }

    private ZipFile getZipFromUri(final Context context, final Uri uri) throws IOException {
        final File tmpFile = File.createTempFile("gb-backup-zip-import", "zip", context.getCacheDir());
        tmpFile.deleteOnExit();

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
            if (inputStream == null) {
                throw new IOException("Failed to get input stream");
            }

            int len;
            while ((len = inputStream.read(copyBuffer)) != -1) {
                outputStream.write(copyBuffer, 0, len);
            }
        }

        return new ZipFile(tmpFile);
    }

    private static void validateBackupFile(final ZipFile zipFile) throws IOException {
        final ZipEntry metadataEntry = zipFile.getEntry(METADATA_FILENAME);
        if (metadataEntry == null) {
            throw new IOException("Zip file has no metadata");
        }
        final InputStream inputStream = zipFile.getInputStream(metadataEntry);
        final ZipBackupMetadata zipBackupMetadata = GSON.fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                ZipBackupMetadata.class
        );

        if (zipBackupMetadata.getBackupVersion() > VERSION) {
            throw new IOException("Unsupported backup version " + zipBackupMetadata.getBackupVersion());
        }

        final ZipEntry databaseEntry = zipFile.getEntry(DATABASE_FILENAME);
        if (databaseEntry == null) {
            throw new IOException("Zip file has no database");
        }
    }
}
