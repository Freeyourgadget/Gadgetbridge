package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.ColorInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class GaugeDrawer {
    private static final Logger LOG = LoggerFactory.getLogger(GaugeDrawer.class);
    protected @ColorInt int color_unknown = Color.argb(25, 128, 128, 128);

    /**
     * Draw a simple gauge.
     *
     * @param color     the gauge color
     * @param value     the gauge value. Range: [0, 1]
     */
    public void drawSimpleGauge(ImageView gaugeBar, final int color,
                                   final float value) {

        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );

        // Draw gauge
        gaugeBar.setImageBitmap(drawSimpleGaugeInternal(
                width,
                Math.round(width * 0.075f),
                color,
                value
        ));
    }

    /**
     * @param width        Bitmap width in pixels
     * @param barWidth     Gauge bar width in pixels
     * @param filledColor  Color of the filled part of the gauge
     * @param filledFactor Factor between 0 and 1 that determines the amount of the gauge that should be filled
     * @return Bitmap containing the gauge
     */
    private Bitmap drawSimpleGaugeInternal(final int width, final int barWidth, @ColorInt final int filledColor, final float filledFactor) {
        final int height = width / 2;
        final int barMargin = (int) Math.ceil(barWidth / 2f);

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth * 0.75f);
        paint.setColor(color_unknown);
        canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180 + 180 * filledFactor, 180 - 180 * filledFactor, false, paint);

        if (filledFactor >= 0) {
            paint.setStrokeWidth(barWidth);
            paint.setColor(filledColor);
            canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180, 180 * filledFactor, false, paint);
        }

        return bitmap;
    }

    /**
     * Draws a segmented gauge.
     *
     * @param colors             the colors of each segment
     * @param segments           the size of each segment. The sum of all segments should be 1
     * @param value              the gauge value, in range [0, 1], or -1 for no value and only segments
     * @param fadeOutsideDot     whether to fade out colors outside the dot value
     * @param gapBetweenSegments whether to introduce a small gap between the segments
     */
    public void drawSegmentedGauge(ImageView gaugeBar,
                                      final int[] colors,
                                      final float[] segments,
                                      final float value,
                                      final boolean fadeOutsideDot,
                                      final boolean gapBetweenSegments) {
        if (colors.length != segments.length) {
            LOG.error("Colors length {} differs from segments length {}", colors.length, segments.length);
            return;
        }

        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );

        final int barWidth = Math.round(width * 0.075f);

        final int height = width / 2;
        final int barMargin = (int) Math.ceil(barWidth / 2f);

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(barWidth);

        final double cornersGapRadians = Math.asin((width * 0.055f) / (double) height);
        final double cornersGapFactor = cornersGapRadians / Math.PI;

        int dotColor = 0;
        float angleSum = 0;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == 0) {
                continue;
            }

            paint.setColor(colors[i]);
            paint.setStrokeWidth(barWidth);

            if (value < 0 || (value >= angleSum && value <= angleSum + segments[i])) {
                dotColor = colors[i];
            } else {
                if (fadeOutsideDot) {
                    paint.setColor(colors[i] - 0xB0000000);
                } else {
                    paint.setStrokeWidth(barWidth * 0.75f);
                }
            }

            float startAngleDegrees = 180 + angleSum * 180;
            float sweepAngleDegrees = segments[i] * 180;

            if (value >= 0) {
                // Do not draw to the end if it will be overlapped by the dot
                if (i == 0 && value <= cornersGapFactor) {
                    startAngleDegrees += (float) Math.toDegrees(cornersGapRadians);
                    sweepAngleDegrees -= (float) Math.toDegrees(cornersGapRadians);
                } else if (i == segments.length - 1 && value >= 1 - cornersGapFactor) {
                    sweepAngleDegrees -= (float) Math.toDegrees(cornersGapRadians);
                }
            }

            if (gapBetweenSegments) {
                if (i + 1 < segments.length) {
                    sweepAngleDegrees -= 2;
                }
            }

            canvas.drawArc(
                    barMargin,
                    barMargin,
                    width - barMargin,
                    width - barMargin,
                    startAngleDegrees,
                    sweepAngleDegrees,
                    false,
                    paint
            );
            angleSum += segments[i];
        }

        if (value >= 0) {
            // Prevent the dot from going outside the widget in the extremities
            final float angleRadians = (float) normalize(value, 0, 1, cornersGapRadians, Math.toRadians(180) - cornersGapRadians);

            paint.setColor(Color.TRANSPARENT);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            // In the corners the circle is slightly offset, so adjust it slightly
            final float widthAdjustment = width * 0.04f * (float) normalize(Math.abs(value - 0.5d), 0, 0.5d);

            final float x = ((width - (barWidth / 2f) - widthAdjustment) / 2f) * (float) Math.cos(angleRadians);
            final float y = (height - (barWidth / 2f)) * (float) Math.sin(angleRadians);

            // Draw hole
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((width / 2f) - x, height - y, barMargin * 1.6f, paint);

            // Draw dot
            paint.setColor(dotColor);
            paint.setXfermode(null);
            canvas.drawCircle((width / 2f) - x, height - y, barMargin, paint);
        }

        gaugeBar.setImageBitmap(bitmap);
    }

    public static Bitmap drawCircleGaugeSegmented(int width,
                                                  int barWidth,
                                                  final int[] colors,
                                                  final float[] segments,
                                                  final boolean gapBetweenSegments,
                                                  String text,
                                                  String lowerText,
                                                  Context context) {
        int TEXT_COLOR = GBApplication.getTextColor(context);
        int height = width;
        int barMargin = (int) Math.ceil(barWidth / 2f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(barWidth);
        paint.setColor(context.getResources().getColor(R.color.gauge_line_color));
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                width - barMargin,
                90,
                360,
                false,
                paint);
        paint.setStrokeWidth(barWidth);

        float remainingAngle = 360;
        float gapDegree = 1f;
        if (gapBetweenSegments) {
            int validSegments = segments.length;
            for (int i = 0; i < segments.length; i++) {
                if (segments[i] == 0) {
                    validSegments--;
                }
            }

            remainingAngle = 360 - (validSegments * gapDegree);
        }

        float angleSum = 0;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == 0) {
                continue;
            }

            paint.setColor(colors[i]);
            paint.setStrokeWidth(barWidth);

            float startAngleDegrees = 270 + (angleSum * remainingAngle);
            float sweepAngleDegrees = segments[i] * remainingAngle;

            canvas.drawArc(
                    barMargin,
                    barMargin,
                    width - barMargin,
                    height - barMargin,
                    startAngleDegrees,
                    sweepAngleDegrees,
                    false,
                    paint
            );
            angleSum += segments[i];
            if (gapBetweenSegments) {
                angleSum += (gapDegree / 360f);
            }
        }

        Paint textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        float textPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.06f, context.getResources().getDisplayMetrics());
        textPaint.setTextSize(textPixels);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int yPos = (int) ((float) height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2)) ;
        canvas.drawText(String.valueOf(text), width / 2f, yPos, textPaint);
        Paint textLowerPaint = new Paint();
        textLowerPaint.setColor(TEXT_COLOR);
        textLowerPaint.setTextAlign(Paint.Align.CENTER);
        float textLowerPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.025f, context.getResources().getDisplayMetrics());
        textLowerPaint.setTextSize(textLowerPixels);
        int yPosLowerText = (int) ((float) height / 2 - textPaint.ascent()) ;
        canvas.drawText(String.valueOf(lowerText), width / 2f, yPosLowerText, textLowerPaint);

        return bitmap;
    }

    public static Bitmap drawCircleGauge(int width,
                                         int barWidth,
                                         @ColorInt int filledColor,
                                         int value,
                                         int maxValue,
                                         Context context) {
        int TEXT_COLOR = GBApplication.getTextColor(context);
        int height = width;
        int barMargin = (int) Math.ceil(barWidth / 2f);
        float filledFactor = (float) value / maxValue;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth);
        paint.setColor(context.getResources().getColor(R.color.gauge_line_color));
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                width - barMargin,
                90,
                360,
                false,
                paint);
        paint.setStrokeWidth(barWidth);
        paint.setColor(filledColor);
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                height - barMargin,
                270,
                360 * filledFactor,
                false,
                paint
        );

        Paint textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        float textPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.06f, context.getResources().getDisplayMetrics());
        textPaint.setTextSize(textPixels);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int yPos = (int) ((float) height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2)) ;
        canvas.drawText(String.valueOf(value), width / 2f, yPos, textPaint);
        Paint textLowerPaint = new Paint();
        textLowerPaint.setColor(TEXT_COLOR);
        textLowerPaint.setTextAlign(Paint.Align.CENTER);
        float textLowerPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.025f, context.getResources().getDisplayMetrics());
        textLowerPaint.setTextSize(textLowerPixels);
        int yPosLowerText = (int) ((float) height / 2 - textPaint.ascent()) ;
        canvas.drawText(String.valueOf(maxValue), width / 2f, yPosLowerText, textLowerPaint);

        return bitmap;
    }

    public static double normalize(final double value, final double min, final double max) {
        return normalize(value, min, max, 0, 1);
    }

    public static double normalize(final double value, final double minSource, final double maxSource, final double minTarget, final double maxTarget) {
        return ((value - minSource) * (maxTarget - minTarget)) / (maxSource - minSource) + minTarget;
    }

}
