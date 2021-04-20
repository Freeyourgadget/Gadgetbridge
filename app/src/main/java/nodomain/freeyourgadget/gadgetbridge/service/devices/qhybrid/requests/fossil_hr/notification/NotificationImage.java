/*  Copyright (C) 2019-2021 Daniel Dakhno

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
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder.RLEEncoder.RLEEncode;
import static nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil.convertDrawableToBitmap;

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
        super(fileName, RLEEncode(get2BitsPixelsFromBitmap(convertIcon(iconBitmap))));
        this.width = Math.min(iconBitmap.getWidth(), MAX_ICON_WIDTH);
        this.height = Math.min(iconBitmap.getHeight(), MAX_ICON_HEIGHT);
    }

    public byte[] getImageData() { return getFileData(); }
    public String getFileName() { return super.getFileName(); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    private static Bitmap convertIcon(Bitmap bitmap) {
        // Scale image only if necessary
        if ((bitmap.getWidth() > MAX_ICON_WIDTH) || (bitmap.getHeight() > MAX_ICON_HEIGHT)) {
            bitmap = Bitmap.createScaledBitmap(bitmap, MAX_ICON_WIDTH, MAX_ICON_HEIGHT, true);
        }
        // Convert to grayscale
        Canvas c = new Canvas(bitmap);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bitmap, 0, 0, paint);
        // Return result
        return bitmap;
    }

    public static byte[] get2BitsPixelsFromBitmap(Bitmap bitmap) {
        // Downsample to 2 bits image
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        byte[] b_pixels = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            b_pixels[i] = (byte) (pixels[i] >> 6 & 0x03);
        }
        return b_pixels;
    }

    public static byte[] getEncodedIconFromDrawable(Drawable drawable) {
        Bitmap icIncomingCallBitmap = convertDrawableToBitmap(drawable);
        if ((icIncomingCallBitmap.getWidth() > MAX_ICON_WIDTH) || (icIncomingCallBitmap.getHeight() > MAX_ICON_HEIGHT)) {
            icIncomingCallBitmap = Bitmap.createScaledBitmap(icIncomingCallBitmap, MAX_ICON_WIDTH, MAX_ICON_HEIGHT, true);
        }
        return RLEEncode(NotificationImage.get2BitsPixelsFromBitmap(icIncomingCallBitmap));
    }
}
