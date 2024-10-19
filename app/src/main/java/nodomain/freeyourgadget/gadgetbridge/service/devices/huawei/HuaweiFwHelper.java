/*  Copyright (C) 2024 Vitalii Tomin

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import  	android.media.MediaFormat;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiBinAppParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileUpload;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class HuaweiFwHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiFwHelper.class);

    private final Uri uri;

    private byte[] fw;
    private int fileSize = 0;
    private byte fileType = 0;
    String fileName = "";

    Bitmap previewBitmap;
    HuaweiWatchfaceManager.WatchfaceDescription watchfaceDescription;
    HuaweiAppManager.AppConfig appConfig;
    HuaweiMusicManager.AudioInfo musicInfo;
    Context mContext;



    public HuaweiFwHelper(final Uri uri, final Context context) {

        this.uri = uri;
        final UriHelper uriHelper;
        this.mContext = context;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper for {}", uri, e);
            return;
        }

        parseFile();
    }

    private void parseFile() {
        if (parseAsMusic()) {
            fileType = FileUpload.Filetype.music;
        } else if (parseAsApp()) {
            assert appConfig.bundleName != null;
            fileType = FileUpload.Filetype.app;
        } else if (parseAsWatchFace()) {
            assert watchfaceDescription.screen != null;
            assert watchfaceDescription.title != null;
            fileType = FileUpload.Filetype.watchface;
        }
    }

    private String getNameWithoutExtension(String fileName) {
        return fileName.indexOf(".") > 0?fileName.substring(0, fileName.lastIndexOf(".")): fileName;
    }

    private String getExtension(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf >= 0 && lastIndexOf + 1 < fileName.length()) {
            return fileName.substring(lastIndexOf + 1);
        }
        return null;
    }

    private HuaweiMusicManager.AudioInfo getAudioInfo(Uri selectedUri) throws IOException {
        ContentResolver contentResolver = mContext.getContentResolver();
        String[] filePathColumn = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

        Cursor cursor = contentResolver.query(selectedUri, filePathColumn, null, null, null);
        if(cursor == null)
            return null;
        cursor.moveToFirst();

        int fileNameIndex = cursor.getColumnIndex(filePathColumn[0]);
        String fileName = cursor.getString(fileNameIndex);
        int fileSizeIndex = cursor.getColumnIndex(filePathColumn[1]);
        long fileSize = cursor.getLong(fileSizeIndex);
        cursor.close();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mContext, selectedUri);

        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if(TextUtils.isEmpty(title)) {
            title = getNameWithoutExtension(fileName);
        }
        if(TextUtils.isEmpty(artist)) {
            artist = "Unknown";
        }

        MediaExtractor mex = new MediaExtractor();
        mex.setDataSource(mContext, selectedUri, null);


        MediaFormat mf = mex.getTrackFormat(0);

        int bitrate = -1; // TODO: calculate or get bitrate
        int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channels = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        long duration = mf.getLong(MediaFormat.KEY_DURATION);

        LOG.info("bitRate: " + bitrate);
        LOG.info("sampleRate: " + sampleRate);
        LOG.info("channelCount: " + channels);
        LOG.info("duration: " + duration);

        String extension = getExtension(fileName);
        if(!TextUtils.isEmpty(extension)) {
            extension = extension.toLowerCase();
        }

        HuaweiMusicManager.AudioInfo audioInfo = new HuaweiMusicManager.AudioInfo(fileName, fileSize, title, artist, extension);
        audioInfo.setCharacteristics(duration, sampleRate, bitrate, (byte) channels);

        return audioInfo;
    }

    private byte[] getFileData(UriHelper uriHelper) throws IOException {
        InputStream inputStream = uriHelper.openInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[1000];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        inputStream.close();
        return buffer.toByteArray();
    }

    boolean parseAsMusic() {
        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            String mimeType = mContext.getContentResolver().getType(uri);
            LOG.info("File mime type: {}", mimeType);

            if(mimeType == null || !mimeType.startsWith("audio/"))
                return false;

            musicInfo = getAudioInfo(uri);
            if(musicInfo == null)
                return false;

            musicInfo.setMimeType(mimeType);

            byte[] musicData = getFileData(uriHelper);

            fileName = musicInfo.getFileName();
            fw = musicData;
            fileSize = fw.length;

            return true;
        } catch (FileNotFoundException e) {
            LOG.error("Music: File was not found {}", e.getMessage());
        } catch (IOException e) {
            LOG.error("Music: General IO error occurred {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Music: Unknown error occurred", e);
        }
        return false;
    }


    boolean parseAsApp() {

        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            byte[] appData = getFileData(uriHelper);

            HuaweiBinAppParser app = new HuaweiBinAppParser();
            app.parseData(appData);

            byte[] config = app.getEntryContent("config.json");
            if (config == null)
                return false;
            appConfig = new HuaweiAppManager.AppConfig(new String(config));
            fileName = app.getPackageName() + "_INSTALL"; //TODO: INSTALL or UPDATE suffix

            fw = appData;
            fileSize = fw.length;

            byte[] icon = app.getEntryContent("icon_small.png");
            if (icon != null) {
                previewBitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
            }

            return true;

        } catch (FileNotFoundException e) {
            LOG.error("App: File was not found{}", e.getMessage());
        } catch (IOException e) {
            LOG.error("App: General IO error occurred {}", e.getMessage());
        } catch (HuaweiBinAppParser.HuaweiBinAppParseError e) {
            LOG.error("App: Error parsing app File {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("App: Unknown error occurred", e);
        }
        return false;
    }


    public byte[] getBytes() {
        return fw;
    }

    public void unsetFwBytes() {
        this.fw = null;
    }

    boolean parseAsWatchFace() {
        boolean isWatchface = false;

        try {
            final UriHelper uriHelper = UriHelper.get(uri, this.mContext);

            GBZipFile watchfacePackage = new GBZipFile(uriHelper.openInputStream());
            byte[] bytesDescription = watchfacePackage.getFileFromZip("description.xml");

            // check if description file contents BOM
            ByteBuffer bb = ByteBuffer.wrap(bytesDescription);
            byte[] bom = new byte[3];
            // get the first 3 bytes
            bb.get(bom, 0, bom.length);
            String content = new String(GB.hexdump(bom));
            String xmlDescription = null;
            if ("efbbbf".equalsIgnoreCase(content)) {
                byte[] contentAfterFirst3Bytes = new byte[bytesDescription.length - 3];
                bb.get(contentAfterFirst3Bytes, 0, contentAfterFirst3Bytes.length);
                xmlDescription = new String(contentAfterFirst3Bytes);
            } else {
                xmlDescription = new String(bytesDescription);
            }

            watchfaceDescription = new HuaweiWatchfaceManager.WatchfaceDescription(xmlDescription);
            if (watchfacePackage.fileExists("preview/cover.jpg")) {
                final byte[] preview = watchfacePackage.getFileFromZip("preview/cover.jpg");
                previewBitmap = BitmapFactory.decodeByteArray(preview, 0, preview.length);
            }

            byte[] watchfaceZip = watchfacePackage.getFileFromZip("com.huawei.watchface");
            try {
                GBZipFile watchfaceBinZip = new GBZipFile(watchfaceZip);
                fw = watchfaceBinZip.getFileFromZip("watchface.bin");
            } catch (ZipFileException e) {
                LOG.error("Unable to get watchfaceZip,  it seems older already watchface.bin");
                fw = watchfaceZip;
            }
            fileSize = fw.length;
            isWatchface = true;

        } catch (ZipFileException e) {
            LOG.error("Watchface: Unable to read file {}", e.getMessage());
        } catch (FileNotFoundException e) {
            LOG.error("Watchface: File was not found {}", e.getMessage());
        } catch (IOException e) {
            LOG.error("Watchface: General IO error occurred {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Watchface: Unknown error occurred", e);
        }

        return isWatchface;
    }

    public boolean isWatchface() {
        return fileType == FileUpload.Filetype.watchface;
    }

    public boolean isAPP() {
        return fileType == FileUpload.Filetype.app;
    }

    public boolean isMusic() {
        return fileType == FileUpload.Filetype.music;
    }

    public boolean isValid() {
        return isWatchface() || isAPP() || isMusic();
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public HuaweiWatchfaceManager.WatchfaceDescription getWatchfaceDescription() {
        return watchfaceDescription;
    }

    public HuaweiAppManager.AppConfig getAppConfig() {
        return appConfig;
    }

    public HuaweiMusicManager.AudioInfo getMusicInfo() {
        return musicInfo;
    }

    public byte getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }


}
