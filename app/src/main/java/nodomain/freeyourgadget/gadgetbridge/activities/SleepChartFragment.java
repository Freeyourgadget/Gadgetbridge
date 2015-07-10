package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.charts.SleepUtils;


public class SleepChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private BarLineChartBase mChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    private GBDevice mGBDevice = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_REFRESH)) {
                // TODO: use LimitLines to visualize smart alarms?
                mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
                mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
                mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
                mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
                refresh();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);

        mChart = (BarLineChartBase) rootView.findViewById(R.id.sleepchart);

        setupChart();

        return rootView;
    }

    private void setupChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.setDescriptionColor(DESCRIPTION_COLOR);


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

        refresh();

        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
//        mChart.getLegend().setEnabled(false);
//
//        mChart.animateXY(2000, 2000);

        // don't forget to refresh the drawing
        mChart.invalidate();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void refresh() {
        if (mGBDevice == null) {
            return;
        }

//        ArrayList<GBActivitySample> samples = getTestSamples(mGBDevice, -1, -1);
        ArrayList<GBActivitySample> samples = getSleepSamples(mGBDevice, -1, -1);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        LOG.info("number of samples:" + samples.size());
        if (samples.size() > 1) {
            float movement_divisor;
            boolean annotate = true;
            boolean use_steps_as_movement;
            switch (getProvider(mGBDevice)) {
                case GBActivitySample.PROVIDER_MIBAND:
                    movement_divisor = 256.0f;
                    use_steps_as_movement = true;
                    break;
                default: // Morpheuz
                    movement_divisor = 5000.0f;
                    use_steps_as_movement = false;
                    break;
            }

            byte last_type = GBActivitySample.TYPE_UNKNOWN;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            SimpleDateFormat annotationDateFormat = new SimpleDateFormat("HH:mm");

            int numEntries = samples.size();
            List<String> xLabels = new ArrayList<>(numEntries);
//            List<BarEntry> deepSleepEntries = new ArrayList<>(numEntries / 4);
//            List<BarEntry> lightSleepEntries = new ArrayList<>(numEntries / 4);
            List<BarEntry> activityEntries = new ArrayList<>(numEntries);
            List<Integer> colors = new ArrayList<>(numEntries); // this is kinda inefficient...

            for (int i = 0; i < numEntries; i++) {
                GBActivitySample sample = samples.get(i);
                byte type = sample.getType();

                // determine start and end dates
                if (i == 0) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // make sure it's converted to long
                    date = cal.getTime();
                    dateStringFrom = dateFormat.format(date);
                } else if (i == samples.size() - 1) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // same here
                    date = cal.getTime();
                    dateStringTo = dateFormat.format(date);
                }

                short movement = sample.getIntensity();

                float value;
                if (type == GBActivitySample.TYPE_DEEP_SLEEP) {
//                    value = Y_VALUE_DEEP_SLEEP;
                    value = ((float) movement) / movement_divisor;
                    value += SleepUtils.Y_VALUE_DEEP_SLEEP;
                    activityEntries.add(createBarEntry(value, i));
                    colors.add(akDeepSleep.color);
                } else {
                    if (type == GBActivitySample.TYPE_LIGHT_SLEEP) {
                        value = ((float) movement) / movement_divisor;
//                        value += SleepUtils.Y_VALUE_LIGHT_SLEEP;
//                        value = Math.min(1.0f, Y_VALUE_LIGHT_SLEEP);
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akLightSleep.color);
                    } else {
                        byte steps = sample.getSteps();
                        if (use_steps_as_movement && steps != 0) {
                            // I'm not sure using steps for this is actually a good idea
                            movement = steps;
                        }
                        value = ((float) movement) / movement_divisor;
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akActivity.color);
                    }
                }

                String xLabel = "";
                if (annotate) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    String dateString = annotationDateFormat.format(date);
                    xLabel = dateString;
//                    if (last_type != type) {
//                        if (isSleep(last_type) && !isSleep(type)) {
//                            // woken up
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextColor(Color.WHITE);
//                            line.setTextSize(15);
//                            mChart.getXAxis().addLimitLine(line);
//                        } else if (!isSleep(last_type) && isSleep(type)) {
//                            // fallen asleep
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextSize(15);
//                            line.setTextColor(Color.WHITE);
//                            mChart.getXAxis().addLimitLine(line);
//                        }
//                    }
                    last_type = type;
                }
                xLabels.add(xLabel);
            }

            mChart.getXAxis().setValues(xLabels);

//            BarDataSet deepSleepSet = createDeepSleepSet(deepSleepEntries, "Deep Sleep");
//            BarDataSet lightSleepSet = createLightSleepSet(lightSleepEntries, "Light Sleep");
            BarDataSet activitySet = createActivitySet(activityEntries, colors, "Activity");

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
//            dataSets.add(deepSleepSet);
//            dataSets.add(lightSleepSet);
            dataSets.add(activitySet);

            // create a data object with the datasets
            BarData data = new BarData(xLabels, dataSets);
            data.setGroupSpace(0);

            mChart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            mChart.setDescriptionPosition(?, ?);
            // set data

            setupLegend(mChart);

            mChart.setData(data);

            mChart.animateX(500, Easing.EasingOption.EaseInOutQuart);

//            textView.setText(dateStringFrom + " to " + dateStringTo);
        }
    }

    private void setupLegend(BarLineChartBase chart) {
        List<Integer> legendColors = new ArrayList<>(3);
        List<String> legendLabels = new ArrayList<>(3);
        legendColors.add(akActivity.color);
        legendLabels.add(akActivity.label);
        legendColors.add(akLightSleep.color);
        legendLabels.add(akLightSleep.label);
        legendColors.add(akDeepSleep.color);
        legendLabels.add(akDeepSleep.label);
        chart.getLegend().setColors(legendColors);
        chart.getLegend().setLabels(legendLabels);
    }
}
