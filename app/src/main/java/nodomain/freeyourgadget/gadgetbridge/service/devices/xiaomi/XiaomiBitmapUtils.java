/*  Copyright (C) 2024 Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class XiaomiBitmapUtils {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBitmapUtils.class);

    public static final int PIXEL_FORMAT_RGB_565_LE = 0;
    public static final int PIXEL_FORMAT_RGB_565_BE = 1;
    public static final int PIXEL_FORMAT_XRGB_8888_LE = 2;
    public static final int PIXEL_FORMAT_ARGB_8888_LE = 3;
    public static final int PIXEL_FORMAT_ARGB_8565_LE = 7;
    public static final int PIXEL_FORMAT_ABGR_8565_LE = 8;

    public static String getPixelFormatString(final int pixelFormat) {
        switch (pixelFormat) {
            case PIXEL_FORMAT_RGB_565_LE:
                return "RGB_565_LE";
            case PIXEL_FORMAT_RGB_565_BE:
                return "RGB_565_BE";
            case PIXEL_FORMAT_XRGB_8888_LE:
                return "XRGB_8888_LE";
            case PIXEL_FORMAT_ARGB_8888_LE:
                return "ARGB_8888_LE";
            case PIXEL_FORMAT_ARGB_8565_LE:
                return "ARGB_8565_LE";
            case PIXEL_FORMAT_ABGR_8565_LE:
                return "ABGR_8565_LE";
        }

        return "UNKNOWN";
    }

    private static Bitmap fit(final Drawable drawable, final int width, final int height) {
        final Rect originalBounds = drawable.copyBounds();
        final Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        drawable.setBounds(originalBounds);
        return result;
    }

    public static byte[] convertToRgb565(final Bitmap bitmap, final boolean littleEndian) {
        final ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 2);

        if (littleEndian)
            buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                final int pixel = bitmap.getPixel(x, y);
                final int r = (pixel >> 19) & 0x1f,
                        g = (pixel >> 10) & 0x3f,
                        b = pixel & 0x1f;
                buffer.putShort((short) ((r << 11) | (g << 5) | b));
            }
        }

        return buffer.array();
    }

    public static byte[] convertToRgb565L(final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        final Bitmap bitmap = fit(drawable, boundsWidth, boundsHeight);
        final byte[] rawBitmap = convertToRgb565(bitmap, true);
        bitmap.recycle();
        return rawBitmap;
    }

    public static byte[] convertToRgb565B(final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        final Bitmap bitmap = fit(drawable, boundsWidth, boundsHeight);
        final byte[] rawBitmap = convertToRgb565(bitmap, true);
        bitmap.recycle();
        return rawBitmap;
    }

    public static byte[] convertToArgb8565(final Bitmap bitmap, final boolean swapChannels) {
        final ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 3).order(ByteOrder.LITTLE_ENDIAN);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                final int pixel = bitmap.getPixel(x, y);
                final int a = (pixel >> 24) & 0xff,
                        r = (pixel >> 19) & 0x1f,
                        g = (pixel >> 10) & 0x3f,
                        b = (pixel >> 3) & 0x1f;
                // emulate int24
                buffer.putShort((short) (((swapChannels ? b : r) << 11) | (g << 5) | (swapChannels ? r : b)));
                buffer.put((byte) a);
            }
        }

        return buffer.array();
    }

    public static byte[] convertToArgb8565(final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        final Bitmap bitmap = fit(drawable, boundsWidth, boundsHeight);
        final byte[] rawBitmap = convertToArgb8565(bitmap, false);
        bitmap.recycle();
        return rawBitmap;
    }

    public static byte[] convertToAbgr8565(final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        final Bitmap bitmap = fit(drawable, boundsWidth, boundsHeight);
        final byte[] rawBitmap = convertToArgb8565(bitmap, true);
        bitmap.recycle();
        return rawBitmap;
    }

    public static byte[] convertToArgb8888(final Bitmap bitmap) {
        final ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                buffer.putInt(bitmap.getPixel(x, y));
            }
        }
        return buffer.array();
    }

    public static byte[] convertToArgb8888(final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        final Bitmap bitmap = fit(drawable, boundsWidth, boundsHeight);
        final byte[] rawBitmap = convertToArgb8888(bitmap);
        bitmap.recycle();
        return rawBitmap;
    }

    public static byte[] convertToPixelFormat(final int pixelFormat, final Drawable drawable, final int boundsWidth, final int boundsHeight) {
        switch (pixelFormat) {
            case PIXEL_FORMAT_RGB_565_LE:
                return convertToRgb565L(drawable, boundsWidth, boundsHeight);
            case PIXEL_FORMAT_RGB_565_BE:
                return convertToRgb565B(drawable, boundsWidth, boundsHeight);
            case PIXEL_FORMAT_XRGB_8888_LE:
            case PIXEL_FORMAT_ARGB_8888_LE:
                return convertToArgb8888(drawable, boundsWidth, boundsHeight);
            case PIXEL_FORMAT_ARGB_8565_LE:
                return convertToArgb8565(drawable, boundsWidth, boundsHeight);
            case PIXEL_FORMAT_ABGR_8565_LE:
                return convertToAbgr8565(drawable, boundsWidth, boundsHeight);
        }

        LOG.error("Unknown pixel format {}", pixelFormat);
        return null;
    }
}
