/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.musicfiles;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class MusicFilesActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(MusicFilesActivity.class);

    public static final String ACTION_FILES_LIST = "nodomain.freeyourgadget.gadgetbridge.activities.musicfiles.action_files_list";
    public static final String ACTION_STARTED = "nodomain.freeyourgadget.gadgetbridge.activities.musicfiles.action_started";
    public static final String ACTION_STATUS = "nodomain.freeyourgadget.gadgetbridge.activities.musicfiles.action_status";
    public static final String ACTION_STOPPED = "nodomain.freeyourgadget.gadgetbridge.activities.musicfiles.action_stopped";

    private ProgressDialog activityFullSyncDialog;

    private GBDevice gbDevice;

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_FILES_LIST:
                    return;
                case ACTION_STARTED:
                    return;
                case ACTION_STATUS:
                    return;
                case ACTION_STOPPED:
                    finish();
                    return;
                default:
                    LOG.warn("Unhandled intent action {}", intent.getAction());
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        setContentView(R.layout.activity_music_files);

        if (!gbDevice.isInitialized()) {
            new MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle("Device not initialized")
                    .setMessage("Please connect to the device")
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        finish();
                    })
                    .show();
            return;
        }

        final IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_FILES_LIST);
        filterLocal.addAction(ACTION_STARTED);
        filterLocal.addAction(ACTION_STATUS);
        filterLocal.addAction(ACTION_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        GBApplication.deviceService(gbDevice).onMusicFilesStart();

        final ActivityResultLauncher<String> activityResultLauncher = this.registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> GBApplication.deviceService(gbDevice).onMusicFilesUpload(new ArrayList<>(uris))
        );

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> activityResultLauncher.launch("audio/*"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GBApplication.deviceService(gbDevice).onMusicFilesStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
