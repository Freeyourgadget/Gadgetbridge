package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class SleepChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private CombinedChart mActivityChart;
    private PieChart mSleepAmountChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples = getSamples(db, device);

        MySleepChartsData mySleepChartsData = refreshSleepAmounts(device, samples);
        DefaultChartsData chartsData = refresh(device, samples);

        return new MyChartsData(mySleepChartsData, chartsData);
    }

    private MySleepChartsData refreshSleepAmounts(GBDevice mGBDevice, List<? extends ActivitySample> samples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        ActivityAmounts amounts = analysis.calculateActivityAmounts(samples);
        PieData data = new PieData();
        List<Entry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;
        long totalSeconds = 0;
        for (ActivityAmount amount : amounts.getAmounts()) {
            if ((amount.getActivityKind() & ActivityKind.TYPE_SLEEP) != 0) {
                long value = amount.getTotalSeconds();
                totalSeconds += value;
                entries.add(new Entry(value, index++));
                colors.add(getColorFor(amount.getActivityKind()));
                data.addXValue(amount.getName(getActivity()));
            }
        }
        String totalSleep = DateTimeUtils.formatDurationHoursMinutes(totalSeconds, TimeUnit.SECONDS);
        PieDataSet set = new PieDataSet(entries, "");
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
            }
        });
        set.setColors(colors);
        data.setDataSet(set);

        //setupLegend(pieChart);
        return new MySleepChartsData(totalSleep, data);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;
        mSleepAmountChart.setCenterText(mcd.getPieData().getTotalSleep());
        mSleepAmountChart.setData(mcd.getPieData().getPieData());

        mActivityChart.setData(mcd.getChartsData().getCombinedData());
    }

    @Override
    public String getTitle() {
        return getString(R.string.sleepchart_your_sleep);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleepchart, container, false);

        mActivityChart = (CombinedChart) rootView.findViewById(R.id.sleepchart);
        mSleepAmountChart = (PieChart) rootView.findViewById(R.id.sleepchart_pie_light_deep);

        setupActivityChart();
        setupSleepAmountChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
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

    private void setupSleepAmountChart() {
        mSleepAmountChart.setBackgroundColor(BACKGROUND_COLOR);
        mSleepAmountChart.setDescriptionColor(DESCRIPTION_COLOR);
        mSleepAmountChart.setDescription("");
        mSleepAmountChart.setNoDataTextDescription("");
        mSleepAmountChart.setNoDataText("");
        mSleepAmountChart.getLegend().setEnabled(false);
    }

    private void setupActivityChart() {
        mActivityChart.setBackgroundColor(BACKGROUND_COLOR);
        mActivityChart.setDescriptionColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mActivityChart);

        XAxis x = mActivityChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mActivityChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaxValue(1f);
        y.setAxisMinValue(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mActivityChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(supportsHeartrate(getChartsHost().getDevice()));
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaxValue(HeartRateUtils.MAX_HEART_RATE_VALUE);
        yAxisRight.setAxisMinValue(HeartRateUtils.MIN_HEART_RATE_VALUE);
    }

    protected void setupLegend(Chart chart) {
        List<Integer> legendColors = new ArrayList<>(2);
        List<String> legendLabels = new ArrayList<>(2);
        legendColors.add(akLightSleep.color);
        legendLabels.add(akLightSleep.label);
        legendColors.add(akDeepSleep.color);
        legendLabels.add(akDeepSleep.label);
        if (supportsHeartrate(getChartsHost().getDevice())) {
            legendColors.add(HEARTRATE_COLOR);
            legendLabels.add(HEARTRATE_LABEL);
        }
        chart.getLegend().setCustom(legendColors, legendLabels);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
// temporary fix for totally wrong sleep amounts
//        return super.getSleepSamples(db, device, tsFrom, tsTo);
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    protected void renderCharts() {
        mActivityChart.animateX(ANIM_TIME, Easing.EasingOption.EaseInOutQuart);
        mSleepAmountChart.invalidate();
    }

    private static class MySleepChartsData extends ChartsData {
        private String totalSleep;
        private final PieData pieData;

        public MySleepChartsData(String totalSleep, PieData pieData) {
            this.totalSleep = totalSleep;
            this.pieData = pieData;
        }

        public PieData getPieData() {
            return pieData;
        }

        public CharSequence getTotalSleep() {
            return totalSleep;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final DefaultChartsData chartsData;
        private final MySleepChartsData pieData;

        public MyChartsData(MySleepChartsData pieData, DefaultChartsData chartsData) {
            this.pieData = pieData;
            this.chartsData = chartsData;
        }

        public MySleepChartsData getPieData() {
            return pieData;
        }

        public DefaultChartsData getChartsData() {
            return chartsData;
        }
    }
}