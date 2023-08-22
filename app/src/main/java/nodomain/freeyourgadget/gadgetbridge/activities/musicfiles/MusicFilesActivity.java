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

import static nodomain.freeyourgadget.gadgetbridge.util.GB.toast;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.internethelper.aidl.ftp.FtpEntry;
import nodomain.freeyourgadget.internethelper.aidl.ftp.IFtpService;
import nodomain.freeyourgadget.internethelper.aidl.ftp.IFtpCallback;

public class MusicFilesActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(MusicFilesActivity.class);

    private GBDevice gbDevice;

    IFtpService iFtpService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        setContentView(R.layout.activity_music_files);

        final ActivityResultLauncher<String> activityResultLauncher = this.registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                urilist -> {
                    final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

                    LOG.info("Got {}", urilist);

                    for (final Uri uri : urilist) {
                        mediaMetadataRetriever.setDataSource(MusicFilesActivity.this, uri);

                        final String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        final String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        final String album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                        long fileSize;
                        try (AssetFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(uri , "r")){
                            fileSize = fileDescriptor.getLength();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        final JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("title", title);
                            jsonObject.put("album", album);
                            jsonObject.put("artist", artist);
                            jsonObject.put("size", fileSize);
                        } catch (final JSONException e) {
                            throw new RuntimeException(e);
                        }

                        final String md5;
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            md5 = FileUtils.md5sum(inputStream);
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }

                        grantUriPermission("nodomain.freeyourgadget.internethelper", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        LOG.info("{}: {} {}", uri, md5, jsonObject);

                        try {
                            iFtpService.upload(ftpClient, uri.toString(), "/cenas.mp3");
                        } catch (RemoteException e) {
                            LOG.error("oops", e);
                        }

                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mediaMetadataRetriever.close();
                    }
                }
        );

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            try {
                if (initFtp()) {
                    activityResultLauncher.launch("audio/*");
                }
            } catch (RemoteException e) {
                LOG.error("Failure", e);
            }
        });
    }

    String ftpClient;
    boolean ftpReady = false;
    final IFtpCallback.Stub cb = new IFtpCallback.Stub() {
        @Override
        public void onConnect(boolean success, String msg) throws RemoteException {
            LOG.info("onConnect {} {}", success, msg);
            if (success) {
                iFtpService.login(ftpClient, "gadgetbridge", "cenas123");
            }
        }

        @Override
        public void onLogin(boolean success, String msg) throws RemoteException {
            LOG.info("onLogin {} {}", success, msg);
            ftpReady = success;
        }

        @Override
        public void onList(String path, List<FtpEntry> entries) throws RemoteException {
            LOG.info("onList {}", path, entries);
        }

        @Override
        public void onUpload(String path, boolean success, String msg) throws RemoteException {
            LOG.info("onUpload");
        }

        @Override
        public void onDownload(String path, boolean success, String msg) throws RemoteException {
            LOG.info("onDownload");
        }
    };

    private boolean initFtp() throws RemoteException {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), "nodomain.freeyourgadget.internethelper.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            LOG.error("No permission to access internet!");
            toast(this, "internet permission missing", Toast.LENGTH_SHORT, GB.ERROR);
            ActivityCompat.requestPermissions(this, new String[]{"nodomain.freeyourgadget.internethelper.INTERNET"}, 0);
            return false;
        }

        if (iFtpService == null) {
            LOG.info("connecting");
            ServiceConnection mConnection = new ServiceConnection() {
                // Called when the connection with the service is established.
                public void onServiceConnected(ComponentName className, IBinder service) {
                    LOG.info("onServiceConnected");

                    // Following the preceding example for an AIDL interface,
                    // this gets an instance of the IRemoteInterface, which we can use to call on the service.
                    iFtpService = IFtpService.Stub.asInterface(service);
                }

                // Called when the connection with the service disconnects unexpectedly.
                public void onServiceDisconnected(ComponentName className) {
                    LOG.error("Service has unexpectedly disconnected");
                    iFtpService = null;
                }
            };
            Intent intent = new Intent("nodomain.freeyourgadget.internethelper.FtpService");
            intent.setPackage("nodomain.freeyourgadget.internethelper");
            boolean res = this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (res) {
                LOG.info("Bound to NetworkService");
            } else {
                LOG.warn("Could not bind to NetworkService");
            }

            return false;
        } else if (!ftpReady) {
            final int version = iFtpService.version();
            LOG.info("version = {}", version);
            ftpClient = iFtpService.createClient(cb);
            LOG.info("client = {}", ftpClient);
            iFtpService.connect(ftpClient, "10.0.1.49", 8710);
        }

        return ftpReady;
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
