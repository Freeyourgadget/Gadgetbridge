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
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class SleepChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private BarLineChartBase mActivityChart;
    private PieChart mSleepAmountChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    protected void refreshInBackground(DBHandler db, GBDevice device) {
        List<ActivitySample> samples = getSamples(db, device);

        refresh(device, mActivityChart, samples);
        refreshSleepAmounts(device, mSleepAmountChart, samples);
    }

    private void refreshSleepAmounts(GBDevice mGBDevice, PieChart pieChart, List<ActivitySample> samples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        ActivityAmounts amounts = analysis.calculateActivityAmounts(samples);
        String totalSleep = DateTimeUtils.formatDurationHoursMinutes(amounts.getTotalSeconds(), TimeUnit.SECONDS);
        pieChart.setCenterText(totalSleep);
        PieData data = new PieData();
        List<Entry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;
        for (ActivityAmount amount : amounts.getAmounts()) {
            entries.add(new Entry(amount.getTotalSeconds(), index++));
            colors.add(getColorFor(amount.getActivityKind()));
            data.addXValue(amount.getName(getActivity()));
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
            }
        });
        set.setColors(colors);
        data.setDataSet(set);
        pieChart.setData(data);

        pieChart.getLegend().setEnabled(false);
        //setupLegend(pieChart);
    }

    @Override
    public String getTitle() {
        return getString(R.string.sleepchart_your_sleep);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleepchart, container, false);

        mActivityChart = (BarLineChartBase) rootView.findViewById(R.id.sleepchart);
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
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mActivityChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
    }

    protected void setupLegend(Chart chart) {
        List<Integer> legendColors = new ArrayList<>(2);
        List<String> legendLabels = new ArrayList<>(2);
        legendColors.add(akLightSleep.color);
        legendLabels.add(akLightSleep.label);
        legendColors.add(akDeepSleep.color);
        legendLabels.add(akDeepSleep.label);
        chart.getLegend().setCustom(legendColors, legendLabels);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return super.getSleepSamples(db, device, tsFrom, tsTo);
    }

    protected void renderCharts() {
        mActivityChart.animateX(ANIM_TIME, Easing.EasingOption.EaseInOutQuart);
        mSleepAmountChart.invalidate();
    }
}