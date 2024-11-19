package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class StepsPeriodFragment extends StepsFragment<StepsPeriodFragment.StepsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;
    private TextView stepsAvg;
    private TextView stepsTotal;
    private TextView distanceAvg;
    private TextView distanceTotal;
    private BarChart stepsChart;

    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int STEPS_GOAL;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;

    public static StepsPeriodFragment newInstance ( int totalDays ) {
        StepsPeriodFragment fragmentFirst = new StepsPeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_steps_period, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.steps_date_view);
        stepsChart = rootView.findViewById(R.id.steps_chart);
        stepsAvg = rootView.findViewById(R.id.steps_avg);
        distanceAvg = rootView.findViewById(R.id.distance_avg);
        stepsTotal = rootView.findViewById(R.id.steps_total);
        distanceTotal = rootView.findViewById(R.id.distance_total);
        STEPS_GOAL = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        setupStepsChart();
        refresh();

        return rootView;
    }

    protected void setupStepsChart() {
        stepsChart.getDescription().setEnabled(false);
        if (TOTAL_DAYS <= 7) {
            stepsChart.setTouchEnabled(false);
            stepsChart.setPinchZoom(false);
        }
        stepsChart.setDoubleTapToZoomEnabled(false);
        stepsChart.getLegend().setEnabled(false);

        final XAxis xAxisBottom = stepsChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = stepsChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setAxisMinimum(0f);
        final LimitLine goalLine = new LimitLine(STEPS_GOAL);
        goalLine.setLineColor(getResources().getColor(R.color.steps_color));
        goalLine.setLineWidth(1.5f);
        goalLine.enableDashedLine(15f, 10f, 0f);
        yAxisLeft.addLimitLine(goalLine);

        final YAxis yAxisRight = stepsChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

        @Override
    public String getTitle() {
        return getString(R.string.steps);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
    }

    @Override
    protected StepsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        List<StepsDay> stepsDaysData = getMyStepsDaysData(db, day, device);
        return new StepsData(stepsDaysData);
    }

    @Override
    protected void updateChartsnUIThread(StepsData stepsData) {
        Date to = new Date((long) getTSEnd() * 1000);
        Date from = DateUtils.addDays(to,-(TOTAL_DAYS - 1));
        String toFormattedDate = new SimpleDateFormat("E, MMM dd").format(to);
        String fromFormattedDate = new SimpleDateFormat("E, MMM dd").format(from);
        mDateView.setText(fromFormattedDate + " - " + toFormattedDate);

        stepsChart.setData(null);

        List<BarEntry> entries = new ArrayList<>();
        int counter = 0;
        for(StepsDay day : stepsData.days) {
            entries.add(new BarEntry(counter, day.steps));
            counter++;
        }
        BarDataSet set = new BarDataSet(entries, "Steps");
        set.setDrawValues(true);
        set.setColors(getResources().getColor(R.color.steps_color));
        final XAxis x = stepsChart.getXAxis();
        x.setValueFormatter(getStepsChartDayValueFormatter(stepsData));
        stepsChart.getAxisLeft().setAxisMaximum(Math.max(set.getYMax(), STEPS_GOAL) + 2000);

        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);
        barData.setValueTextSize(10f);
        if (TOTAL_DAYS > 7) {
            stepsChart.setRenderer(new AngledLabelsChartRenderer(stepsChart, stepsChart.getAnimator(), stepsChart.getViewPortHandler()));
        }
        stepsChart.setData(barData);
        stepsAvg.setText(String.format(String.valueOf(stepsData.stepsDailyAvg)));
        final WorkoutValueFormatter valueFormatter = new WorkoutValueFormatter();
        distanceAvg.setText(valueFormatter.formatValue(stepsData.distanceDailyAvg, "km"));
        stepsTotal.setText(String.format(String.valueOf(stepsData.totalSteps)));
        distanceTotal.setText(valueFormatter.formatValue(stepsData.totalDistance, "km"));
    }

    ValueFormatter getStepsChartDayValueFormatter(StepsPeriodFragment.StepsData stepsData) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                StepsPeriodFragment.StepsDay day = stepsData.days.get((int) value);
                String pattern = TOTAL_DAYS > 7 ? "dd" : "EEE";
                SimpleDateFormat formatLetterDay = new SimpleDateFormat(pattern, Locale.getDefault());
                return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
            }
        };
    }

    @Override
    protected void renderCharts() {
        stepsChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    protected static class StepsData extends ChartsData {
        List<StepsDay> days;
        long stepsDailyAvg = 0;
        double distanceDailyAvg = 0;
        long totalSteps = 0;
        double totalDistance = 0;
        StepsDay todayStepsDay;
        protected StepsData(List<StepsDay> days) {
            this.days = days;
            int daysCounter = 0;
            for(StepsDay day : days) {
                this.totalSteps += day.steps;
                this.totalDistance += day.distance;
                if (day.steps > 0) {
                    daysCounter++;
                }
            }
            if (daysCounter > 0) {
                this.stepsDailyAvg = this.totalSteps / daysCounter;
                this.distanceDailyAvg = this.totalDistance / daysCounter;
            }
            this.todayStepsDay = days.get(days.size() - 1);
        }
    }
}
