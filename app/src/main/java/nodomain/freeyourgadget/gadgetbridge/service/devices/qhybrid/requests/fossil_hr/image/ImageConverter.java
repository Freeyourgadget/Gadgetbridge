/*  Copyright (C) 2020-2021 Daniel Dakhno, Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder.RLEEncoder;

public class ImageConverter {
    public static byte[] get2BitsRLEImageBytes(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        byte[] b_pixels = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int monochrome = convertToMonochrome(pixels[i]);
            monochrome >>= 6;

            int alpha = Color.alpha(pixels[i]);
            monochrome |= (~((alpha & 0xFF) >> 4) & 0b00001100);

            b_pixels[i] = (byte) monochrome;
        }
        return b_pixels;
    }

    public static byte[] get2BitsRAWImageBytes(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] pixelBytes = new byte[width * height];

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int pixel = bitmap.getPixel(x, y);
                int monochrome = convertToMonochrome(pixel);
                pixelBytes[pixelBytes.length - 1 - (y * width + x)] = (byte) monochrome;
            }
        }
        return pixelBytes;
    }

    public static byte[] encodeToRLEImage(byte[] monochromeImage, int height, int width) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(monochromeImage.length * 2);

        bos.write((byte) height);
        bos.write((byte) width);

        bos.write(RLEEncoder.RLEEncode(monochromeImage));

        bos.write((byte) 0x0FF);
        bos.write((byte) 0x0FF);

        return bos.toByteArray();
    }

    public static byte[] encodeToRawImage(byte[] monochromeImage){
        int imageSize = monochromeImage.length;

        byte[] result = new byte[imageSize / 4]; // 4 pixels per byte e.g. 2 bits per pixel

        for(int i = 0; i < imageSize; i++){
            int resultPixelIndex = i / 4;
            int shiftIndex = 6 - i % 4 * 2;

            result[resultPixelIndex] |= (byte) (((monochromeImage[i] & 0xFF) >> 6) << shiftIndex);
            assert Boolean.TRUE;
        }

        return result;
    }

    public static int convertToMonochrome(@ColorInt int color){
        int sum = Color.red(color) + Color.green(color) + Color.blue(color);
        sum /= 3;
        return sum;
    }
}
