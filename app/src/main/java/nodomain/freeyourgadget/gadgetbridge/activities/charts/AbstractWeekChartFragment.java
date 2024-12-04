/*  Copyright (C) 2017-2024 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, José Rebelo, Pavel Elagin, Petr Vaněk, a0z

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;


public abstract class AbstractWeekChartFragment extends AbstractActivityChartFragment<AbstractWeekChartFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractWeekChartFragment.class);
    protected int TOTAL_DAYS = getRangeDays();
    protected int TOTAL_DAYS_FOR_AVERAGE = 0;

    protected Locale mLocale;
    protected int mTargetValue = 0;

    protected BarChart mWeekChart;
    protected TextView mBalanceView;

    private int mOffsetHours = getOffsetHours();

    protected String getWeeksChartsLabel(Calendar day){
        if (TOTAL_DAYS > 7) {
            //month, show day date
            return String.valueOf(day.get(Calendar.DAY_OF_MONTH));
        }
        else{
            //week, show short day name
            return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale);
        }
    }
    protected WeekChartsData<BarData> refreshWeekBeforeData(DBHandler db, BarChart barChart, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        List<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<String>();

        long balance = 0;
        long daily_balance = 0;
        TOTAL_DAYS_FOR_AVERAGE=0;
        List<Entry> sleepScoreEntities = new ArrayList<>();
        final List<ILineDataSet> sleepScoreDataSets = new ArrayList<>();
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            // Sleep stages
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            daily_balance=calculateBalance(amounts);
            if (daily_balance > 0) {
                TOTAL_DAYS_FOR_AVERAGE++;
            }
            balance += daily_balance;
            entries.add(new BarEntry(counter, getTotalsForActivityAmounts(amounts)));
            labels.add(getWeeksChartsLabel(day));
            // Sleep score
            if (supportsSleepScore()) {
                List<? extends SleepScoreSample> sleepScoreSamples = getSleepScoreSamples(db, device, day);
                if (!sleepScoreSamples.isEmpty() && sleepScoreSamples.get(0).getSleepScore() > 0) {
                    sleepScoreEntities.add(new Entry(counter, sleepScoreSamples.get(0).getSleepScore()));
                } else {
                    if (!sleepScoreEntities.isEmpty()) {
                        List<Entry> clone = new ArrayList<>(sleepScoreEntities.size());
                        clone.addAll(sleepScoreEntities);
                        sleepScoreDataSets.add(createSleepScoreDataSet(clone));
                        sleepScoreEntities.clear();
                    }
                }
            }
            day.add(Calendar.DATE, 1);
        }
        if (!sleepScoreEntities.isEmpty()) {
            sleepScoreDataSets.add(createSleepScoreDataSet(sleepScoreEntities));
        }
        final LineData sleepScoreLineData = new LineData(sleepScoreDataSets);
        sleepScoreLineData.setHighlightEnabled(false);

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(getColors());
        set.setValueFormatter(getBarValueFormatter());

        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);
        barData.setValueTextSize(10f);

        barChart.getAxisLeft().setAxisMaximum(Math.max(set.getYMax(), mTargetValue) + 60);

        LimitLine target = new LimitLine(mTargetValue);
        target.setLineWidth(1.5f);
        target.enableDashedLine(15f, 10f, 0f);
        target.setLineColor(getResources().getColor(R.color.chart_deep_sleep_dark));
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.getAxisLeft().addLimitLine(target);

        float average = 0;
        if (TOTAL_DAYS_FOR_AVERAGE > 0) {
            average = Math.abs(balance / TOTAL_DAYS_FOR_AVERAGE);
        }
        LimitLine average_line = new LimitLine(average);
        average_line.setLineWidth(1.5f);
        average_line.enableDashedLine(15f, 10f, 0f);
        average_line.setLabel(getString(R.string.average, getAverage(average)));

        if (average > (mTargetValue)) {
            average_line.setLineColor(Color.GREEN);
            average_line.setTextColor(Color.GREEN);
        }
        else {
            average_line.setLineColor(Color.RED);
            average_line.setTextColor(Color.RED);
        }
        if (average > 0) {
            if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
                barChart.getAxisLeft().addLimitLine(average_line);
            }
        }

        if (supportsSleepScore()) {
            return new WeekChartsData(barData, new PreformattedXIndexLabelFormatter(labels), getBalanceMessage(balance, mTargetValue), sleepScoreLineData);
        }
        return new WeekChartsData(barData, new PreformattedXIndexLabelFormatter(labels), getBalanceMessage(balance, mTargetValue));
    }

    protected List<SleepScoreSample> getSleepScoreSamples(DBHandler db, GBDevice device, Calendar day) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        TimeSampleProvider<? extends SleepScoreSample> provider = device.getDeviceCoordinator().getSleepScoreProvider(device, db.getDaoSession());
        return (List<SleepScoreSample>) provider.getAllSamples(startTs * 1000L, endTs * 1000L);
    }

    protected LineDataSet createSleepScoreDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.sleep_score));
        lineDataSet.setColor(getResources().getColor(R.color.chart_light_sleep_light));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setCircleColor(getResources().getColor(R.color.chart_light_sleep_light));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setValueTextColor(CHART_TEXT_COLOR);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;

        View rootView = inflater.inflate(R.layout.fragment_weeksteps_chart, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        final int goal = getGoal();
        if (goal >= 0) {
            mTargetValue = goal;
        }

        mWeekChart = rootView.findViewById(R.id.weekstepschart);
        mBalanceView = rootView.findViewById(R.id.balance);

        setupWeekChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    protected void setupWeekChart() {
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
        y.setValueFormatter(getYAxisFormatter());
        y.setEnabled(true);

        YAxis yAxisRight = mWeekChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
    }

    private List<? extends ActivitySample> getSamplesOfDay(DBHandler db, Calendar day, int offsetHours, GBDevice device) {
        int startTs;
        int endTs;

        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, offsetHours);

        startTs = (int) (day.getTimeInMillis() / 1000);
        endTs = startTs + 24 * 60 * 60 - 1;

        return getSamples(db, device, startTs, endTs);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    protected static class MyChartsData extends ChartsData {
        private final WeekChartsData<BarData> weekBeforeData;

        MyChartsData(WeekChartsData<BarData> weekBeforeData) {
            this.weekBeforeData = weekBeforeData;
        }

        WeekChartsData<BarData> getWeekBeforeData() {
            return weekBeforeData;
        }
    }

    protected ActivityAmounts getActivityAmountsForDay(DBHandler db, Calendar day, GBDevice device) {

        LimitedQueue<Integer, ActivityAmounts> activityAmountCache = null;
        ActivityAmounts amounts = null;

        Activity activity = getActivity();
        int key = (int) (day.getTimeInMillis() / 1000) + (mOffsetHours * 3600);
        if (activity != null) {
            activityAmountCache = ((ActivityChartsActivity) activity).mActivityAmountCache;
            amounts = activityAmountCache.lookup(key);
        }

        if (amounts == null) {
            ActivityAnalysis analysis = new ActivityAnalysis();
            amounts = analysis.calculateActivityAmounts(getSamplesOfDay(db, day, mOffsetHours, device));
            if (activityAmountCache != null) {
                activityAmountCache.add(key, amounts);
            }
        }

        return amounts;
    }

    private int getRangeDays(){
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return 30;}
        else{
            return 7;
        }
    }

    public boolean supportsSleepScore() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsSleepScore();
    }

    abstract String getAverage(float value);

    abstract int getGoal();

    abstract int getOffsetHours();

    abstract float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts);

    abstract String formatPieValue(long value);

    abstract String[] getPieLabels();

    abstract ValueFormatter getPieValueFormatter();

    abstract ValueFormatter getBarValueFormatter();

    abstract ValueFormatter getYAxisFormatter();

    abstract int[] getColors();

    abstract String getPieDescription(int targetValue);

    protected abstract long calculateBalance(ActivityAmounts amounts);

    protected abstract String getBalanceMessage(long balance, int targetValue);

    protected class WeekChartsData<T extends ChartData<?>> extends DefaultChartsData<T> {
        private final String balanceMessage;
        private LineData sleepScoresLineData;

        public WeekChartsData(T data, PreformattedXIndexLabelFormatter xIndexLabelFormatter, String balanceMessage) {
            super(data, xIndexLabelFormatter);
            this.balanceMessage = balanceMessage;
        }

        public WeekChartsData(T data, PreformattedXIndexLabelFormatter xIndexLabelFormatter, String balanceMessage, LineData sleepScores) {
            super(data, xIndexLabelFormatter);
            this.balanceMessage = balanceMessage;
            this.sleepScoresLineData = sleepScores;
        }

        public String getBalanceMessage() {
            return balanceMessage;
        }

        public LineData getSleepScoreData() { return sleepScoresLineData; }
    }
}
