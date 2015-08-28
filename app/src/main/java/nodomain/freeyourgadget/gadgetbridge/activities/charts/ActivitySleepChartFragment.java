package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


public class ActivitySleepChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private BarLineChartBase mChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);

        mChart = (BarLineChartBase) rootView.findViewById(R.id.activitysleepchart);

        setupChart();

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.activity_sleepchart_activity_and_sleep);
    }

    private void setupChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.setDescriptionColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mChart);


        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaxValue(1f);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ChartsHost.REFRESH)) {
            // TODO: use LimitLines to visualize smart alarms?
            mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
            mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
            mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
            mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected void refreshInBackground(DBHandler db, GBDevice device) {
        List<ActivitySample> samples = getSamples(db, device);
        refresh(device, mChart, samples);

        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    protected void renderCharts() {
        mChart.animateX(ANIM_TIME, Easing.EasingOption.EaseInOutQuart);
    }

    protected void setupLegend(Chart chart) {
        List<Integer> legendColors = new ArrayList<>(4);
        List<String> legendLabels = new ArrayList<>(4);
        legendColors.add(akActivity.color);
        legendLabels.add(akActivity.label);
        legendColors.add(akLightSleep.color);
        legendLabels.add(akLightSleep.label);
        legendColors.add(akDeepSleep.color);
        legendLabels.add(akDeepSleep.label);
        legendColors.add(akNotWorn.color);
        legendLabels.add(akNotWorn.label);
        chart.getLegend().setColors(legendColors);
        chart.getLegend().setLabels(legendLabels);
    }

    @Override
    protected List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return getAllSamples(db, device, tsFrom, tsTo);
    }
}
