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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.network.InternetHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.internethelper.aidl.ftp.FtpEntry;
import nodomain.freeyourgadget.internethelper.aidl.ftp.IFtpCallback;
import nodomain.freeyourgadget.internethelper.aidl.ftp.IFtpService;

public class ZeppOsMusicFilesCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMusicFilesCoordinator.class);

    private final Context mContext;

    private final Map<Uri, String> filesToUpload = new HashMap<>();

    private final InternetHelper internetHelper;

    private IFtpService iFtpService;
    private String ftpClient;
    boolean ftpReady = false;

    private final ServiceConnection mFtpConnection = new ServiceConnection() {
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            LOG.info("onServiceConnected: {}", className);

            iFtpService = IFtpService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(final ComponentName className) {
            LOG.error("Service has unexpectedly disconnected: {}", className);
            iFtpService = null;
        }
    };

    final IFtpCallback.Stub ftpCallback = new IFtpCallback.Stub() {
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

    public ZeppOsMusicFilesCoordinator(final Context context) {
        this.mContext = context;
        this.internetHelper = new InternetHelper(mContext);
    }

    public void initFtp() {
        final Intent intent = new Intent("nodomain.freeyourgadget.internethelper.FtpService");
        intent.setPackage(internetHelper.getPackageName());
        boolean res = mContext.bindService(intent, mFtpConnection, Context.BIND_AUTO_CREATE);
        if (res) {
            LOG.info("Bound to FtpService");
        } else {
            LOG.warn("Could not bind to FtpService");
        }
    }

    public void destroy() {
        mContext.unbindService(mFtpConnection);
    }

    public void uploadFiles(final List<Uri> uriList) {
        if (iFtpService == null || ftpClient == null) {
            LOG.error("No ftp client");
            return;
        }

        final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        for (final Uri uri : uriList) {
            mediaMetadataRetriever.setDataSource(mContext, uri);

            final String title = StringUtils.ensureNotNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            final String artist = StringUtils.ensureNotNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            final String album = StringUtils.ensureNotNull(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));

            long fileSize;
            try (AssetFileDescriptor fileDescriptor = mContext.getContentResolver().openAssetFileDescriptor(uri, "r")) {
                if (fileDescriptor == null) {
                    throw new IOException("Failed to get file descriptor for " + uri);
                }

                fileSize = fileDescriptor.getLength();
            } catch (final IOException e) {
                LOG.error("Failed to get file size for {}", uri, e);
                continue;
            }

            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("title", title);
                jsonObject.put("album", album);
                jsonObject.put("artist", artist);
                jsonObject.put("size", fileSize);
            } catch (final JSONException e) {
                LOG.error("Failed to build json object - this should never happen...", e);
                continue;
            }

            final String md5;
            try (InputStream inputStream = mContext.getContentResolver().openInputStream(uri)) {
                md5 = FileUtils.md5sum(inputStream);
            } catch (final IOException e) {
                LOG.error("Failed to compute md5 for {}", uri, e);
                continue;
            }

            mContext.grantUriPermission(internetHelper.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            LOG.debug("Info for {}: md5={} jsonObj={}", uri, md5, jsonObject);

            final String targetName = md5 + "." + FileUtils.getExtension(uri.toString());

            filesToUpload.put(uri, "/" + targetName);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaMetadataRetriever.close();
        }

        uploadNextFile();
    }

    public void uploadNextFile() {
        //try {
        //    iFtpService.upload(ftpClient, uri.toString(), "/cenas.mp3");
        //} catch (RemoteException e) {
        //    LOG.error("oops", e);
        //}
    }
}
