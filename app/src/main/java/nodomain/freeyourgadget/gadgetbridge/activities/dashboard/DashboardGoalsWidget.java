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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardGoalsWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardGoalsWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardGoalsWidget.class);
    private View goalsView;
    private ImageView goalsChart;

    public DashboardGoalsWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardGoalsWidget.
     */
    public static DashboardGoalsWidget newInstance(DashboardFragment.DashboardData dashboardData) {
        DashboardGoalsWidget fragment = new DashboardGoalsWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        goalsView = inflater.inflate(R.layout.dashboard_widget_goals, container, false);
        goalsChart = goalsView.findViewById(R.id.dashboard_goals_chart);

        // Initialize legend
        TextView legend = goalsView.findViewById(R.id.dashboard_goals_legend);
        SpannableString l_steps = new SpannableString("■ " + getString(R.string.steps));
        l_steps.setSpan(new ForegroundColorSpan(color_activity), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_distance = new SpannableString("■ " + getString(R.string.distance));
        l_distance.setSpan(new ForegroundColorSpan(color_distance), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_active_time = new SpannableString("■ " + getString(R.string.activity_list_summary_active_time));
        l_active_time.setSpan(new ForegroundColorSpan(color_active_time), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_sleep = new SpannableString("■ " + getString(R.string.menuitem_sleep));
        l_sleep.setSpan(new ForegroundColorSpan(color_light_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder legendBuilder = new SpannableStringBuilder();
        legend.setText(legendBuilder.append(l_steps).append(" ").append(l_distance).append("\n").append(l_active_time).append(" ").append(l_sleep));

        Prefs prefs = GBApplication.getPrefs();
        legend.setVisibility(prefs.getBoolean("dashboard_widget_goals_legend", true) ? View.VISIBLE : View.GONE);

        return goalsView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (goalsChart != null) fillData();
    }

    @Override
    protected void fillData() {
        if (goalsView == null) return;
        goalsView.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private Bitmap goalsBitmap;

        @Override
        protected Void doInBackground(Void... params) {
            final long nanoStart = System.nanoTime();

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            int height = width;
            int barWidth = Math.round(height * 0.04f);
            int barMargin = (int) Math.ceil(barWidth / 2f);

            goalsBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(goalsBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);

            paint.setStrokeWidth(barWidth * 0.75f);
            paint.setColor(color_unknown);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360, false, paint);
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_activity);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360 * dashboardData.getStepsGoalFactor(), false, paint);

            barMargin += barWidth * 1.5;
            paint.setStrokeWidth(barWidth * 0.75f);
            paint.setColor(color_unknown);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360, false, paint);
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_distance);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360 * dashboardData.getDistanceGoalFactor(), false, paint);

            barMargin += barWidth * 1.5;
            paint.setStrokeWidth(barWidth * 0.75f);
            paint.setColor(color_unknown);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360, false, paint);
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_active_time);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360 * dashboardData.getActiveMinutesGoalFactor(), false, paint);

            barMargin += barWidth * 1.5;
            paint.setStrokeWidth(barWidth * 0.75f);
            paint.setColor(color_unknown);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360, false, paint);
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_light_sleep);
            canvas.drawArc(barMargin, barMargin, width - barMargin, height - barMargin, 270, 360 * dashboardData.getSleepMinutesGoalFactor(), false, paint);

            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", DashboardGoalsWidget.this.getClass().getSimpleName(), executionTime);

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            goalsChart.setImageBitmap(goalsBitmap);
        }
    }
}