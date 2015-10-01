package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LiveActivityFragment extends AbstractChartFragment {
    private static final Logger LOG = LoggerFactory.getLogger(LiveActivityFragment.class);
    private BarEntry totalStepsEntry;
    private BarEntry stepsPerMinuteEntry;
    private BarDataSet mStepsPerMinuteData;
    private BarDataSet mTotalStepsData;
    private LineDataSet mHistorySet = new LineDataSet(new ArrayList<Entry>(), "History");

    private class Steps {
        private int initialSteps;

        private int steps;
        private long lastTimestamp;
        private int currentStepsPerMinute;
        private int maxStepsPerMinute;

        public int getStepsPerMinute() {
            return currentStepsPerMinute;
        }

        public int getTotalSteps() {
            return steps - initialSteps;
        }

        public int getMaxStepsPerMinute() {
            return maxStepsPerMinute;
        }

        public void updateCurrentSteps(int newSteps, long timestamp) {
            try {
                if (steps == 0) {
                    steps = newSteps;
                    lastTimestamp = timestamp;

                    if (newSteps > 0) {
                        initialSteps = newSteps;
                    }
                    return;
                }

                if (newSteps >= steps) {
                    int stepsDelta = newSteps - steps;
                    long timeDelta = timestamp - lastTimestamp;
                    currentStepsPerMinute = calculateStepsPerMinute(stepsDelta, timeDelta);
                    maxStepsPerMinute = Math.max(maxStepsPerMinute, currentStepsPerMinute);
                    steps = newSteps;
                    lastTimestamp = timestamp;
                } else {
                    // TODO: handle new day?

                }
            } catch (Exception ex) {
                GB.toast(LiveActivityFragment.this.getContext(), ex.getMessage(), Toast.LENGTH_SHORT, GB.ERROR, ex);
            }
        }

        private int calculateStepsPerMinute(int stepsDelta, long millis) {
            if (stepsDelta == 0) {
                return 0; // not walking or not enough data per mills?
            }
            if (millis <= 0) {
                throw new IllegalArgumentException("delta in millis is <= 0 -- time change?");
            }

            long oneMinute = 60 * 1000;
            float factor = oneMinute / millis;
            return (int) (stepsDelta * factor);
        }
    }


    private BarLineChartBase mStepsPerMinuteHistoryChart;
    private CustomBarChart mStepsPerMinuteCurrentChart;
    private CustomBarChart mTotalStepsChart;

    private Steps mSteps = new Steps();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case DeviceService.ACTION_REALTIME_STEPS:
                    int steps = intent.getIntExtra(DeviceService.EXTRA_REALTIME_STEPS, 0);
                    long timestamp = intent.getLongExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());
                    refreshCurrentSteps(steps, timestamp);
                    break;
            }
        }
    };

    private void refreshCurrentSteps(int steps, long timestamp) {
        mSteps.updateCurrentSteps(steps, timestamp);
        // Or: count down the steps until goal reached? And then flash GOAL REACHED -> Set stretch goal
        mTotalStepsChart.setSingleEntryYValue(mSteps.getTotalSteps());
//        totalStepsEntry.setVal(mSteps.getTotalSteps());
        LOG.info("Steps: " + steps + ", total: " + mSteps.getTotalSteps() + ", current: " + mSteps.getStepsPerMinute());
//        mTotalStepsChart.setCenterText(NumberFormat.getNumberInstance().format(mSteps.getTotalSteps()));
        mStepsPerMinuteCurrentChart.getAxisLeft().setAxisMaxValue(mSteps.getMaxStepsPerMinute());
        mStepsPerMinuteCurrentChart.setSingleEntryYValue(mSteps.getStepsPerMinute());
//        stepsPerMinuteEntry.setVal(mSteps.getStepsPerMinute());
//        mStepsPerMinuteCurrentChart.setCenterText(NumberFormat.getNumberInstance().format(mSteps.getStepsPerMinute()));
        mStepsPerMinuteHistoryChart.getData().addEntry(new Entry(mSteps.getStepsPerMinute(), mHistorySet.getEntryCount()), 0);
        mStepsPerMinuteHistoryChart.getData().addXValue("");
        mHistorySet.addColor(akActivity.color);


        mTotalStepsChart.getData().notifyDataChanged();
        mTotalStepsData.notifyDataSetChanged();
        mStepsPerMinuteCurrentChart.getData().notifyDataChanged();
        mStepsPerMinuteData.notifyDataSetChanged();
        mStepsPerMinuteHistoryChart.getData().notifyDataChanged();
        mHistorySet.notifyDataSetChanged();

        renderCharts();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceService.ACTION_REALTIME_STEPS);

        View rootView = inflater.inflate(R.layout.fragment_live_activity, container, false);

        mStepsPerMinuteCurrentChart = (CustomBarChart) rootView.findViewById(R.id.livechart_steps_per_minute_current);
        mTotalStepsChart = (CustomBarChart) rootView.findViewById(R.id.livechart_steps_total);
        mStepsPerMinuteHistoryChart = (BarLineChartBase) rootView.findViewById(R.id.livechart_steps_per_minute_history);

        totalStepsEntry = new BarEntry(0, 1);
        stepsPerMinuteEntry = new BarEntry(0, 1);

        mStepsPerMinuteData = setupCurrentChart(mStepsPerMinuteCurrentChart, stepsPerMinuteEntry, "Steps/min");
        mTotalStepsData = setupTotalStepsChart(mTotalStepsChart, totalStepsEntry, "Total Steps");
        setupHistoryChart(mStepsPerMinuteHistoryChart);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filterLocal);

        return rootView;
    }

    @Override
    protected void onMadeVisibleInActivity() {
        GBApplication.deviceService().onEnableRealtimeSteps(true);
        super.onMadeVisibleInActivity();
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onMadeInvisibleInActivity() {
        GBApplication.deviceService().onEnableRealtimeSteps(false);
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onMadeInvisibleInActivity();
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    private BarDataSet setupCurrentChart(CustomBarChart chart, BarEntry entry, String title) {
        mStepsPerMinuteCurrentChart.getAxisLeft().setAxisMaxValue(300);
        return setupCommonChart(chart, entry, title);
    }

    private BarDataSet setupCommonChart(CustomBarChart chart, BarEntry entry, String title) {
        chart.setSinglAnimationEntry(entry);

        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription(title);
        chart.setNoDataTextDescription("");
        chart.setNoDataText("");
        chart.getAxisRight().setEnabled(false);
//        chart.setDrawSliceText(false);

        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

//        int value = 0;
//        chart.setCenterText(NumberFormat.getNumberInstance().format(value));
        entries.add(new BarEntry(0,0));
        entries.add(entry);
        entries.add(new BarEntry(0,2));
        colors.add(akActivity.color);
        colors.add(akActivity.color);
        colors.add(akActivity.color);
        //we don't want labels on the pie chart
        xLabels.add("");
        xLabels.add("");
        xLabels.add("");

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(colors);
        BarData data = new BarData(xLabels, set);
        data.setGroupSpace(0);
        //this hides the values (numeric) added to the set. These would be shown aside the strings set with addXValue above
//        data.setDrawValues(false);
        chart.setData(data);

        chart.getLegend().setEnabled(false);

        return set;
    }

    private BarDataSet setupTotalStepsChart(CustomBarChart chart, BarEntry entry, String label) {
        mTotalStepsChart.getAxisLeft().setAxisMaxValue(5000); // TODO: use daily goal - already reached steps
        return setupCommonChart(chart, entry, label); // at the moment, these look the same
    }

    private void setupHistoryChart(BarLineChartBase chart) {
        configureBarLineChartDefaults(chart);

        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription("");
        chart.setNoDataText("Start your activity");

        XAxis x = chart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = chart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

        y.setEnabled(true);

        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);

        LineData data = new LineData();
        data.addDataSet(mHistorySet);
        // add dummy value because we get ArrayIndexOutOfBoundsExceptions without ;(
        mHistorySet.addEntry(new Entry(0, 0));
        mHistorySet.addColor(akActivity.color);
        data.addXValue("");
        chart.setData(data);


    }

    @Override
    public String getTitle() {
        return getContext().getString(R.string.liveactivity_live_activity);
    }

    @Override
    protected void showDateBar(boolean show) {
        // never show the data bar
        super.showDateBar(false);
    }

    @Override
    protected void refresh() {
        // do nothing, we don't have any db interaction
    }

    @Override
    protected void refreshInBackground(DBHandler db, GBDevice device) {
    }

    @Override
    protected void renderCharts() {
        mStepsPerMinuteCurrentChart.animateY(150);
        mTotalStepsChart.animateY(150);
        mStepsPerMinuteHistoryChart.invalidate();
    }

    @Override
    protected List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        throw new UnsupportedOperationException("no db access supported for live activity");
    }

    @Override
    protected void setupLegend(Chart chart) {
        // no legend
    }
}
