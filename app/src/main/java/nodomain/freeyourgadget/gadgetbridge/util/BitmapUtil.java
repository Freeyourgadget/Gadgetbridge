/*  Copyright (C) 2017-2021 Frank Slezak

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

package nodomain.freeyourgadget.gadgetbridge.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.nio.ByteBuffer;

public class BitmapUtil {
    /**
     * Downscale a bitmap to a maximum resolution. Doesn't scale if the bitmap is already smaller than the max resolution.
     *
     * @param bitmap
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Bitmap scaleWithMax(Bitmap bitmap, int maxWidth, int maxHeight) {
        // Scale image only if necessary
        if ((bitmap.getWidth() > maxWidth) || (bitmap.getHeight() > maxHeight)) {
            bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);
        }
        return bitmap;
    }

    /**
     * Get a Bitmap from any given Drawable.
     *
     * Note that this code will fail if the drawable is 0x0.
     *
     * @param drawable A Drawable to convert.
     * @return A Bitmap representing the drawable.
     */
    public static Bitmap convertDrawableToBitmap(Drawable drawable) {
        // If whoever made this drawable decided to be nice to us...
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Converts the provided Bitmap to grayscale.
     *
     * @param bitmap
     * @return
     */
    public static Bitmap convertToGrayscale(Bitmap bitmap) {
        Canvas c = new Canvas(bitmap);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bitmap, 0, 0, paint);
        return bitmap;
    }

    /**
     * Change the contrast and brightness on a Bitmap
     *
     * Code from: https://stackoverflow.com/questions/12891520/how-to-programmatically-change-contrast-of-a-bitmap-in-android#17887577
     *
     * @param bmp input bitmap
     * @param contrast 0..10 1 is default
     * @param brightness -255..255 0 is default
     * @return new bitmap
     */
    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness)
    {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }


    /**
     * Invert the colors of a Bitmap
     *
     * @param bmp input bitmap
     * @return new bitmap
     */
    public static Bitmap invertBitmapColors(Bitmap bmp)
    {
        ColorMatrix colorMatrix_Inverted =
                new ColorMatrix(new float[] {
                        -1,  0,  0,  0, 255,
                        0, -1,  0,  0, 255,
                        0,  0, -1,  0, 255,
                        0,  0,  0,  1,   0});

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix_Inverted));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }


    /**
     * Crops a circular image from the center of the provided Bitmap.
     * From: https://www.tutorialspoint.com/android-how-to-crop-circular-area-from-bitmap
     * @param srcBitmap
     * @return
     */
    public static Bitmap getCircularBitmap(Bitmap srcBitmap) {
        // Calculate the circular bitmap width with border
        int squareBitmapWidth = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight());
        // Initialize a new instance of Bitmap
        Bitmap dstBitmap = Bitmap.createBitmap (
                squareBitmapWidth, // Width
                squareBitmapWidth, // Height
                Bitmap.Config.ARGB_8888 // Config
        );
        Canvas canvas = new Canvas(dstBitmap);
        // Initialize a new Paint instance
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, squareBitmapWidth, squareBitmapWidth);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Calculate the left and top of copied bitmap
        float left = (squareBitmapWidth-srcBitmap.getWidth())/2;
        float top = (squareBitmapWidth-srcBitmap.getHeight())/2;
        canvas.drawBitmap(srcBitmap, left, top, paint);
        // Return the circular bitmap
        return dstBitmap;
    }

    /**
     * Rotates a given Bitmap
     * @param bitmap input bitmap
     * @param degree int Degree of rotation
     * @return new bitmap
     */
    public static Bitmap rotateImage(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    /**
     * Overlays two bitmaps on top of each other,
     * bmp1 is assumed to be larger or equal to bmp2
     * From: https://stackoverflow.com/a/2287218
     * @param bmp1
     * @param bmp2
     * @return new Bitmap
     */
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }

    /**
     * Converts a {@link Drawable} to a {@link Bitmap}, in ARGB8888 mode.
     *
     * @param drawable the {@link Drawable}
     * @return the {@link Bitmap}
     */
    public static Bitmap toBitmap(final Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Converts a {@link Bitmap} to an uncompressed TGA image, as raw bytes, in RGB565 encoding.
     * @param bmp the {@link Bitmap} to convert.
     * @param width the target width
     * @param height the target height
     * @param id the TGA ID
     * @return the raw bytes for the TGA image
     */
    public static byte[] convertToTgaRGB565(final Bitmap bmp, final int width, final int height, final byte[] id) {
        final Bitmap bmp565;
        if (bmp.getConfig().equals(Bitmap.Config.RGB_565) && bmp.getWidth() == width && bmp.getHeight() == height) {
            // Right encoding and size
            bmp565 = bmp;
        } else {
            // Convert encoding / scale
            bmp565 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            final Canvas canvas = new Canvas(bmp565);
            final Rect rect = new Rect(0, 0, width, height);
            canvas.drawBitmap(bmp, null, rect, null);
        }

        int size = bmp565.getRowBytes() * bmp565.getHeight();
        final ByteBuffer bmp565buf = ByteBuffer.allocate(size);
        bmp565.copyPixelsToBuffer(bmp565buf);

        // As per https://en.wikipedia.org/wiki/Truevision_TGA
        // 18 bytes
        final byte[] header = {
                // ID length
                (byte) id.length,
                // Color map type - (0 - no color map)
                0x00,
                // Image type (2 - uncompressed true-color image)
                0x02,
                // Color map specification (5 bytes)
                0x00, 0x00, // first entry index
                0x00, 0x00, /// color map length
                0x00, // color map entry size
                // Image dimensions and format (10 bytes)
                0x00, 0x00, // x origin
                0x00, 0x00, // y origin
                (byte) (width & 0xff), (byte) ((width >> 8) & 0xff), // width
                (byte) (height & 0xff), (byte) ((height >> 8) & 0xff), // height
                16, // bits per pixel (10)
                0x20, // image descriptor (0x20, 00100000)
                // bits 3-0 give the alpha channel depth, bits 5-4 give pixel ordering
                // Bit 4 of the image descriptor byte indicates right-to-left pixel ordering if set.
                // Bit 5 indicates an ordering of top-to-bottom. Otherwise, pixels are stored in bottom-to-top, left-to-right order.
        };

        final ByteBuffer tga565buf = ByteBuffer.allocate(header.length + id.length + size);

        tga565buf.put(header);
        tga565buf.put(id);
        tga565buf.put(bmp565buf.array());

        return tga565buf.array();
    }
}
