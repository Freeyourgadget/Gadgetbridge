package nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Arrays;
import java.util.BitSet;

public class BitmapToBitSet {
    public int getWidth() {
        return width;
    }

    private final int width;
    private final int height;
    private final Bitmap bitmap;
    private final BitSet bwPixels;
    private int threshold = 128;

    public BitmapToBitSet(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        this.bwPixels = new BitSet(this.width * this.height);
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public Bitmap getPreview() {
        final Bitmap bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final int[] pixels = new int[width * height];
        Arrays.fill(pixels, Color.WHITE);

        for (int i = bwPixels.nextSetBit(0); i >= 0; i = bwPixels.nextSetBit(i + 1)) {
            pixels[i] = Color.BLACK;
        }

        bwBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bwBitmap;
    }

    public BitSet toBlackAndWhite(final boolean applyDithering) {
        bwPixels.clear();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int currentPixelColor = pixels[index];

                if (Color.alpha(currentPixelColor) == 0) {
                    pixels[index] = Color.WHITE; //flatten transparent pixels to white
                    continue;
                }

                int currentPixelGrayscale = getGrayscaleValue(currentPixelColor);
                boolean isBlack = (currentPixelGrayscale < threshold);

                if (isBlack)
                    bwPixels.set(index);

                if (applyDithering) {
                    int bwResultingPixel = (isBlack) ? 0 : 255;

                    pixels[index] = Color.argb(Color.alpha(currentPixelColor), bwResultingPixel, bwResultingPixel, bwResultingPixel);

                    int quantError = currentPixelGrayscale - bwResultingPixel;

                    // Apply dithering to neighboring pixels
                    if (x + 1 < width) {
                        applyErrorToPixel(pixels, y * width + (x + 1), quantError * 7 / 16);
                    }
                    if (x - 1 >= 0 && y + 1 < height) {
                        applyErrorToPixel(pixels, (y + 1) * width + (x - 1), quantError * 3 / 16);
                    }
                    if (y + 1 < height) {
                        applyErrorToPixel(pixels, (y + 1) * width + x, quantError * 5 / 16);
                    }
                    if (x + 1 < width && y + 1 < height) {
                        applyErrorToPixel(pixels, (y + 1) * width + (x + 1), quantError / 16);
                    }
                }
            }
        }
        return bwPixels;
    }

    private void applyErrorToPixel(int[] pixels, int index, int error) {
        int currentPixelColor = pixels[index];
        final int newGray = Math.max(0, Math.min(255, getGrayscaleValue(currentPixelColor) + error));
        pixels[index] = Color.argb(Color.alpha(currentPixelColor), newGray, newGray, newGray);
    }

    private int getGrayscaleValue(int color) {
        return (int) (0.3 * Color.red(color) + 0.59 * Color.green(color) + 0.11 * Color.blue(color));
    }

}
