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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.backup.AbstractZipBackupJob;
import nodomain.freeyourgadget.gadgetbridge.util.backup.ZipBackupCallback;
import nodomain.freeyourgadget.gadgetbridge.util.backup.ZipBackupExportJob;
import nodomain.freeyourgadget.gadgetbridge.util.backup.ZipBackupImportJob;

public class BackupRestoreProgressActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(BackupRestoreProgressActivity.class);

    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_ACTION = "action"; // import/export

    private boolean jobFinished = false;
    private Uri uri;
    private String action;
    private Thread mThread;
    private AbstractZipBackupJob mZipBackupJob;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore_progress);

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            LOG.error("No extras");
            finish();
            return;
        }

        uri = extras.getParcelable(EXTRA_URI);
        if (uri == null) {
            LOG.error("No uri");
            finish();
            return;
        }

        action = extras.getString(EXTRA_ACTION);
        if (action == null) {
            LOG.error("No action");
            finish();
            return;
        }

        final TextView backupRestoreHint = findViewById(R.id.backupRestoreHint);
        final ProgressBar backupRestoreProgressBar = findViewById(R.id.backupRestoreProgressBar);
        final TextView backupRestoreProgressText = findViewById(R.id.backupRestoreProgressText);
        final TextView backupRestoreProgressPercentage = findViewById(R.id.backupRestoreProgressPercentage);

        final ZipBackupCallback zipBackupCallback = new ZipBackupCallback() {
            @Override
            public void onProgress(final int progress, final String message) {
                backupRestoreProgressBar.setIndeterminate(progress == 0);
                backupRestoreProgressBar.setProgress(progress);
                backupRestoreProgressText.setText(message);
                backupRestoreProgressPercentage.setText(getString(R.string.battery_percentage_str, String.valueOf(progress)));
            }

            @Override
            public void onSuccess(final String warnings) {
                jobFinished = true;
                backupRestoreHint.setVisibility(View.GONE);
                backupRestoreProgressBar.setProgress(100);
                backupRestoreProgressPercentage.setText(getString(R.string.battery_percentage_str, "100"));

                switch (action) {
                    case "import":
                        backupRestoreProgressText.setText(R.string.backup_restore_import_complete);

                        final StringBuilder message = new StringBuilder();

                        message.append(getString(R.string.backup_restore_restart_summary, getString(R.string.app_name)));

                        if (warnings != null) {
                            message.append("\n\n").append(warnings);
                        }

                        new MaterialAlertDialogBuilder(BackupRestoreProgressActivity.this)
                                .setCancelable(false)
                                .setIcon(R.drawable.ic_sync)
                                .setTitle(R.string.backup_restore_restart_title)
                                .setMessage(message.toString())
                                .setOnCancelListener((dialog -> {
                                    GBApplication.restart();
                                }))
                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                    GBApplication.restart();
                                }).show();
                        break;
                    case "export":
                        backupRestoreProgressText.setText(R.string.backup_restore_export_complete);
                        break;
                }
            }

            @Override
            public void onFailure(@Nullable final String errorMessage) {
                jobFinished = true;

                switch (action) {
                    case "import":
                        backupRestoreHint.setText(R.string.backup_restore_error_import);
                        break;
                    case "export":
                        backupRestoreHint.setText(R.string.backup_restore_error_export);
                        break;
                }

                backupRestoreProgressText.setText(errorMessage);
                backupRestoreProgressPercentage.setVisibility(View.GONE);

                if ("export".equals(action)) {
                    final DocumentFile documentFile = DocumentFile.fromSingleUri(BackupRestoreProgressActivity.this, uri);
                    if (documentFile != null) {
                        documentFile.delete();
                    }
                }
            }
        };

        switch (action) {
            case "import":
                backupRestoreHint.setText(getString(R.string.backup_restore_do_not_exit, getString(R.string.backup_restore_importing)));
                mZipBackupJob = new ZipBackupImportJob(GBApplication.getContext(), zipBackupCallback, uri);
                break;
            case "export":
                backupRestoreHint.setText(getString(R.string.backup_restore_do_not_exit, getString(R.string.backup_restore_exporting)));
                mZipBackupJob = new ZipBackupExportJob(GBApplication.getContext(), zipBackupCallback, uri);
                break;
            default:
                LOG.error("Unknown action {}", action);
                finish();
                return;
        }

        mThread = new Thread(mZipBackupJob, "gb-backup-restore");
        mThread.start();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            confirmExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmExit() {
        if (jobFinished) {
            finish();
            return;
        }

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.backup_restore_abort_title)
                .setPositiveButton(R.string.backup_restore_abort_title, (dialog, which) -> {
                    if (mZipBackupJob != null) {
                        LOG.info("Aborting {}", action);
                        final Handler handler = new Handler(getMainLooper());
                        mZipBackupJob.abort();
                        new Thread(() -> {
                            try {
                                mThread.join(60_000);
                            } catch (final InterruptedException ignored) {
                            }
                            handler.post(() -> {
                                LOG.info("Aborted {}", action);
                                if ("export".equals(action)) {
                                    // Delete the incomplete export file
                                    final DocumentFile documentFile = DocumentFile.fromSingleUri(BackupRestoreProgressActivity.this, uri);
                                    if (documentFile != null) {
                                        documentFile.delete();
                                    }
                                }
                                finish();
                            });
                        }).start();
                    }
                })
                .setNegativeButton(R.string.Cancel, (dialog, which) -> {
                });

        if ("import".equals(action)) {
            builder.setMessage(R.string.backup_restore_abort_import_confirmation);
        } else {
            builder.setMessage(R.string.backup_restore_abort_export_confirmation);
        }

        builder.show();
    }
}
