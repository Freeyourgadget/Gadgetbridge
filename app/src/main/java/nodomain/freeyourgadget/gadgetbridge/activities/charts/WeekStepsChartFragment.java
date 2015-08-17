package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

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

    private BarLineChartBase mWeekStepsChart;
    private PieChart mTodayStepsChart;

    private GBDevice mGBDevice = null;

    @Override
    protected void refreshInBackground(DBHandler db) {
        Calendar day = Calendar.getInstance();
        //NB: we could have omitted the day, but this way we can move things to the past easily
        refreshDaySteps(db, mTodayStepsChart, day);
        refreshWeekBeforeSteps(db, mWeekStepsChart, day);
    }

    @Override
    protected void renderCharts() {
        mWeekStepsChart.invalidate();
        mTodayStepsChart.invalidate();
    }

    private void refreshWeekBeforeSteps(DBHandler db, BarLineChartBase barChart, Calendar day) {

        ActivityAnalysis analysis = new ActivityAnalysis();

        day.add(Calendar.DATE, -7);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int counter = 0; counter < 7; counter++) {
            entries.add(new BarEntry(analysis.calculateTotalSteps(getSamplesOfDay(db, day)), counter));
            labels.add(day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale));
            day.add(Calendar.DATE, 1);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColor(akActivity.color);

        BarData data = new BarData(labels, set);
        data.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);

        LimitLine target = new LimitLine(mTargetSteps);

        barChart.getAxisLeft().addLimitLine(target);

        setupLegend(barChart);
        barChart.setData(data);
        barChart.getLegend().setEnabled(false);
    }

    private void refreshDaySteps(DBHandler db, PieChart pieChart, Calendar day) {
        ActivityAnalysis analysis = new ActivityAnalysis();

        int totalSteps = analysis.calculateTotalSteps(getSamplesOfDay(db, day));

        pieChart.setCenterText(NumberFormat.getNumberInstance(mLocale).format(totalSteps));
        PieData data = new PieData();
        List<Entry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        entries.add(new Entry(totalSteps, 0));
        colors.add(akActivity.color);
        //we don't want labels on the pie chart
        data.addXValue("");

        if (totalSteps < mTargetSteps) {
            entries.add(new Entry((mTargetSteps - totalSteps), 1));
            colors.add(Color.GRAY);
            //we don't want labels on the pie chart
            data.addXValue("");
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        data.setDataSet(set);
        //this hides the values (numeric) added to the set. These would be shown aside the strings set with addXValue above
        data.setDrawValues(false);
        pieChart.setData(data);

        pieChart.getLegend().setEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;

        View rootView = inflater.inflate(R.layout.fragment_sleepchart, container, false);

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }

        if(mGBDevice != null) {
            mTargetSteps = MiBandCoordinator.getFitnessGoal(mGBDevice.getAddress());
        }

        mWeekStepsChart = (BarLineChartBase) rootView.findViewById(R.id.sleepchart);
        mTodayStepsChart = (PieChart) rootView.findViewById(R.id.sleepchart_pie_light_deep);

        setupWeekStepsChart();
        setupTodayStepsChart();

        refresh();

        return rootView;
    }

    private void setupTodayStepsChart() {
        mTodayStepsChart.setBackgroundColor(BACKGROUND_COLOR);
        mTodayStepsChart.setDescriptionColor(DESCRIPTION_COLOR);
        mTodayStepsChart.setDescription("Steps today, target: " + mTargetSteps);
        mTodayStepsChart.setNoDataTextDescription("");
        mTodayStepsChart.setNoDataText("");
    }

    private void setupWeekStepsChart() {
        mWeekStepsChart.setBackgroundColor(BACKGROUND_COLOR);
        mWeekStepsChart.setDescriptionColor(DESCRIPTION_COLOR);
        mWeekStepsChart.setDescription("");

        configureBarLineChartDefaults(mWeekStepsChart);

        XAxis x = mWeekStepsChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

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

    protected void setupLegend(Chart chart) {
        List<Integer> legendColors = new ArrayList<>(1);
        List<String> legendLabels = new ArrayList<>(1);
        legendColors.add(akActivity.color);
        legendLabels.add("Steps");
        chart.getLegend().setColors(legendColors);
        chart.getLegend().setLabels(legendLabels);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    private List<ActivitySample> getSamplesOfDay(DBHandler db, Calendar day) {
        int startTs;
        int endTs;

        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        startTs = (int) (day.getTimeInMillis() / 1000);

        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        endTs = (int) (day.getTimeInMillis() / 1000);

        return getSamples(db, mGBDevice, startTs, endTs);
    }

    @Override
    protected List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }
}
