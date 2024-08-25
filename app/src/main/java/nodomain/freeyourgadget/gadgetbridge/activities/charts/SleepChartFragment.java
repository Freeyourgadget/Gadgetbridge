/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, José Rebelo, ozkanpakdil, Pavel Elagin, Petr Vaněk, Q-er

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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepAnalysis.SleepSession;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class SleepChartFragment extends AbstractActivityChartFragment<SleepChartFragment.MyChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private LineChart mActivityChart;
    private PieChart mSleepAmountChart;
    private TextView mSleepchartInfo;
    private TextView remSleepTimeText;
    private LinearLayout remSleepTimeTextWrapper;
    private TextView awakeSleepTimeText;
    private LinearLayout awakeSleepTimeTextWrapper;
    private TextView deepSleepTimeText;
    private TextView lightSleepTimeText;
    private TextView lowestHrText;
    private TextView highestHrText;
    private TextView movementIntensityText;
    private TextView sleepDateText;
    private int heartRateMin = 0;
    private int heartRateMax = 0;
    private float intensityTotal = 0;



    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    Prefs prefs = GBApplication.getPrefs();
    private boolean CHARTS_SLEEP_RANGE_24H = prefs.getBoolean("chart_sleep_range_24h", false);
    private boolean SHOW_CHARTS_AVERAGE = prefs.getBoolean("charts_show_average", true);
    private int sleepLinesLimit = prefs.getInt("chart_sleep_lines_limit", 6);


    @Override
    protected MyChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples;
        if (CHARTS_SLEEP_RANGE_24H) {
            samples = getSamples(db, device);
        } else {
            samples = getSamplesofSleep(db, device);
        }

        MySleepChartsData mySleepChartsData = refreshSleepAmounts(device, samples);

        if (!CHARTS_SLEEP_RANGE_24H) {
            if (mySleepChartsData.sleepSessions.size() > 0) {
                long tstart = mySleepChartsData.sleepSessions.get(0).getSleepStart().getTime() / 1000;
                long tend = mySleepChartsData.sleepSessions.get(mySleepChartsData.sleepSessions.size() - 1).getSleepEnd().getTime() / 1000;

                for (Iterator<ActivitySample> iterator = (Iterator<ActivitySample>) samples.iterator(); iterator.hasNext(); ) {
                    ActivitySample sample = iterator.next();
                    if (sample.getTimestamp() < tstart || sample.getTimestamp() > tend) {
                        iterator.remove();
                    }
                }
            }
        }
        DefaultChartsData<LineData> chartsData = refresh(device, samples);
        Triple<Float, Integer, Integer> hrData = calculateHrData(samples);
        Triple<Float, Float, Float> intensityData = calculateIntensityData(samples);
        return new MyChartsData(mySleepChartsData, chartsData, hrData.getLeft(), hrData.getMiddle(), hrData.getRight(), intensityData.getLeft(), intensityData.getMiddle(), intensityData.getRight());
    }



    private MySleepChartsData refreshSleepAmounts(GBDevice mGBDevice, List<? extends ActivitySample> samples) {
        SleepAnalysis sleepAnalysis = new SleepAnalysis();
        List<SleepSession> sleepSessions = sleepAnalysis.calculateSleepSessions(samples);

        PieData data = new PieData();


        final long lightSleepDuration = calculateLightSleepDuration(sleepSessions);
        final long deepSleepDuration = calculateDeepSleepDuration(sleepSessions);
        final long remSleepDuration = calculateRemSleepDuration(sleepSessions);
        final long awakeSleepDuration = calculateAwakeSleepDuration(sleepSessions);
        final long totalSeconds = lightSleepDuration + deepSleepDuration + remSleepDuration;

        final List<PieEntry> entries = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();

        if (!sleepSessions.isEmpty()) {
            entries.add(new PieEntry(lightSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_light_sleep)));
            entries.add(new PieEntry(deepSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_deep_sleep)));
            colors.add(getColorFor(ActivityKind.LIGHT_SLEEP));
            colors.add(getColorFor(ActivityKind.DEEP_SLEEP));

            if (supportsRemSleep(mGBDevice)) {
                entries.add(new PieEntry(remSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_rem_sleep)));
                colors.add(getColorFor(ActivityKind.REM_SLEEP));
            }

            if (supportsAwakeSleep(mGBDevice)) {
                entries.add(new PieEntry(awakeSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_awake_sleep)));
                colors.add(getColorFor(ActivityKind.AWAKE_SLEEP));
            }
        } else {
            entries.add(new PieEntry(1));
            colors.add(getResources().getColor(R.color.gauge_line_color));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setSliceSpace(2f);
        set.setColors(colors);
        set.setValueTextColor(DESCRIPTION_COLOR);
        set.setValueTextSize(13f);
        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        data.setDataSet(set);

        String totalSleep = DateTimeUtils.formatDurationHoursMinutes(totalSeconds, TimeUnit.SECONDS);
        String totalAwake = DateTimeUtils.formatDurationHoursMinutes(awakeSleepDuration, TimeUnit.SECONDS);
        String totalRem = DateTimeUtils.formatDurationHoursMinutes(remSleepDuration, TimeUnit.SECONDS);
        String totalDeep = DateTimeUtils.formatDurationHoursMinutes(deepSleepDuration, TimeUnit.SECONDS);
        String totalLight = DateTimeUtils.formatDurationHoursMinutes(lightSleepDuration, TimeUnit.SECONDS);
        //setupLegend(pieChart);
        return new MySleepChartsData(data, sleepSessions, totalSleep, totalAwake, totalRem, totalDeep, totalLight);
    }

    private long calculateLightSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getLightSleepDuration();
        }
        return result;
    }

    private long calculateDeepSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getDeepSleepDuration();
        }
        return result;
    }

    private long calculateRemSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getRemSleepDuration();
        }
        return result;
    }

    private long calculateAwakeSleepDuration(List<SleepSession> sleepSessions) {
        long result = 0;
        for (SleepSession sleepSession : sleepSessions) {
            result += sleepSession.getAwakeSleepDuration();
        }
        return result;
    }

    @Override
    protected void updateChartsnUIThread(MyChartsData mcd) {
        MySleepChartsData pieData = mcd.getPieData();

        Date date = new Date((long) this.getTSEnd() * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        sleepDateText.setText(formattedDate);

        pieData.pieData.setDrawValues(false);
        mSleepAmountChart.setTouchEnabled(false);
        mSleepAmountChart.setCenterTextColor(GBApplication.getTextColor(getContext()));
        mSleepAmountChart.setCenterText(pieData.getTotalSleep());
        if (!pieData.sleepSessions.isEmpty()) {
            awakeSleepTimeText.setText(pieData.getTotalAwake());
            remSleepTimeText.setText(pieData.getTotalRem());
            deepSleepTimeText.setText(pieData.getTotalDeep());
            lightSleepTimeText.setText(pieData.getTotalLight());
        } else {
            awakeSleepTimeText.setText("-");
            remSleepTimeText.setText("-");
            deepSleepTimeText.setText("-");
            lightSleepTimeText.setText("-");
        }
        if (!supportsRemSleep(getChartsHost().getDevice())) {
            remSleepTimeTextWrapper.setVisibility(View.GONE);
        }
        if (!supportsAwakeSleep(getChartsHost().getDevice())) {
            awakeSleepTimeTextWrapper.setVisibility(View.GONE);
        }
        mSleepAmountChart.setCenterTextSize(18f);
        mSleepAmountChart.setHoleColor(getContext().getResources().getColor(R.color.transparent));
        mSleepAmountChart.setData(pieData.getPieData());
        mSleepchartInfo.setText(buildYouSleptText(pieData));
        mSleepchartInfo.setMovementMethod(new ScrollingMovementMethod());
        mActivityChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mActivityChart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
        mActivityChart.getAxisLeft().setDrawLabels(false);

        mActivityChart.setData(mcd.getChartsData().getData());
        heartRateMin = mcd.getHeartRateAxisMin();
        heartRateMax = mcd.getHeartRateAxisMax();
        intensityTotal = mcd.getIntensityTotal();
        lowestHrText.setText(String.valueOf(heartRateMin != 0 ? heartRateMin : "-"));
        highestHrText.setText(String.valueOf(heartRateMax != 0 ? heartRateMax : "-"));
        movementIntensityText.setText(intensityTotal != 0 ? new DecimalFormat("###.#").format(intensityTotal) : "-");

        mSleepAmountChart.setHoleRadius(85);
        mSleepAmountChart.setDrawEntryLabels(false);
        mSleepAmountChart.getLegend().setEnabled(false);

        if (!CHARTS_SLEEP_RANGE_24H
                && supportsHeartrate(getChartsHost().getDevice())
                && SHOW_CHARTS_AVERAGE) {
            if (mcd.getHeartRateAxisMax() != 0 || mcd.getHeartRateAxisMin() != 0) {
                mActivityChart.getAxisRight().setAxisMaximum(mcd.getHeartRateAxisMax() + (mcd.getHeartRateAxisMin() / 2f));
                mActivityChart.getAxisRight().setAxisMinimum(mcd.getHeartRateAxisMin() / 2f);
            }
            LimitLine hrAverage_line = new LimitLine(mcd.getHeartRateAverage());
            hrAverage_line.setLineColor(Color.RED);
            hrAverage_line.setLineWidth(0.1f);
            mActivityChart.getAxisRight().removeAllLimitLines();
            mActivityChart.getAxisRight().addLimitLine(hrAverage_line);
        }
    }

    private Triple<Float, Integer, Integer> calculateHrData(List<? extends ActivitySample> samples) {
        if (samples.toArray().length < 1) {
            return Triple.of(0f, 0, 0);
        }

        List<Integer> heartRateValues = new ArrayList<>();
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        for (ActivitySample sample : samples) {
            if (sample.getKind() == ActivityKind.LIGHT_SLEEP || sample.getKind() == ActivityKind.DEEP_SLEEP) {
                int heartRate = sample.getHeartRate();
                if (heartRateUtilsInstance.isValidHeartRateValue(heartRate)) {
                    heartRateValues.add(heartRate);
                }
            }
        }
        if (heartRateValues.toArray().length < 1) {
            return Triple.of(0f, 0, 0);
        }

        int min = Collections.min(heartRateValues);
        int max = Collections.max(heartRateValues);
        int count = heartRateValues.toArray().length;
        float sum = calculateSumOfInts(heartRateValues);
        float average = sum / count;
        return Triple.of(average, min, max);
    }

    private float calculateIntensitySum(List<Float> samples) {
        float result = 0;
        for (Float sample : samples) {
            result += sample;
        }
        return result;
    }

    private float calculateSumOfInts(List<Integer> samples) {
        float result = 0;
        for (Integer sample : samples) {
            result += sample;
        }
        return result;
    }

    private Triple<Float, Float, Float> calculateIntensityData(List<? extends ActivitySample> samples) {
        if (samples.toArray().length < 1) {
            return Triple.of(0f, 0f, 0f);
        }

        List<Float> allIntensities = new ArrayList<>();

        for (ActivitySample s : samples) {
            if (s.getKind() == ActivityKind.LIGHT_SLEEP || s.getKind() == ActivityKind.DEEP_SLEEP) {
                float intensity = s.getIntensity();
                allIntensities.add(intensity);
            }
        }
        if (allIntensities.toArray().length < 1) {
            return Triple.of(0f, 0f, 0f);
        }

        Float min = Collections.min(allIntensities);
        Float max = Collections.max(allIntensities);
        Float sum = calculateIntensitySum(allIntensities);

        return Triple.of(sum, min, max);
    }

    private String buildYouSleptText(MySleepChartsData pieData) {
        final StringBuilder result = new StringBuilder();
        if (!pieData.getSleepSessions().isEmpty()) {
            for (SleepSession sleepSession : pieData.getSleepSessions()) {
                if (result.length() > 0) {
                    result.append("  |  ");
                }
                String from = DateTimeUtils.timeToString(sleepSession.getSleepStart());
                String to = DateTimeUtils.timeToString(sleepSession.getSleepEnd());
                result.append(String.format("%s - %s", from, to));
            }
        }
        return result.toString();
    }

    @Override
    public String getTitle() {
        return getString(R.string.sleepchart_your_sleep);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sleepchart, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mActivityChart = rootView.findViewById(R.id.sleepchart);
        mSleepAmountChart = rootView.findViewById(R.id.sleepchart_pie_light_deep);
        mSleepchartInfo = rootView.findViewById(R.id.sleepchart_info);
        remSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_rem_time);
        remSleepTimeTextWrapper = rootView.findViewById(R.id.sleep_chart_legend_rem_time_wrapper);
        awakeSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_awake_time);
        awakeSleepTimeTextWrapper = rootView.findViewById(R.id.sleep_chart_legend_awake_time_wrapper);
        deepSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_deep_time);
        lightSleepTimeText = rootView.findViewById(R.id.sleep_chart_legend_light_time);
        lowestHrText = rootView.findViewById(R.id.sleep_hr_lowest);
        highestHrText = rootView.findViewById(R.id.sleep_hr_highest);
        movementIntensityText = rootView.findViewById(R.id.sleep_movement_intensity);
        sleepDateText = rootView.findViewById(R.id.sleep_date);

        mSleepchartInfo.setMaxLines(sleepLinesLimit);

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
        mSleepAmountChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mSleepAmountChart.setEntryLabelColor(DESCRIPTION_COLOR);
        mSleepAmountChart.getDescription().setText("");
//        mSleepAmountChart.getDescription().setNoDataTextDescription("");
        mSleepAmountChart.setNoDataText("");
        mSleepAmountChart.getLegend().setEnabled(false);
    }

    private void setupActivityChart() {
        mActivityChart.setBackgroundColor(BACKGROUND_COLOR);
        mActivityChart.getDescription().setTextColor(DESCRIPTION_COLOR);
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
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
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
        yAxisRight.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        yAxisRight.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(4);
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

        if (supportsHeartrate(getChartsHost().getDevice())) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
            if (!CHARTS_SLEEP_RANGE_24H && SHOW_CHARTS_AVERAGE) {
                LegendEntry hrAverageEntry = new LegendEntry();
                hrAverageEntry.label = HEARTRATE_AVERAGE_LABEL;
                hrAverageEntry.formColor = Color.RED;
                legendEntries.add(hrAverageEntry);
            }
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
// temporary fix for totally wrong sleep amounts
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void renderCharts() {
        mActivityChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
        mSleepAmountChart.invalidate();
    }

    private static class MySleepChartsData extends ChartsData {
        private String totalSleep;
        private String totalAwake;
        private String totalRem;
        private String totalDeep;
        private String totalLight;
        private final PieData pieData;
        private final List<SleepSession> sleepSessions;

        public MySleepChartsData(PieData pieData, List<SleepSession> sleepSessions, String totalSleep, String totalAwake, String totalRem, String totalDeep, String totalLight) {
            this.pieData = pieData;
            this.sleepSessions = sleepSessions;
            this.totalAwake = totalAwake;
            this.totalSleep = totalSleep;
            this.totalRem = totalRem;
            this.totalDeep = totalDeep;
            this.totalLight = totalLight;
        }

        public PieData getPieData() {
            return pieData;
        }

        public CharSequence getTotalSleep() {
            return totalSleep;
        }

        public CharSequence getTotalAwake() {
            return totalAwake;
        }

        public CharSequence getTotalRem() {
            return totalRem;
        }

        public CharSequence getTotalDeep() {
            return totalDeep;
        }

        public CharSequence getTotalLight() {
            return totalLight;
        }

        public List<SleepSession> getSleepSessions() {
            return sleepSessions;
        }
    }

    protected static class MyChartsData extends ChartsData {
        private final DefaultChartsData<LineData> chartsData;
        private final MySleepChartsData pieData;
        private final float heartRateAverage;
        private int heartRateAxisMax;
        private int heartRateAxisMin;
        private float intensityAxisMax;
        private float intensityAxisMin;
        private float intensityTotal;

        public MyChartsData(MySleepChartsData pieData, DefaultChartsData<LineData> chartsData, float heartRateAverage, int heartRateAxisMin, int heartRateAxisMax, float intensityTotal, float intensityAxisMin, float intensityAxisMax) {
            this.pieData = pieData;
            this.chartsData = chartsData;
            this.heartRateAverage = heartRateAverage;
            this.heartRateAxisMax = heartRateAxisMax;
            this.heartRateAxisMin = heartRateAxisMin;
            this.intensityTotal = intensityTotal;
            this.intensityAxisMin = intensityAxisMin;
            this.intensityAxisMax = intensityAxisMax;
        }

        public MySleepChartsData getPieData() {
            return pieData;
        }

        public DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }

        public float getHeartRateAverage() {
            return heartRateAverage;
        }

        public int getHeartRateAxisMax() {
            return heartRateAxisMax;
        }

        public int getHeartRateAxisMin() { return heartRateAxisMin; }

        public float getIntensityAxisMax() {
            return intensityAxisMax;
        }

        public float getIntensityAxisMin() {
            return intensityAxisMin;
        }

        public float getIntensityTotal() { return intensityTotal;}

    }
}
