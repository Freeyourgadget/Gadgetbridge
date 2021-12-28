package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractActivityListingAdapter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

public class ActivityListingAdapter extends AbstractActivityListingAdapter<ActivitySession> {
    public static final String CHART_COLOR_START = "#e74c3c";
    public static final String CHART_COLOR_END = "#2ecc71";
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractWeekChartFragment.class);
    protected final int ANIM_TIME = 250;
    private final int SESSION_SUMMARY = ActivitySession.SESSION_SUMMARY;
    private final int SESSION_EMPTY = ActivitySession.SESSION_EMPTY;
    ActivityUser activityUser = new ActivityUser();
    int stepsGoal = activityUser.getStepsGoal();
    int distanceGoalMeters = activityUser.getDistanceGoalMeters();
    long activeTimeGoalTimeMillis = activityUser.getActiveTimeGoalMinutes() * 60 * 1000L;

    public ActivityListingAdapter(Context context) {
        super(context);
    }

    @Override
    protected View fill_dashboard(ActivitySession item, int position, View view, ViewGroup parent, Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.activity_list_dashboard_item, parent, false);
        PieChart ActiveStepsChart;
        PieChart DistanceChart;
        PieChart ActiveTimeChart;

        ActiveStepsChart = view.findViewById(R.id.activity_dashboard_piechart1);
        setUpChart(ActiveStepsChart);
        int steps = item.getActiveSteps();
        setChartsData(ActiveStepsChart, steps, stepsGoal, context.getString(R.string.activity_list_summary_active_steps), context);

        DistanceChart = view.findViewById(R.id.activity_dashboard_piechart2);
        setUpChart(DistanceChart);
        float distance = item.getDistance();
        setChartsData(DistanceChart, distance, distanceGoalMeters, context.getString(R.string.distance), context);

        ActiveTimeChart = view.findViewById(R.id.activity_dashboard_piechart3);
        setUpChart(ActiveTimeChart);
        long duration = item.getEndTime().getTime() - item.getStartTime().getTime();
        setChartsData(ActiveTimeChart, duration, activeTimeGoalTimeMillis, context.getString(R.string.activity_list_summary_active_time), context);

        TextView stepLabel = view.findViewById(R.id.line_layout_step_label);
        TextView stepTotalLabel = view.findViewById(R.id.line_layout_total_step_label);
        TextView distanceLabel = view.findViewById(R.id.line_layout_distance_label);
        TextView hrLabel = view.findViewById(R.id.heartrate_widget_label);
        TextView intensityLabel = view.findViewById(R.id.intensity_widget_label);
        TextView intensity2Label = view.findViewById(R.id.line_layout_intensity2_label);
        TextView durationLabel = view.findViewById(R.id.line_layout_duration_label);
        TextView sessionCountLabel = view.findViewById(R.id.line_layout_count_label);
        LinearLayout durationLayout = view.findViewById(R.id.line_layout_duration);
        LinearLayout countLayout = view.findViewById(R.id.line_layout_count);
        View hrLayout = view.findViewById(R.id.heartrate_widget_icon);
        LinearLayout stepsLayout = view.findViewById(R.id.line_layout_step);
        LinearLayout stepsTotalLayout = view.findViewById(R.id.line_layout_total_step);
        LinearLayout distanceLayout = view.findViewById(R.id.line_layout_distance);
        View intensityLayout = view.findViewById(R.id.intensity_widget_icon);
        View intensity2Layout = view.findViewById(R.id.line_layout_intensity2);

        stepLabel.setText(getStepLabel(item));
        stepTotalLabel.setText(getStepTotalLabel(item));
        distanceLabel.setText(getDistanceLabel(item));
        hrLabel.setText(getHrLabel(item));
        intensityLabel.setText(getIntensityLabel(item));
        intensity2Label.setText(getIntensityLabel(item));
        durationLabel.setText(getDurationLabel(item));
        sessionCountLabel.setText(getSessionCountLabel(item));

        if (!hasHR(item)) {
            hrLayout.setVisibility(View.GONE);
            hrLabel.setVisibility(View.GONE);
        } else {
            hrLayout.setVisibility(View.VISIBLE);
            hrLabel.setVisibility(View.VISIBLE);
        }

        if (!hasIntensity(item)) {
            intensityLayout.setVisibility(View.GONE);
            intensity2Layout.setVisibility(View.GONE);
            intensityLabel.setVisibility(View.GONE);
            intensity2Label.setVisibility(View.GONE);
        } else {
            intensityLayout.setVisibility(View.VISIBLE);
            intensity2Layout.setVisibility(View.VISIBLE);
            intensityLabel.setVisibility(View.VISIBLE);
            intensity2Label.setVisibility(View.VISIBLE);
        }

        if (!hasDistance(item)) {
            distanceLayout.setVisibility(View.GONE);
        } else {
            distanceLayout.setVisibility(View.VISIBLE);
        }

        if (!hasSteps(item)) {
            stepsLayout.setVisibility(View.GONE);
        } else {
            stepsLayout.setVisibility(View.VISIBLE);
        }

        if (!hasTotalSteps(item)) {
            stepsTotalLayout.setVisibility(View.GONE);
            countLayout.setVisibility(View.GONE);
            durationLayout.setVisibility(View.GONE);
        } else {
            stepsTotalLayout.setVisibility(View.VISIBLE);
            countLayout.setVisibility(View.VISIBLE);
            durationLayout.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void setChartsData(PieChart pieChart, float value, float target, String label, Context context) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) value, context.getResources().getDrawable(R.drawable.ic_star_gold)));

        Easing.EasingFunction animationEffect = Easing.EaseInOutSine;

        if (value < target) {
            entries.add(new PieEntry((float) (target - value)));
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

    @Override
    protected String getDateLabel(ActivitySession item) {
        return "";
    }

    @Override
    protected boolean hasGPS(ActivitySession item) {
        return false;
    }

    @Override
    protected boolean hasDate(ActivitySession item) {
        return false;
    }

    @Override
    protected String getTimeFrom(ActivitySession item) {
        Date time = item.getStartTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getTimeTo(ActivitySession item) {
        Date time = item.getEndTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getActivityName(ActivitySession item) {
        return ActivityKind.asString(item.getActivityKind(), getContext());
    }

    @Override
    protected String getStepLabel(ActivitySession item) {
        return String.valueOf(item.getActiveSteps());
    }

    @Override
    protected String getDistanceLabel(ActivitySession item) {
        return FormatUtils.getFormattedDistanceLabel(item.getDistance());
    }

    @Override
    protected String getHrLabel(ActivitySession item) {
        return String.valueOf(item.getHeartRateAverage());
    }

    @Override
    protected String getIntensityLabel(ActivitySession item) {
        DecimalFormat df = new DecimalFormat("###");
        return df.format(item.getIntensity());
    }

    @Override
    protected String getDurationLabel(ActivitySession item) {
        long duration = item.getEndTime().getTime() - item.getStartTime().getTime();
        return DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    protected String getSpeedLabel(ActivitySession item) {
        long duration = item.getEndTime().getTime() - item.getStartTime().getTime();
        double distanceMeters = item.getDistance();

        double speed = distanceMeters * 1000 / duration;
        String unit = "###.#km/h";
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            unit = "###.#mi/h";
            speed = speed * 0.6213712;
        }
        DecimalFormat df = new DecimalFormat(unit);
        return df.format(speed);
    }

    @Override
    protected String getSessionCountLabel(ActivitySession item) {
        return String.valueOf(item.getSessionCount());
    }

    @Override
    public boolean hasHR(ActivitySession item) {
        return item.getHeartRateAverage() > 0;
    }

    @Override
    public boolean hasIntensity(ActivitySession item) {
        return item.getIntensity() > 0;
    }

    @Override
    protected boolean hasDistance(ActivitySession item) {
        return item.getDistance() > 0;
    }

    @Override
    protected boolean hasSteps(ActivitySession item) {
        return item.getActiveSteps() > 0;
    }

    @Override
    protected boolean hasTotalSteps(ActivitySession item) {
        return item.getTotalDaySteps() > 0;
    }

    @Override
    protected boolean isSummary(ActivitySession item, int position) {
        int sessionType = item.getSessionType();
        return sessionType == SESSION_SUMMARY;
    }

    @Override
    protected boolean isEmptySession(ActivitySession item, int position) {
        int sessionType = item.getSessionType();
        return sessionType == SESSION_EMPTY;
    }

    @Override
    protected boolean isEmptySummary(ActivitySession item) {
        return item.getIsEmptySummary();
    }

    @Override
    protected String getStepTotalLabel(ActivitySession item) {
        return String.valueOf(item.getTotalDaySteps());
    }

    @Override
    protected int getIcon(ActivitySession item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }
}
