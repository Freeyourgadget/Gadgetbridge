package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


public abstract class AbstractWeekChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractWeekChartFragment.class);

    private Locale mLocale;
    private int mTargetValue = 0;

    private PieChart mTodayPieChart;
    private BarChart mWeekChart;

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        //NB: we could have omitted the day, but this way we can move things to the past easily
        DayData dayData = refreshDayPie(db, day, device);
        DefaultChartsData weekBeforeData = refreshWeekBeforeData(db, mWeekChart, day, device);

        return new MyChartsData(dayData, weekBeforeData);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;

//        setupLegend(mWeekChart);
        mTodayPieChart.setCenterText(mcd.getDayData().centerText);
        mTodayPieChart.setData(mcd.getDayData().data);

        mWeekChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mWeekChart.setData(mcd.getWeekBeforeData().getData());
        mWeekChart.getLegend().setEnabled(false);
        mWeekChart.getXAxis().setValueFormatter(mcd.getWeekBeforeData().getXValueFormatter());
    }

    @Override
    protected void renderCharts() {
        mWeekChart.invalidate();
        mTodayPieChart.invalidate();
    }

    private DefaultChartsData<BarData> refreshWeekBeforeData(DBHandler db, BarChart barChart, Calendar day, GBDevice device) {

        ActivityAnalysis analysis = new ActivityAnalysis();

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -7);
        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<String>();

        for (int counter = 0; counter < 7; counter++) {
            entries.add(new BarEntry(counter, getTotalForSamples(getSamplesOfDay(db, day, device))));
            labels.add(day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale));
            day.add(Calendar.DATE, 1);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColor(getMainColor());
        set.setValueFormatter(getFormatter());

        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);

        LimitLine target = new LimitLine(mTargetValue);
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.getAxisLeft().addLimitLine(target);

        return new DefaultChartsData(barData, new PreformattedXIndexLabelFormatter(labels));
    }


    private DayData refreshDayPie(DBHandler db, Calendar day, GBDevice device) {

        int totalValue = getTotalForSamples(getSamplesOfDay(db, day, device));

        PieData data = new PieData();
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        entries.add(new PieEntry(totalValue, "")); //we don't want labels on the pie chart
        colors.add(getMainColor());

        if (totalValue < mTargetValue) {
            entries.add(new PieEntry((mTargetValue - totalValue))); //we don't want labels on the pie chart
            colors.add(Color.GRAY);
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setValueFormatter(getFormatter());
        set.setColors(colors);
        data.setDataSet(set);
        //this hides the values (numeric) added to the set. These would be shown aside the strings set with addXValue above
        data.setDrawValues(false);

        return new DayData(data, formatPieValue(totalValue));
    }

    protected abstract String formatPieValue(int value);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;

        View rootView = inflater.inflate(R.layout.fragment_weeksteps_chart, container, false);

        int goal = getGoal();
        if (goal >= 0) {
            mTargetValue = goal;
        }

        mTodayPieChart = (PieChart) rootView.findViewById(R.id.todaystepschart);
        mWeekChart = (BarChart) rootView.findViewById(R.id.weekstepschart);

        setupWeekChart();
        setupTodayPieChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    private void setupTodayPieChart() {
        mTodayPieChart.setBackgroundColor(BACKGROUND_COLOR);
        mTodayPieChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mTodayPieChart.getDescription().setText(getContext().getString(R.string.weeksteps_today_steps_description, String.valueOf(mTargetValue)));
//        mTodayPieChart.setNoDataTextDescription("");
        mTodayPieChart.setNoDataText("");
        mTodayPieChart.getLegend().setEnabled(false);
//        setupLegend(mTodayPieChart);
    }

    private void setupWeekChart() {
        mWeekChart.setBackgroundColor(BACKGROUND_COLOR);
        mWeekChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mWeekChart.getDescription().setText("");
        mWeekChart.setFitBars(true);

        configureBarLineChartDefaults(mWeekChart);

        XAxis x = mWeekChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis y = mWeekChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setDrawZeroLine(true);
        y.setSpaceBottom(0);
        y.setAxisMinimum(0);

        y.setEnabled(true);

        YAxis yAxisRight = mWeekChart.getAxisRight();
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

    private static class DayData {
        private final PieData data;
        private final CharSequence centerText;

        DayData(PieData data, String centerText) {
            this.data = data;
            this.centerText = centerText;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final DefaultChartsData<BarData> weekBeforeData;
        private final DayData dayData;

        MyChartsData(DayData dayData, DefaultChartsData<BarData> weekBeforeData) {
            this.dayData = dayData;
            this.weekBeforeData = weekBeforeData;
        }

        DayData getDayData() {
            return dayData;
        }

        DefaultChartsData<BarData> getWeekBeforeData() {
            return weekBeforeData;
        }
    }

    abstract int getGoal();

    abstract int getTotalForSamples(List<? extends ActivitySample> activitySamples);

    abstract IValueFormatter getFormatter();

    abstract Integer getMainColor();
}

