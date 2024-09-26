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
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiBitmapUtils {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBitmapUtils.class);
    private static final byte[] LVGL_RLE_HEADER = new byte[] {(byte) 0xe0, 0x21, (byte) 0xa5, 0x5a };

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

    public static byte[] decompressLvglRleV1(final byte[] bitmapData) {
        if (!ArrayUtils.equals(bitmapData, LVGL_RLE_HEADER, 0)) {
            LOG.debug("Compressed data does not start with expected LVGL RLE header (found {})",
                    GB.hexdump(bitmapData, 0, 4));
            return null;
        }

        int chunkSize = bitmapData[4] & 0xf;
        if (chunkSize == 0) {
            chunkSize = 1;
        }

        final ByteBuffer bb = ByteBuffer.wrap(bitmapData).order(ByteOrder.LITTLE_ENDIAN);
        bb.getInt(); // magic
        final int decompressedSize = (bb.getInt() >> 4) & 0xfffffff;

        final byte[] out = new byte[decompressedSize];
        int outOff = 0;

        while (bb.hasRemaining()) {
            byte control = bb.get();
            int n = control & 0x7f;

            if (outOff + chunkSize * (n+1) > out.length) {
                LOG.error("decompression overflow");
                return null;
            }

            if ((control & 0x80) != 0) {
                // copy next chunk n+1 times to out
                if (bb.remaining() < chunkSize) {
                    LOG.error("not enough data to decompress");
                    return null;
                }

                final byte[] chunk = new byte[chunkSize];
                bb.get(chunk);

                for (int i = 0; i < n + 1; i++) {
                    System.arraycopy(chunk, 0, out, outOff, chunk.length);
                    outOff += chunk.length;
                }
            } else {
                // copy next n+1 chunks to out
                if (bb.remaining() < chunkSize * (n + 1)) {
                    LOG.error("not enough data to decompress");
                    return null;
                }

                final byte[] chunk = new byte[chunkSize * (n+1)];
                bb.get(chunk);
                System.arraycopy(chunk, 0, out, outOff, chunk.length);
                outOff += chunk.length;
            }
        }

        return out;
    }

    public static byte[] decompressLvglRleV2(final byte[] bitmapData) {
        if (!ArrayUtils.equals(bitmapData, LVGL_RLE_HEADER, 0)) {
            LOG.debug("Compressed data does not start with expected LVGL RLE header (found {})",
                    GB.hexdump(bitmapData, 0, 4));
            return null;
        }

        int chunkSize = bitmapData[4] & 0xf;
        if (chunkSize == 0) {
            chunkSize = 1;
        }

        LOG.debug("Chunk size: {}", chunkSize);
        final ByteBuffer bb = ByteBuffer.wrap(bitmapData).order(ByteOrder.LITTLE_ENDIAN);
        bb.getInt(); // magic
        final int decompressedSize = (bb.getInt() >> 4) & 0xfffffff;
        LOG.debug("Compressed size: {}, decompressed size: {}", bb.remaining(), decompressedSize);

        final byte[] out = new byte[decompressedSize];
        int outOff = 0;

        while (bb.hasRemaining()) {
            byte control = bb.get();
            int n = control & 0x7f;

            if (outOff + chunkSize * n > out.length) {
                LOG.error("decompression overflow");
                return null;
            }

            if ((control & 0x80) != 0) {
                // copy next n+1 chunks to out
                if (bb.remaining() < chunkSize * n) {
                    LOG.error("not enough data to decompress");
                    return null;
                }

                final byte[] chunk = new byte[chunkSize * n];
                bb.get(chunk);
                System.arraycopy(chunk, 0, out, outOff, chunk.length);
                outOff += chunk.length;
            } else {
                // copy next chunk n+1 times to out
                if (bb.remaining() < chunkSize) {
                    LOG.error("not enough data to decompress");
                    return null;
                }

                final byte[] chunk = new byte[chunkSize];
                bb.get(chunk);

                for (int i = 0; i < n; i++) {
                    System.arraycopy(chunk, 0, out, outOff, chunk.length);
                    outOff += chunk.length;
                }
            }
        }

        return out;
    }

    public static Bitmap decodeWatchfaceImage(final byte[] bitmapData, final int bitmapFormat, final boolean swapRedBlueChannel, final int width, final int height) {
        final int expectedInputSize;
        switch (bitmapFormat) {
            case 0:
                expectedInputSize = width * height * 4;
                break;
            case 1:
            case 4:
            case 7:
                expectedInputSize = width * height * 2;
                break;
            case 16:
                expectedInputSize = 256 * 4 + width * height;
                break;
            default:
                LOG.warn("bitmap format {} unknown", bitmapFormat);
                return null;
        }

        if (expectedInputSize > bitmapData.length) {
            LOG.error("Not enough pixel data (expected {} bytes, got {})",
                    expectedInputSize,
                    bitmapData.length);
            return null;
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final ByteBuffer bb = ByteBuffer.wrap(bitmapData);
        bb.order(bitmapFormat == 7 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);


        int[] palette = new int[0];
        if (bitmapFormat == 16) {
            palette = new int[256];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = bb.getInt();
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                switch (bitmapFormat) {
                    case 0x00:
                        bitmap.setPixel(x, y, bb.getInt());
                        break;
                    case 0x01:
                    case 0x04:
                    case 0x07:
                        final int c565 = bb.getShort() & 0xffff;
                        final int pixel = 0xff000000 |
                                ((c565 & 0xf800) << 8) |
                                ((c565 & 0x07e0) << 5) |
                                ((c565 & 0x001f) << 3);
                        bitmap.setPixel(x, y, pixel);
                        break;
                    case 0x10:
                        final int paletteId = bb.get() & 0xff;
                        bitmap.setPixel(x, y, palette[paletteId]);
                        break;
                }
            }
        }

        return bitmap;
    }
}
