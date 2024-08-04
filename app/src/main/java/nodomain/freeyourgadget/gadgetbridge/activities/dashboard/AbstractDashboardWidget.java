/*  Copyright (C) 2023-2024 Arjan Schrijver

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
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;

public abstract class AbstractDashboardWidget extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDashboardWidget.class);

    protected static String ARG_DASHBOARD_DATA = "dashboard_widget_argument_data";

    protected DashboardFragment.DashboardData dashboardData;

    protected @ColorInt int color_unknown = Color.argb(25, 128, 128, 128);
    protected @ColorInt int color_not_worn = Color.BLACK;
    protected @ColorInt int color_worn = Color.rgb(128, 128, 128);
    protected @ColorInt int color_activity = Color.GREEN;
    protected @ColorInt int color_exercise = Color.rgb(255, 128, 0);
    protected @ColorInt int color_deep_sleep = Color.rgb(0, 84, 163);
    protected @ColorInt int color_light_sleep = Color.rgb(7, 158, 243);
    protected @ColorInt int color_rem_sleep = Color.rgb(228, 39, 199);
    protected @ColorInt int color_distance = Color.BLUE;
    protected @ColorInt int color_active_time = Color.rgb(170, 0, 255);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dashboardData = (DashboardFragment.DashboardData) getArguments().getSerializable(ARG_DASHBOARD_DATA);
        }
    }


    public void update() {
        fillData();
    }

    protected abstract void fillData();

    /**
     * @param width Bitmap width in pixels
     * @param barWidth Gauge bar width in pixels
     * @param filledColor Color of the filled part of the gauge
     * @param filledFactor Factor between 0 and 1 that determines the amount of the gauge that should be filled
     * @return Bitmap containing the gauge
     */
    Bitmap drawGauge(int width, int barWidth, @ColorInt int filledColor, float filledFactor) {
        int height = width / 2;
        int barMargin = (int) Math.ceil(barWidth / 2f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth * 0.75f);
        paint.setColor(color_unknown);
        canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180 + 180 * filledFactor, 180 - 180 * filledFactor, false, paint);
        paint.setStrokeWidth(barWidth);
        paint.setColor(filledColor);
        canvas.drawArc(barMargin, barMargin, width - barMargin, width - barMargin, 180, 180 * filledFactor, false, paint);

        return bitmap;
    }
}
