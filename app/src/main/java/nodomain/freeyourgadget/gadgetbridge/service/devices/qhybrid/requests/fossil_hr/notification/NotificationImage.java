/*  Copyright (C) 2019-2021 Daniel Dakhno, Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image.ImageConverter;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder.RLEEncoder.RLEEncode;

public class NotificationImage extends AssetFile {
    public static final int MAX_ICON_WIDTH = 24;
    public static final int MAX_ICON_HEIGHT = 24;
    private int width;
    private int height;

    public NotificationImage(String fileName, byte[] imageData, int width, int height) {
        super(fileName, imageData);
        this.width = width;
        this.height = height;
    }

    public NotificationImage(String fileName, Bitmap iconBitmap) {
        super(fileName, RLEEncode(ImageConverter.get2BitsRLEImageBytes(BitmapUtil.scaleWithMax(iconBitmap, MAX_ICON_WIDTH, MAX_ICON_HEIGHT))));
        this.width = Math.min(iconBitmap.getWidth(), MAX_ICON_WIDTH);
        this.height = Math.min(iconBitmap.getHeight(), MAX_ICON_HEIGHT);
    }

    public byte[] getImageData() { return getFileData(); }
    public String getFileName() { return super.getFileName(); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public static byte[] getEncodedIconFromDrawable(Drawable drawable) {
        Bitmap iconBitmap = BitmapUtil.scaleWithMax(BitmapUtil.convertDrawableToBitmap(drawable), MAX_ICON_WIDTH, MAX_ICON_HEIGHT);
        return RLEEncode(ImageConverter.get2BitsRLEImageBytes(iconBitmap));
    }
}
