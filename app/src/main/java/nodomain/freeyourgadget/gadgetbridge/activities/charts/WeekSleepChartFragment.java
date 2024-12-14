/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo,
    Pavel Elagin, Petr Vaněk, a0z

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

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.formatter.ValueFormatter;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class WeekSleepChartFragment extends AbstractWeekChartFragment {

    private TextView awakeSleepTimeText;
    private LinearLayout awakeSleepTimeTextWrapper;
    private TextView remSleepTimeText;
    private LinearLayout remSleepTimeTextWrapper;
    private TextView deepSleepTimeText;
    private TextView lightSleepTimeText;
    private TextView sleepDatesText;
    private MySleepWeeklyData mySleepWeeklyData;
    private LinearLayout sleepScoreWrapper;
    private LineChart sleepScoreChart;

    public static WeekSleepChartFragment newInstance ( int totalDays ) {
        WeekSleepChartFragment fragmentFirst = new WeekSleepChartFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    private MySleepWeeklyData getMySleepWeeklyData(DBHandler db, Calendar day, GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);
        TOTAL_DAYS_FOR_AVERAGE=0;
        long awakeWeeklyTotal = 0;
        long remWeeklyTotal = 0;
        long deepWeeklyTotal = 0;
        long lightWeeklyTotal = 0;

        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            ActivityAmounts amounts = getActivityAmountsForDay(db, day, device);
            if (calculateBalance(amounts) > 0) {
                TOTAL_DAYS_FOR_AVERAGE++;
            }

            float[] totalAmounts = getTotalsForActivityAmounts(amounts);
            int i = 0;
            deepWeeklyTotal += (long) totalAmounts[i++];
            lightWeeklyTotal += (long) totalAmounts[i++];
            if (supportsRemSleep(getChartsHost().getDevice())) {
                remWeeklyTotal += (long) totalAmounts[i++];
            }
            if (supportsAwakeSleep(getChartsHost().getDevice())) {
                awakeWeeklyTotal += (long) totalAmounts[i++];
            }

            day.add(Calendar.DATE, 1);
        }

        return new MySleepWeeklyData(awakeWeeklyTotal, remWeeklyTotal, deepWeeklyTotal, lightWeeklyTotal);
    }

        @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;
        View rootView = inflater.inflate(R.layout.fragment_weeksleep_chart, container, false);

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
        sleepScoreWrapper = rootView.findViewById(R.id.sleep_score_wrapper);
        sleepScoreChart = rootView.findViewById(R.id.sleep_score_chart);
        remSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_rem_time);
        remSleepTimeTextWrapper = rootView.findViewById(R.id.sleep_chart_legend_rem_time_wrapper);
        awakeSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_awake_time);
        awakeSleepTimeTextWrapper = rootView.findViewById(R.id.sleep_chart_legend_awake_time_wrapper);
        deepSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_deep_time);
        lightSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_light_time);
        sleepDatesText = rootView.findViewById(R.id.sleep_dates);

        mBalanceView = rootView.findViewById(R.id.balance);

        if (!supportsSleepScore()) {
            sleepScoreWrapper.setVisibility(View.GONE);
        } else {
            setupSleepScoreChart();
        }

        setupWeekChart();
        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    protected void setupWeekChart() {
        super.setupWeekChart();
        if (TOTAL_DAYS > 7) {
            mWeekChart.setRenderer(new AngledLabelsChartRenderer(mWeekChart, mWeekChart.getAnimator(), mWeekChart.getViewPortHandler()));
        } else {
            mWeekChart.setScaleEnabled(false);
            mWeekChart.setTouchEnabled(false);
        }
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        setupLegend(mWeekChart);

        mWeekChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mWeekChart.setData(mcd.getWeekBeforeData().getData());
        mWeekChart.getXAxis().setValueFormatter(mcd.getWeekBeforeData().getXValueFormatter());
        mWeekChart.getBarData().setValueTextSize(10f);

        if (supportsSleepScore()) {
            sleepScoreChart.setData(null);
            sleepScoreChart.getXAxis().setValueFormatter(mcd.getWeekBeforeData().getXValueFormatter());
            sleepScoreChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
            sleepScoreChart.setData(mcd.getWeekBeforeData().getSleepScoreData());
        }

        // The last value is for awake time, which we do not want to include in the "total sleep time"
        final int barIgnoreLast = supportsAwakeSleep(getChartsHost().getDevice()) ? 1 : 0;
        mWeekChart.getBarData().setValueFormatter(new BarChartStackedTimeValueFormatter(false, "", 0, barIgnoreLast));

        if (TOTAL_DAYS_FOR_AVERAGE > 0) {
            float avgDeep = Math.abs(this.mySleepWeeklyData.getTotalDeep() / TOTAL_DAYS_FOR_AVERAGE);
            deepSleepTimeText.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgDeep, TimeUnit.MINUTES));
            float avgLight = Math.abs(this.mySleepWeeklyData.getTotalLight() / TOTAL_DAYS_FOR_AVERAGE);
            lightSleepTimeText.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgLight, TimeUnit.MINUTES));
            float avgRem = Math.abs(this.mySleepWeeklyData.getTotalRem() / TOTAL_DAYS_FOR_AVERAGE);
            remSleepTimeText.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgRem, TimeUnit.MINUTES));
            float avgAwake = Math.abs(this.mySleepWeeklyData.getTotalAwake() / TOTAL_DAYS_FOR_AVERAGE);
            awakeSleepTimeText.setText(DateTimeUtils.formatDurationHoursMinutes((int) avgAwake, TimeUnit.MINUTES));
        } else {
            deepSleepTimeText.setText("-");
            lightSleepTimeText.setText("-");
            remSleepTimeText.setText("-");
            awakeSleepTimeText.setText("-");
        }

        if (!supportsRemSleep(getChartsHost().getDevice())) {
            remSleepTimeTextWrapper.setVisibility(View.GONE);
        }

        if (!supportsAwakeSleep(getChartsHost().getDevice())) {
            awakeSleepTimeTextWrapper.setVisibility(View.GONE);
        }

        Date to = new Date((long) this.getTSEnd() * 1000);
        Date from = DateUtils.addDays(to,-(TOTAL_DAYS - 1));
        String toFormattedDate = new SimpleDateFormat("E, MMM dd").format(to);
        String fromFormattedDate = new SimpleDateFormat("E, MMM dd").format(from);
        sleepDatesText.setText(fromFormattedDate + " - " + toFormattedDate);

        mBalanceView.setText(mcd.getWeekBeforeData().getBalanceMessage());
    }

    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        //NB: we could have omitted the day, but this way we can move things to the past easily
        WeekChartsData<BarData> weekBeforeData = refreshWeekBeforeData(db, mWeekChart, day, device);
        mySleepWeeklyData = getMySleepWeeklyData(db, day, device);

        return new MyChartsData(weekBeforeData);
    }

    private void setupSleepScoreChart() {
        final XAxis xAxisBottom = sleepScoreChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(TOTAL_DAYS-1);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);

        final YAxis yAxisLeft = sleepScoreChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = sleepScoreChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);

        sleepScoreChart.setDoubleTapToZoomEnabled(false);
        sleepScoreChart.getDescription().setEnabled(false);
        if (TOTAL_DAYS <= 7) {
            sleepScoreChart.setScaleEnabled(false);
            sleepScoreChart.setTouchEnabled(false);
        }
    }

    @Override
    protected void renderCharts() {
        mWeekChart.invalidate();
        sleepScoreChart.invalidate();
    }

    @Override
    public String getTitle() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return getString(R.string.weeksleepchart_sleep_a_month);
        }
        else{
            return getString(R.string.weeksleepchart_sleep_a_week);
        }
    }

    @Override
    String getPieDescription(int targetValue) {
        return getString(R.string.weeksleepchart_today_sleep_description, DateTimeUtils.formatDurationHoursMinutes(targetValue, TimeUnit.MINUTES));
    }

    @Override
    int getGoal() {
        return GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_SLEEP_DURATION, 8) * 60;
    }

    @Override
    int getOffsetHours() {
        return -12;
    }


    @Override
    protected long calculateBalance(ActivityAmounts activityAmounts) {
        long balance = 0;

        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.DEEP_SLEEP ||
                    amount.getActivityKind() == ActivityKind.LIGHT_SLEEP ||
                    amount.getActivityKind() == ActivityKind.REM_SLEEP) {
                balance += amount.getTotalSeconds();
            }
        }
        return (int) (balance / 60);
    }

    @Override
    protected String getBalanceMessage(long balance, int targetValue) {
        if (balance > 0) {
            final long totalBalance = balance - ((long)targetValue * TOTAL_DAYS_FOR_AVERAGE);
            if (totalBalance > 0)
                return getString(R.string.overslept, getHM(totalBalance));
            else
                return getString(R.string.lack_of_sleep, getHM(Math.abs(totalBalance)));
        } else
            return getString(R.string.no_data);
    }

    @Override
    float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSecondsDeepSleep = 0;
        long totalSecondsLightSleep = 0;
        long totalSecondsRemSleep = 0;
        long totalSecondsAwakeSleep = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.DEEP_SLEEP) {
                totalSecondsDeepSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.LIGHT_SLEEP) {
                totalSecondsLightSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.REM_SLEEP) {
                totalSecondsRemSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.AWAKE_SLEEP) {
                totalSecondsAwakeSleep += amount.getTotalSeconds();
            }
        }
        int totalMinutesDeepSleep = (int) (totalSecondsDeepSleep / 60);
        int totalMinutesLightSleep = (int) (totalSecondsLightSleep / 60);
        int totalMinutesRemSleep = (int) (totalSecondsRemSleep / 60);
        int totalMinutesAwakeSleep = (int) (totalSecondsAwakeSleep / 60);

        float[] activityAmountsTotals =  {totalMinutesDeepSleep, totalMinutesLightSleep};
        if (supportsRemSleep(getChartsHost().getDevice())) {
            activityAmountsTotals = ArrayUtils.add(activityAmountsTotals, totalMinutesRemSleep);
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            activityAmountsTotals = ArrayUtils.add(activityAmountsTotals, totalMinutesAwakeSleep);
        }

        return activityAmountsTotals;
    }

    @Override
    protected String formatPieValue(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    @Override
    String[] getPieLabels() {
        String[] labels = {
                getString(R.string.abstract_chart_fragment_kind_deep_sleep),
                getString(R.string.abstract_chart_fragment_kind_light_sleep)
        };
        if (supportsRemSleep(getChartsHost().getDevice())) {
            labels = ArrayUtils.add(labels,  getString(R.string.abstract_chart_fragment_kind_rem_sleep));
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            labels = ArrayUtils.add(labels,  getString(R.string.abstract_chart_fragment_kind_awake_sleep));
        }
        return labels;
    }

    @Override
    ValueFormatter getPieValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatPieValue((long) value);
            }
        };
    }

    @Override
    ValueFormatter getBarValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    ValueFormatter getYAxisFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    int[] getColors() {
        int[] colors = {akDeepSleep.color, akLightSleep.color};
        if (supportsRemSleep(getChartsHost().getDevice())) {
            colors = ArrayUtils.add(colors, akRemSleep.color);
        }
        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            colors = ArrayUtils.add(colors, akAwakeSleep.color);
        }
        return colors;
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(2);

        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_light);
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_deep);
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsRemSleep(getChartsHost().getDevice())) {
            LegendEntry remSleepEntry = new LegendEntry();
            remSleepEntry.label = getActivity().getString(R.string.sleep_colored_stats_rem);
            remSleepEntry.formColor = akRemSleep.color;
            legendEntries.add(remSleepEntry);
        }

        if (supportsAwakeSleep(getChartsHost().getDevice())) {
            LegendEntry awakeSleepEntry = new LegendEntry();
            awakeSleepEntry.label = getActivity().getString(R.string.abstract_chart_fragment_kind_awake_sleep);
            awakeSleepEntry.formColor = akAwakeSleep.color;
            legendEntries.add(awakeSleepEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    @Override
    String getAverage(float value) {
        return getHM((long)value);
    }

    private static class MySleepWeeklyData {
        private long totalAwake;
        private long totalRem;
        private long totalDeep;
        private long totalLight;
        private int totalDaysForAverage;

        public MySleepWeeklyData(long totalAwake, long totalRem, long totalDeep, long totalLight) {
            this.totalDeep = totalDeep;
            this.totalRem = totalRem;
            this.totalAwake = totalAwake;
            this.totalLight = totalLight;
            this.totalDaysForAverage = 0;
        }

        public long getTotalAwake() {
            return this.totalAwake;
        }

        public long getTotalRem() {
            return this.totalRem;
        }

        public long getTotalDeep() {
            return this.totalDeep;
        }

        public long getTotalLight() {
            return this.totalLight;
        }

        public int getTotalDaysForAverage() {
            return this.totalDaysForAverage;
        }
    }
}
