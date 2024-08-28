/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;

public abstract class AbstractGaugeWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGaugeWidget.class);

    private TextView gaugeValue;
    private ImageView gaugeBar;

    private final int label;
    private final String targetActivityTab;

    public AbstractGaugeWidget(@StringRes final int label, @Nullable final String targetActivityTab) {
        this.label = label;
        this.targetActivityTab = targetActivityTab;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.dashboard_widget_generic_gauge, container, false);

        if (targetActivityTab != null) {
            onClickOpenChart(fragmentView, targetActivityTab, label);
        }

        gaugeValue = fragmentView.findViewById(R.id.gauge_value);
        gaugeBar = fragmentView.findViewById(R.id.gauge_bar);
        final TextView gaugeLabel = fragmentView.findViewById(R.id.gauge_label);
        gaugeLabel.setText(label);

        fillData();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gaugeValue != null && gaugeBar != null) fillData();
    }

    @Override
    protected void fillData() {
        if (gaugeBar == null) return;
        gaugeBar.post(() -> {
            final FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
            myAsyncTask.execute();
        });
    }

    /**
     * This is called from the async task, outside of the UI thread. It's expected that
     * {@link nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment.DashboardData} be
     * populated with the necessary data for display.
     *
     * @param dashboardData the DashboardData to populate
     */
    protected abstract void populateData(DashboardFragment.DashboardData dashboardData);

    /**
     * This is called from the UI thread.
     *
     * @param dashboardData populated DashboardData
     */
    protected abstract void draw(DashboardFragment.DashboardData dashboardData);

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(final Void... params) {
            final long nanoStart = System.nanoTime();
            try {
                populateData(dashboardData);
            } catch (final Exception e) {
                LOG.error("fillData for {} failed", AbstractGaugeWidget.this.getClass().getSimpleName(), e);
            }
            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", AbstractGaugeWidget.this.getClass().getSimpleName(), executionTime);
            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {
            super.onPostExecute(unused);
            try {
                draw(dashboardData);
            } catch (final Exception e) {
                LOG.error("draw for {} failed", AbstractGaugeWidget.this.getClass().getSimpleName(), e);
            }
        }
    }

    protected void setText(final CharSequence text) {
        gaugeValue.setText(text);
    }

    /**
     * Draw a simple gauge.
     *
     * @param color     the gauge color
     * @param value     the gauge value. Range: [0, 1]
     */
    protected void drawSimpleGauge(final int color,
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
    protected void drawSegmentedGauge(final int[] colors,
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

    protected static double normalize(final double value, final double min, final double max) {
        return normalize(value, min, max, 0, 1);
    }

    public static double normalize(final double value, final double minSource, final double maxSource, final double minTarget, final double maxTarget) {
        return ((value - minSource) * (maxTarget - minTarget)) / (maxSource - minSource) + minTarget;
    }
}
