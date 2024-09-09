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
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class ZipBackupExportJob extends AbstractZipBackupJob {
    private static final Logger LOG = LoggerFactory.getLogger(ZipBackupExportJob.class);

    private final Uri mUri;

    private final byte[] copyBuffer = new byte[8192];

    public ZipBackupExportJob(final Context context, final ZipBackupCallback callback, final Uri uri) {
        super(context, callback);
        this.mUri = uri;
    }

    @Override
    public void run() {
        try (final OutputStream outputStream = getContext().getContentResolver().openOutputStream(mUri);
             final ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

            if (isAborted()) return;

            // Preferences
            updateProgress(0, R.string.backup_restore_exporting_preferences);
            exportPreferences(zipOut);

            if (isAborted()) return;

            // Database
            updateProgress(10, R.string.backup_restore_exporting_database);
            exportDatabase(zipOut, getContext());

            if (isAborted()) return;

            // External files
            updateProgress(25, R.string.backup_restore_exporting_files);

            final File externalFilesDir = FileUtils.getExternalFilesDir();
            LOG.debug("Exporting external files from {}", externalFilesDir);

            final List<String> allExternalFiles = getAllRelativeFiles(externalFilesDir);
            LOG.debug("Got {} files to export", allExternalFiles.size());

            for (int i = 0; i < allExternalFiles.size() && !isAborted(); i++) {
                final String child = allExternalFiles.get(i);
                exportSingleExternalFile(zipOut, externalFilesDir, child);

                final int progress = (int) Math.min(99, 50 + 49 * (i / (float) allExternalFiles.size()));
                updateProgress(progress, R.string.backup_restore_exporting_files_i_of_n, i + 1, allExternalFiles.size());
            }

            // Metadata
            updateProgress(99, R.string.backup_restore_exporting_finishing);

            if (isAborted()) return;

            addMetadata(zipOut);

            zipOut.finish();
            zipOut.flush();

            if (isAborted()) return;

            LOG.info("Export complete");

            onSuccess(null);
        } catch (final Exception e) {
            LOG.error("Export failed", e);

            if (!isAborted()) {
                onFailure(e.getLocalizedMessage());
            }
        }
    }

    private static void exportPreferences(final ZipOutputStream zipOut) throws IOException {
        LOG.debug("Exporting global preferences");

        final SharedPreferences globalPreferences = GBApplication.getPrefs().getPreferences();
        exportPreferences(zipOut, globalPreferences, PREFS_GLOBAL_FILENAME);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final List<Device> activeDevices = DBHelper.getActiveDevices(dbHandler.getDaoSession());
            for (Device dbDevice : activeDevices) {
                LOG.debug("Exporting device preferences for {}", dbDevice.getIdentifier());
                final SharedPreferences devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(dbDevice.getIdentifier());
                if (devicePrefs != null) {
                    exportPreferences(zipOut, devicePrefs, String.format(Locale.ROOT, PREFS_DEVICE_FILENAME, dbDevice.getIdentifier()));
                }
            }
        } catch (final Exception e) {
            throw new IOException("Failed to export device preferences", e);
        }
    }

    private static void exportPreferences(final ZipOutputStream zipOut,
                                          final SharedPreferences sharedPreferences,
                                          final String zipEntryName) throws IOException {
        LOG.debug("Exporting preferences to {}", zipEntryName);

        final JsonBackupPreferences jsonBackupPreferences = JsonBackupPreferences.exportFrom(sharedPreferences);
        final String preferencesJson = jsonBackupPreferences.toJson();

        final ZipEntry zipEntry = new ZipEntry(zipEntryName);
        zipOut.putNextEntry(zipEntry);
        zipOut.write(preferencesJson.getBytes(StandardCharsets.UTF_8));
    }

    private static void exportDatabase(final ZipOutputStream zipOut, final Context context) throws IOException {
        LOG.debug("Exporting database");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DBHelper helper = new DBHelper(context);
            helper.exportDB(dbHandler, baos);
        } catch (final Exception e) {
            throw new IOException("Failed to export database", e);
        }

        final ZipEntry zipEntry = new ZipEntry(DATABASE_FILENAME);
        zipOut.putNextEntry(zipEntry);
        zipOut.write(baos.toByteArray());
    }

    /**
     * Gets a list of the relative path of all files from a directory, recursively.
     */
    private static List<String> getAllRelativeFiles(final File dir) {
        final List<String> ret = new ArrayList<>();

        final String[] childEntries = dir.list();
        if (childEntries == null) {
            LOG.warn("Files in external dir are null");
            return ret;
        }

        for (final String child : childEntries) {
            getAllRelativeFilesAux(ret, dir, child);
        }

        return ret;
    }

    private static void getAllRelativeFilesAux(final List<String> currentList,
                                               final File externalFilesDir,
                                               final String relativePath) {
        final File file = new File(externalFilesDir, relativePath);
        if (file.isDirectory()) {
            final String[] childEntries = file.list();
            if (childEntries == null) {
                LOG.warn("Files in {} are null", file);
                return;
            }

            for (final String child : childEntries) {
                getAllRelativeFilesAux(currentList, externalFilesDir, relativePath + "/" + child);
            }
        } else if (file.isFile()) {
            currentList.add(relativePath);
        } else {
            // Should never happen?
            LOG.error("Unknown file type for {}", file);
        }
    }

    private void exportSingleExternalFile(final ZipOutputStream zipOut,
                                          final File externalFilesDir,
                                          final String relativePath) throws IOException {
        final File file = new File(externalFilesDir, relativePath);
        if (!file.isFile()) {
            throw new IOException("Not a file: " + file);
        }

        LOG.trace("Exporting file: {}", relativePath);

        final ZipEntry zipEntry = new ZipEntry(EXTERNAL_FILES_FOLDER + "/" + relativePath);
        zipOut.putNextEntry(zipEntry);

        try (final InputStream in = new FileInputStream(new File(externalFilesDir, relativePath))) {
            int read;
            while ((read = in.read(copyBuffer)) > 0) {
                zipOut.write(copyBuffer, 0, read);
            }
        } catch (final Exception e) {
            throw new IOException("Failed to write " + relativePath, e);
        }
    }

    private static void addMetadata(final ZipOutputStream zipOut) throws IOException {
        LOG.debug("Adding metadata");

        final ZipBackupMetadata metadata = new ZipBackupMetadata(
                BuildConfig.APPLICATION_ID,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                VERSION,
                new Date()
        );
        final String metadataJson = GSON.toJson(metadata);

        final ZipEntry zipEntry = new ZipEntry(METADATA_FILENAME);
        zipOut.putNextEntry(zipEntry);
        zipOut.write(metadataJson.getBytes(StandardCharsets.UTF_8));
    }
}
