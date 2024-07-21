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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    Bitmap watchfacePreviewBitmap;
    HuaweiWatchfaceManager.WatchfaceDescription watchfaceDescription;
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
        if (parseAsWatchFace()) {
            assert watchfaceDescription.screen != null;
            assert watchfaceDescription.title != null;
            fileType = FileUpload.Filetype.watchface;
        }
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
                watchfacePreviewBitmap = BitmapFactory.decodeByteArray(preview, 0, preview.length);
            }

            byte[] watchfaceZip = watchfacePackage.getFileFromZip("com.huawei.watchface");
            try {
                GBZipFile watchfaceBinZip  = new GBZipFile(watchfaceZip);
                fw = watchfaceBinZip.getFileFromZip("watchface.bin");
            } catch (ZipFileException e) {
                LOG.error("Unable to get watchfaceZip,  it seems older already watchface.bin");
                fw = watchfaceZip;
            }
            fileSize = fw.length;
            isWatchface = true;

        } catch (ZipFileException e) {
            LOG.error("Unable to read watchface file.", e);
        } catch (FileNotFoundException e) {
            LOG.error("The watchface file was not found.", e);
        } catch (IOException e) {
            LOG.error("General IO error occurred.", e);
        } catch (Exception e) {
            LOG.error("Unknown error occurred.", e);
        }

        return isWatchface;
    }

    public boolean isWatchface() {
        return fileType == FileUpload.Filetype.watchface;
    }
    public boolean isValid() {
        return isWatchface();
    }

    public Bitmap getWatchfacePreviewBitmap() {
        return watchfacePreviewBitmap;
    }

    public HuaweiWatchfaceManager.WatchfaceDescription getWatchfaceDescription() {
        return watchfaceDescription;
    }

    public byte getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }


}
