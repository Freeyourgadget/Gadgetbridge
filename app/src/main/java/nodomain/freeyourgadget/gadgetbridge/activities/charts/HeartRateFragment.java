package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;


/*
//Start Heart Rate measuring
                GB.toast("Measuring heart rate, please wait...", Toast.LENGTH_LONG, GB.INFO);
                GBApplication.deviceService().onHeartRateTest();


 */

public class HeartRateFragment extends AbstractChartFragment {
    private static final Logger LOG = LoggerFactory.getLogger(HeartRateFragment.class);
    private static final int HR_LOW = 40;
    private static final int HR_HIGH = 95;
    private static final int HR_EXHAUSTED = 130;

    private static final int MAX_HEARTRATE = 180;

    private BarEntry totalStepsEntry;
    private BarEntry stepsPerMinuteEntry;
    private BarDataSet mStepsPerMinuteData;
    private BarDataSet mTotalStepsData;
    private LineDataSet mHistorySet;
    private BarLineChartBase mHeartRateHistoryChart;
    private CustomBarChart mHeartrateCurrentChart;

    private final Heartrate mHeartrate = new Heartrate();
    private ScheduledExecutorService pulseScheduler;
    private int maxStepsResetCounter;

    private class Heartrate {

        private int heartrate = 0;
        private long timestamp;
        private long lastTimestamp;
        private int lastHeartrate;

        public int getHeartrate() {
            lastHeartrate = heartrate;
            int result = heartrate;
            return result;
        }


        public void updateHeartrate(int newHeartrate, long newTimestamp) {

                heartrate = newHeartrate;
                timestamp = newTimestamp;

        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case DeviceService.ACTION_REALTIME_HEARTRATE:
                    int heartrate = intent.getIntExtra(DeviceService.EXTRA_REALTIME_HEARTRATE, 0);
                    long timestamp = intent.getLongExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());
                    refreshHeartrate(heartrate, timestamp);
                    break;
            }
        }
    };

    private void refreshHeartrate(int heartrate, long timestamp) {
        mHeartrate.updateHeartrate(heartrate, timestamp);
    }

    private void refreshHeartrate() {
        YAxis stepsPerMinuteCurrentYAxis = mHeartrateCurrentChart.getAxisLeft();
        int maxStepsPerMinute = mHeartrate.getHeartrate();
        LimitLine target = new LimitLine(maxStepsPerMinute);
        stepsPerMinuteCurrentYAxis.removeAllLimitLines();
        stepsPerMinuteCurrentYAxis.addLimitLine(target);

        int heartrate = mHeartrate.getHeartrate();
        mHeartrateCurrentChart.setSingleEntryYValue(heartrate);

        if (mHeartRateHistoryChart.getData() == null) {
            if (mHeartrate.getHeartrate() == 0) {
                return; // ignore the first default value to keep the "no-data-description" visible
            }
            LineData data = new LineData();
            mHeartRateHistoryChart.setData(data);
            data.addDataSet(mHistorySet);
        }

        LineData historyData = (LineData) mHeartRateHistoryChart.getData();
        historyData.addXValue("");
        historyData.addEntry(new Entry(heartrate, mHistorySet.getEntryCount()), 0);

        mTotalStepsData.notifyDataSetChanged();
        mStepsPerMinuteData.notifyDataSetChanged();
        mHeartRateHistoryChart.notifyDataSetChanged();

        renderCharts();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceService.ACTION_REALTIME_HEARTRATE);

        View rootView = inflater.inflate(R.layout.fragment_live_activity, container, false);

        mHeartrateCurrentChart = (CustomBarChart) rootView.findViewById(R.id.livechart_steps_per_minute_current);
        mHeartRateHistoryChart = (BarLineChartBase) rootView.findViewById(R.id.livechart_steps_per_minute_history);

        totalStepsEntry = new BarEntry(0, 1);
        stepsPerMinuteEntry = new BarEntry(0, 1);

        mStepsPerMinuteData = setupCurrentChart(mHeartrateCurrentChart, stepsPerMinuteEntry, getString(R.string.live_activity_current_steps_per_minute));
        setupHistoryChart(mHeartRateHistoryChart);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filterLocal);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pulseScheduler != null) {
            pulseScheduler.shutdownNow();
            pulseScheduler = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        pulseScheduler = startActivityPulse();
    }

    private ScheduledExecutorService startActivityPulse() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                FragmentActivity activity = HeartRateFragment.this.getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pulse();
                        }
                    });
                }
            }
        }, 0, getPulseIntervalMillis(), TimeUnit.MILLISECONDS);
        return service;
    }

    /**
     * Called in the UI thread.
     */
    private void pulse() {
        refreshHeartrate();
    }

    private long getPulseIntervalMillis() {
        return 1000;
    }

    @Override
    protected void onMadeVisibleInActivity() {
        GBApplication.deviceService().onEnableRealtimeHeartrate(true);
        super.onMadeVisibleInActivity();
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onMadeInvisibleInActivity() {
        GBApplication.deviceService().onEnableRealtimeHeartrate(false);
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
        mHeartrateCurrentChart.getAxisLeft().setAxisMaxValue(MAX_HEARTRATE);
        return setupCommonChart(chart, entry, title);
    }

    private BarDataSet setupCommonChart(CustomBarChart chart, BarEntry entry, String title) {
        chart.setSinglAnimationEntry(entry);

//        chart.getXAxis().setPosition(XAxis.XAxisPosition.TOP);
        chart.getXAxis().setDrawLabels(false);
        chart.getXAxis().setEnabled(false);
        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription(title);
        chart.setNoDataTextDescription("");
        chart.setNoDataText("");
        chart.getAxisRight().setEnabled(false);

        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        entries.add(new BarEntry(0, 0));
        entries.add(entry);
        entries.add(new BarEntry(0, 2));
        colors.add(akActivity.color);
        colors.add(akActivity.color);
        colors.add(akActivity.color);
        //we don't want labels
        xLabels.add("");
        xLabels.add("");
        xLabels.add("");

        BarDataSet set = new BarDataSet(entries, "");
        set.setDrawValues(false);
        set.setColors(colors);
        BarData data = new BarData(xLabels, set);
        data.setGroupSpace(0);
        chart.setData(data);

        chart.getLegend().setEnabled(false);

        return set;
    }

    private BarDataSet setupTotalStepsChart(CustomBarChart chart, BarEntry entry, String label) {
        return setupCommonChart(chart, entry, label); // at the moment, these look the same
    }

    private void setupHistoryChart(BarLineChartBase chart) {
        configureBarLineChartDefaults(chart);

        chart.setBackgroundColor(BACKGROUND_COLOR);
        chart.setDescriptionColor(DESCRIPTION_COLOR);
        chart.setDescription(getString(R.string.live_activity_steps_per_minute_history));
        chart.setNoDataText(getString(R.string.live_activity_start_your_activity));
        chart.getLegend().setEnabled(false);
        Paint infoPaint = chart.getPaint(Chart.PAINT_INFO);
        infoPaint.setTextSize(Utils.convertDpToPixel(20f));
        infoPaint.setFakeBoldText(true);
        chart.setPaint(infoPaint, Chart.PAINT_INFO);

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

        mHistorySet = new LineDataSet(new ArrayList<Entry>(), getString(R.string.live_activity_steps_history));
        mHistorySet.setColor(akActivity.color);
        mHistorySet.setDrawCircles(false);
        mHistorySet.setDrawCubic(true);
        mHistorySet.setDrawFilled(true);
        mHistorySet.setDrawValues(false);
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
        mHeartrateCurrentChart.animateY(150);
        mHeartRateHistoryChart.invalidate();
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
