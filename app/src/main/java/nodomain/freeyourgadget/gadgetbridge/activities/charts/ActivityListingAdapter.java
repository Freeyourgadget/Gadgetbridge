/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.MPPointF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractActivityListingAdapter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityListItem;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

public class ActivityListingAdapter extends AbstractActivityListingAdapter<ActivitySession> {
    public static final String CHART_COLOR_START = "#e74c3c";
    public static final String CHART_COLOR_END = "#2ecc71";
    protected static final Logger LOG = LoggerFactory.getLogger(ActivityListingAdapter.class);
    protected final int ANIM_TIME = 250;
    ActivityUser activityUser = new ActivityUser();
    int stepsGoal = activityUser.getStepsGoal();
    int distanceGoalMeters = activityUser.getDistanceGoalMeters();
    long activeTimeGoalTimeMillis = activityUser.getActiveTimeGoalMinutes() * 60 * 1000L;

    public ActivityListingAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public AbstractActivityListingViewHolder<ActivitySession> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case 0: // dashboard
                return new DashboardViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.activity_list_dashboard_item, parent, false));
            case 2: // item
                return new ActivityItemViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.activity_list_item, parent, false));
        }

        return super.onCreateViewHolder(parent, viewType);
    }

    private void setChartsData(PieChart pieChart, float value, float target, String label, Context context) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(value, AppCompatResources.getDrawable(context, R.drawable.ic_star_gold)));

        Easing.EasingFunction animationEffect = Easing.EaseInOutSine;

        if (value < target) {
            entries.add(new PieEntry(target - value));
        }

        pieChart.setCenterText(String.format("%d%%\n%s", (int) (value * 100 / target), label));
        float colorValue = Math.max(0, Math.min(1, value / target));
        int chartColor = interpolateColor(Color.parseColor(CHART_COLOR_START), Color.parseColor(CHART_COLOR_END), colorValue);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setDrawIcons(false);
        dataSet.setIconsOffset(new MPPointF(0, -66));

        if (colorValue == 1) {
            dataSet.setDrawIcons(true);
        }
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(chartColor, Color.LTGRAY);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(0f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.animateY(ANIM_TIME, animationEffect);
    }

    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    public static class ActivityItemViewHolder extends AbstractActivityListingViewHolder<ActivitySession> {
        final View rootView;
        final ActivityListItem activityListItem;

        public ActivityItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.rootView = itemView;
            this.activityListItem = new ActivityListItem(itemView);
        }

        @Override
        public void fill(final int position, final ActivitySession session, final boolean selected) {
            this.activityListItem.update(
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getActivityKind(),
                    null,
                    session.getActiveSteps(),
                    session.getDistance(),
                    session.getHeartRateAverage(),
                    session.getIntensity(),
                    session.getEndTime().getTime() - session.getStartTime().getTime(),
                    false,
                    null,
                    position % 2 == 1,
                    selected
            );
        }
    }

    public class DashboardViewHolder extends AbstractActivityListingViewHolder<ActivitySession> {
        final PieChart ActiveStepsChart;
        final PieChart DistanceChart;
        final PieChart ActiveTimeChart;
        final TextView stepLabel;
        final TextView stepTotalLabel;
        final TextView distanceLabel;
        final TextView hrLabel;
        final TextView intensityLabel;
        final TextView intensity2Label;
        final TextView durationLabel;
        final TextView sessionCountLabel;
        final LinearLayout durationLayout;
        final LinearLayout countLayout;
        final View hrLayout;
        final LinearLayout stepsLayout;
        final LinearLayout stepsTotalLayout;
        final LinearLayout distanceLayout;
        final View intensityLayout;
        final View intensity2Layout;

        public DashboardViewHolder(@NonNull final View itemView) {
            super(itemView);

            ActiveStepsChart = itemView.findViewById(R.id.activity_dashboard_piechart1);
            DistanceChart = itemView.findViewById(R.id.activity_dashboard_piechart2);
            ActiveTimeChart = itemView.findViewById(R.id.activity_dashboard_piechart3);
            stepLabel = itemView.findViewById(R.id.line_layout_step_label);
            stepTotalLabel = itemView.findViewById(R.id.line_layout_total_step_label);
            distanceLabel = itemView.findViewById(R.id.line_layout_distance_label);
            hrLabel = itemView.findViewById(R.id.heartrate_widget_label);
            intensityLabel = itemView.findViewById(R.id.intensity_widget_label);
            intensity2Label = itemView.findViewById(R.id.line_layout_intensity2_label);
            durationLabel = itemView.findViewById(R.id.line_layout_duration_label);
            sessionCountLabel = itemView.findViewById(R.id.line_layout_count_label);
            durationLayout = itemView.findViewById(R.id.line_layout_duration);
            countLayout = itemView.findViewById(R.id.line_layout_count);
            hrLayout = itemView.findViewById(R.id.heartrate_widget_icon);
            stepsLayout = itemView.findViewById(R.id.line_layout_step);
            stepsTotalLayout = itemView.findViewById(R.id.line_layout_total_step);
            distanceLayout = itemView.findViewById(R.id.line_layout_distance);
            intensityLayout = itemView.findViewById(R.id.intensity_widget_icon);
            intensity2Layout = itemView.findViewById(R.id.line_layout_intensity2);
        }

        @Override
        public void fill(final int position, final ActivitySession session, final boolean selected) {
            setUpChart(ActiveStepsChart);
            int steps = session.getActiveSteps();
            setChartsData(ActiveStepsChart, steps, stepsGoal, getContext().getString(R.string.activity_list_summary_active_steps), getContext());

            setUpChart(DistanceChart);
            float distance = session.getDistance();
            setChartsData(DistanceChart, distance, distanceGoalMeters, getContext().getString(R.string.distance), getContext());

            setUpChart(ActiveTimeChart);
            long duration = session.getEndTime().getTime() - session.getStartTime().getTime();
            setChartsData(ActiveTimeChart, duration, activeTimeGoalTimeMillis, getContext().getString(R.string.activity_list_summary_active_time), getContext());

            durationLabel.setText(DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS));
            sessionCountLabel.setText(String.valueOf(session.getSessionCount()));

            if (session.getHeartRateAverage() > 0) {
                hrLabel.setText(String.valueOf(session.getHeartRateAverage()));
                hrLayout.setVisibility(View.VISIBLE);
                hrLabel.setVisibility(View.VISIBLE);
            } else {
                hrLayout.setVisibility(View.GONE);
                hrLabel.setVisibility(View.GONE);
            }

            if (session.getIntensity() > 0) {
                final DecimalFormat df = new DecimalFormat("###");
                intensityLabel.setText(df.format(session.getIntensity()));
                intensity2Label.setText(df.format(session.getIntensity()));
                intensityLayout.setVisibility(View.VISIBLE);
                intensity2Layout.setVisibility(View.VISIBLE);
                intensityLabel.setVisibility(View.VISIBLE);
                intensity2Label.setVisibility(View.VISIBLE);
            } else {
                intensityLayout.setVisibility(View.GONE);
                intensity2Layout.setVisibility(View.GONE);
                intensityLabel.setVisibility(View.GONE);
                intensity2Label.setVisibility(View.GONE);
            }

            if (session.getDistance() > 0) {
                distanceLabel.setText(FormatUtils.getFormattedDistanceLabel(session.getDistance()));
                distanceLayout.setVisibility(View.VISIBLE);
            } else {
                distanceLayout.setVisibility(View.GONE);
            }

            if (session.getActiveSteps() > 0) {
                stepLabel.setText(String.valueOf(session.getActiveSteps()));
                stepsLayout.setVisibility(View.VISIBLE);
            } else {
                stepsLayout.setVisibility(View.GONE);
            }

            if (session.getTotalDaySteps() > 0) {
                stepTotalLabel.setText(String.valueOf(session.getTotalDaySteps()));
                stepsTotalLayout.setVisibility(View.VISIBLE);
                countLayout.setVisibility(View.VISIBLE);
                durationLayout.setVisibility(View.VISIBLE);
            } else {
                stepsTotalLayout.setVisibility(View.GONE);
                countLayout.setVisibility(View.GONE);
                durationLayout.setVisibility(View.GONE);
            }
        }

        private void setUpChart(PieChart DashboardChart) {
            DashboardChart.setNoDataText("");
            DashboardChart.getLegend().setEnabled(false);
            DashboardChart.setDrawHoleEnabled(true);
            DashboardChart.setHoleColor(Color.WHITE);
            DashboardChart.getDescription().setText("");
            DashboardChart.setTransparentCircleColor(Color.WHITE);
            DashboardChart.setTransparentCircleAlpha(110);
            DashboardChart.setHoleRadius(70f);
            DashboardChart.setTransparentCircleRadius(75f);
            DashboardChart.setDrawCenterText(true);
            DashboardChart.setRotationEnabled(true);
            DashboardChart.setHighlightPerTapEnabled(true);
            DashboardChart.setCenterTextOffset(0, 0);
        }
    }
}
