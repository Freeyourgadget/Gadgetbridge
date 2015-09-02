package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
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
    private Entry totalStepsEntry;
    private Entry stepsPerMinuteEntry;
    private PieData mStepsPerMinuteData;
    private PieData mTotalStepsData;

    private class Steps {
        private int steps;
        private long lastTimestamp;
        private int currentStepsPerMinute;

        public int getStepsPerMinute() {
            return currentStepsPerMinute;
        }

        public int getTotalSteps() {
            return steps;
        }

        public void updateCurrentSteps(int newSteps, long timestamp) {
            try {
                if (steps == 0) {
                    steps = newSteps;
                    lastTimestamp = timestamp;
                    return;
                }

                if (newSteps >= steps) {
                    int stepsDelta = newSteps - steps;
                    long timeDelta = timestamp - lastTimestamp;
                    currentStepsPerMinute = calculateStepsPerMinute(stepsDelta, timeDelta);
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
    private PieChart mStepsPerMinuteCurrentChart;
    private PieChart mStepsTotalChart;

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
        totalStepsEntry.setVal(mSteps.getTotalSteps());
        mStepsTotalChart.setCenterText(NumberFormat.getNumberInstance().format(mSteps.getTotalSteps()));
        stepsPerMinuteEntry.setVal(mSteps.getStepsPerMinute());
        mStepsPerMinuteCurrentChart.setCenterText(NumberFormat.getNumberInstance().format(mSteps.getStepsPerMinute()));

        mTotalStepsData.notifyDataChanged();
        mStepsPerMinuteData.notifyDataChanged();

        renderCharts();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceService.ACTION_REALTIME_STEPS);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filterLocal);

        View rootView = inflater.inflate(R.layout.fragment_live_activity, container, false);

        mStepsPerMinuteHistoryChart = (BarLineChartBase) rootView.findViewById(R.id.livechart_steps_per_minute_history);
        mStepsPerMinuteCurrentChart = (PieChart) rootView.findViewById(R.id.livechart_steps_per_minute_current);
        mStepsTotalChart = (PieChart) rootView.findViewById(R.id.livechart_steps_total);

        totalStepsEntry = new Entry(0, 0);
        stepsPerMinuteEntry = new Entry(0, 0);

        setupHistoryChart(mStepsPerMinuteHistoryChart);
        mStepsPerMinuteData = setupCurrentChart(mStepsPerMinuteCurrentChart, stepsPerMinuteEntry);
        mTotalStepsData = setupTotalStepsChart(mStepsTotalChart, totalStepsEntry);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        GBApplication.deviceService().onEnableRealtimeSteps(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        GBApplication.deviceService().onEnableRealtimeSteps(false);
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    private PieData setupCurrentChart(PieChart chart, Entry entry) {
        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription("");
        chart.setNoDataTextDescription("");
        chart.setNoDataText("");

        PieData data = new PieData();
        List<Entry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int value = 0;
        chart.setCenterText(NumberFormat.getNumberInstance().format(value));
        entries.add(entry);
        colors.add(akActivity.color);
        //we don't want labels on the pie chart
        data.addXValue("");

//            entries.add(new Entry((20), 1));
//            colors.add(Color.GRAY);
//            //we don't want labels on the pie chart
//            data.addXValue("");

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        data.setDataSet(set);
        //this hides the values (numeric) added to the set. These would be shown aside the strings set with addXValue above
        data.setDrawValues(false);
        chart.setData(data);

        chart.getLegend().setEnabled(false);

        return data;
    }

    private PieData setupTotalStepsChart(PieChart chart, Entry entry) {
        return setupCurrentChart(chart, entry); // at the moment, these look the same
    }


    private void setupHistoryChart(BarLineChartBase chart) {
        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription("");

        configureBarLineChartDefaults(chart);

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
    }

    @Override
    public String getTitle() {
        return getContext().getString(R.string.liveactivity_live_activity);
    }

    @Override
    protected void refreshInBackground(DBHandler db, GBDevice device) {

    }

    @Override
    protected void renderCharts() {
        mStepsPerMinuteCurrentChart.animateXY(50, 50);
        mStepsTotalChart.animateXY(50, 50);
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
