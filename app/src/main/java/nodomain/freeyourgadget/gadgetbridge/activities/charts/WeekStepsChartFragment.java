package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


public class WeekStepsChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(WeekStepsChartFragment.class);

    private Locale mLocale;
    private int mTargetSteps = 10000;

    private PieChart mTodayStepsChart;
    private CombinedChart mWeekStepsChart;
    private XIndexLabelFormatter xIndexLabelFormatter = new XIndexLabelFormatter();

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        //NB: we could have omitted the day, but this way we can move things to the past easily
        DaySteps daySteps = refreshDaySteps(db, day, device);
        DefaultChartsData weekBeforeStepsData = refreshWeekBeforeSteps(db, mWeekStepsChart, day, device);

        return new MyChartsData(daySteps, weekBeforeStepsData);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;

//        setupLegend(mWeekStepsChart);
        mTodayStepsChart.setCenterText(NumberFormat.getNumberInstance(mLocale).format(mcd.getDaySteps().totalSteps));
        mTodayStepsChart.setData(mcd.getDaySteps().data);

        mWeekStepsChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mWeekStepsChart.setData(mcd.getWeekBeforeStepsData().getCombinedData());
        mWeekStepsChart.getLegend().setEnabled(false);
    }

    @Override
    protected void renderCharts() {
        mWeekStepsChart.invalidate();
        mTodayStepsChart.invalidate();
    }

    private DefaultChartsData refreshWeekBeforeSteps(DBHandler db, CombinedChart combinedChart, Calendar day, GBDevice device) {

        ActivityAnalysis analysis = new ActivityAnalysis();

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -7);
        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<String>();

        for (int counter = 0; counter < 7; counter++) {
            entries.add(new BarEntry(counter, analysis.calculateTotalSteps(getSamplesOfDay(db, day, device))));
            labels.add(day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale));
            day.add(Calendar.DATE, 1);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColor(akActivity.color);

        xIndexLabelFormatter.setxLabels(labels);
        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);

        LimitLine target = new LimitLine(mTargetSteps);
        combinedChart.getAxisLeft().removeAllLimitLines();
        combinedChart.getAxisLeft().addLimitLine(target);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        return new DefaultChartsData(combinedData);
    }



    private DaySteps refreshDaySteps(DBHandler db, Calendar day, GBDevice device) {
        ActivityAnalysis analysis = new ActivityAnalysis();

        int totalSteps = analysis.calculateTotalSteps(getSamplesOfDay(db, day, device));

        PieData data = new PieData();
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        entries.add(new PieEntry(totalSteps, "")); //we don't want labels on the pie chart
        colors.add(akActivity.color);

        if (totalSteps < mTargetSteps) {
            entries.add(new PieEntry((mTargetSteps - totalSteps))); //we don't want labels on the pie chart
            colors.add(Color.GRAY);
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        data.setDataSet(set);
        //this hides the values (numeric) added to the set. These would be shown aside the strings set with addXValue above
        data.setDrawValues(false);

        return new DaySteps(data, totalSteps);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;

        View rootView = inflater.inflate(R.layout.fragment_sleepchart, container, false);

        GBDevice device = getChartsHost().getDevice();
        if (device != null) {
            // TODO: eek, this is device specific!
            mTargetSteps = MiBandCoordinator.getFitnessGoal(device.getAddress());
        }

        mWeekStepsChart = (CombinedChart) rootView.findViewById(R.id.sleepchart);
        mTodayStepsChart = (PieChart) rootView.findViewById(R.id.sleepchart_pie_light_deep);

        setupWeekStepsChart();
        setupTodayStepsChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.weekstepschart_steps_a_week);
    }

    private void setupTodayStepsChart() {
        mTodayStepsChart.setBackgroundColor(BACKGROUND_COLOR);
        mTodayStepsChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mTodayStepsChart.getDescription().setText(getContext().getString(R.string.weeksteps_today_steps_description, String.valueOf(mTargetSteps)));
//        mTodayStepsChart.setNoDataTextDescription("");
        mTodayStepsChart.setNoDataText("");
        mTodayStepsChart.getLegend().setEnabled(false);
//        setupLegend(mTodayStepsChart);
    }

    private void setupWeekStepsChart() {
        mWeekStepsChart.setBackgroundColor(BACKGROUND_COLOR);
        mWeekStepsChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mWeekStepsChart.getDescription().setText("");

        configureBarLineChartDefaults(mWeekStepsChart);

        XAxis x = mWeekStepsChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setValueFormatter(xIndexLabelFormatter);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis y = mWeekStepsChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

        y.setEnabled(true);

        YAxis yAxisRight = mWeekStepsChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
    }

    @Override
    protected void setupLegend(Chart chart) {
//        List<Integer> legendColors = new ArrayList<>(1);
//        List<String> legendLabels = new ArrayList<>(1);
//        legendColors.add(akActivity.color);
//        legendLabels.add(getContext().getString(R.string.chart_steps));
//        chart.getLegend().setCustom(legendColors, legendLabels);
//        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    private List<? extends ActivitySample> getSamplesOfDay(DBHandler db, Calendar day, GBDevice device) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        startTs = (int) (day.getTimeInMillis() / 1000);

        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        endTs = (int) (day.getTimeInMillis() / 1000);

        return getSamples(db, device, startTs, endTs);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    private static class DaySteps {
        private final PieData data;
        private final int totalSteps;

        public DaySteps(PieData data, int totalSteps) {
            this.data = data;
            this.totalSteps = totalSteps;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final DefaultChartsData weekBeforeStepsData;
        private final DaySteps daySteps;

        public MyChartsData(DaySteps daySteps, DefaultChartsData weekBeforeStepsData) {
            this.daySteps = daySteps;
            this.weekBeforeStepsData = weekBeforeStepsData;
        }

        public DaySteps getDaySteps() {
            return daySteps;
        }

        public DefaultChartsData getWeekBeforeStepsData() {
            return weekBeforeStepsData;
        }
    }
}
