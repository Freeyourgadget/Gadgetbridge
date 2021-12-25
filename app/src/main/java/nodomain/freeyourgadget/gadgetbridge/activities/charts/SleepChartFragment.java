/*  Copyright (C) 2015-2020 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Dikay900, Pavel Elagin, Q-er, vanous

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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


public class SleepChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private LineChart mActivityChart;
    private PieChart mSleepAmountChart;
    private TextView mSleepchartInfo;
    private TextView heartRateAverageLabel;
    private ImageView heartRateIcon;
    private TextView intensityTotalLabel;
    private ImageView intensityTotalIcon;
    private int heartRateMin = 0;
    private int heartRateMax = 0;
    private float intensityTotal = 0;



    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    Prefs prefs = GBApplication.getPrefs();
    private boolean CHARTS_SLEEP_RANGE_24H = prefs.getBoolean("chart_sleep_range_24h", false);
    private boolean SHOW_CHARTS_AVERAGE = GBApplication.getPrefs().getBoolean("charts_show_average", true);


    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
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
        DefaultChartsData chartsData = refresh(device, samples);
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

        final long totalSeconds = lightSleepDuration + deepSleepDuration;

        final List<PieEntry> entries;
        final List<Integer> colors;

        if (sleepSessions.isEmpty()) {
            entries = Collections.emptyList();
            colors = Collections.emptyList();
        } else {
            entries = Arrays.asList(
                    new PieEntry(lightSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_light_sleep)),
                    new PieEntry(deepSleepDuration, getActivity().getString(R.string.abstract_chart_fragment_kind_deep_sleep))
            );
            colors = Arrays.asList(
                    getColorFor(ActivityKind.TYPE_LIGHT_SLEEP),
                    getColorFor(ActivityKind.TYPE_DEEP_SLEEP)
            );
        }


        String totalSleep = DateTimeUtils.formatDurationHoursMinutes(totalSeconds, TimeUnit.SECONDS);
        PieDataSet set = new PieDataSet(entries, "");
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.SECONDS);
            }
        });
        set.setColors(colors);
        set.setValueTextColor(DESCRIPTION_COLOR);
        set.setValueTextSize(13f);
        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        data.setDataSet(set);

        //setupLegend(pieChart);
        return new MySleepChartsData(totalSleep, data, sleepSessions);
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

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;
        MySleepChartsData pieData = mcd.getPieData();
        mSleepAmountChart.setCenterText(pieData.getTotalSleep());
        mSleepAmountChart.setData(pieData.getPieData());
        mActivityChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mActivityChart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
        mActivityChart.setData(mcd.getChartsData().getData());
        mSleepchartInfo.setText(buildYouSleptText(pieData));
        heartRateMin = mcd.getHeartRateAxisMin();
        heartRateMax = mcd.getHeartRateAxisMax();
        intensityTotal = mcd.getIntensityTotal();

        if (!CHARTS_SLEEP_RANGE_24H
                && supportsHeartrate(getChartsHost().getDevice())
                && SHOW_CHARTS_AVERAGE) {
            if (mcd.getHeartRateAxisMax() != 0 || mcd.getHeartRateAxisMin() != 0) {
                mActivityChart.getAxisRight().setAxisMaximum(mcd.getHeartRateAxisMax());
                mActivityChart.getAxisRight().setAxisMinimum(mcd.getHeartRateAxisMin());
            }
            LimitLine hrAverage_line = new LimitLine(mcd.getHeartRateAverage());
            hrAverage_line.setLineColor(Color.RED);
            hrAverage_line.setLineWidth(0.1f);
            mActivityChart.getAxisRight().removeAllLimitLines();
            mActivityChart.getAxisRight().addLimitLine(hrAverage_line);
            DecimalFormat df = new DecimalFormat("###.#");
            heartRateAverageLabel.setText(df.format(mcd.getHeartRateAverage()));
            intensityTotalLabel.setText(df.format(mcd.getIntensityTotal()));
        }
    }

    private Triple<Float, Integer, Integer> calculateHrData(List<? extends ActivitySample> samples) {
        if (samples.toArray().length < 1) {
            return Triple.of(0f, 0, 0);
        }

        List<Integer> heartRateValues = new ArrayList<>();
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        for (ActivitySample sample : samples) {
            if (sample.getKind() == ActivityKind.TYPE_LIGHT_SLEEP || sample.getKind() == ActivityKind.TYPE_DEEP_SLEEP) {
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
            if (s.getKind() == ActivityKind.TYPE_LIGHT_SLEEP || s.getKind() == ActivityKind.TYPE_DEEP_SLEEP) {
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
        if (pieData.getSleepSessions().isEmpty()) {
            result.append(getContext().getString(R.string.you_did_not_sleep));
        } else {
            for (SleepSession sleepSession : pieData.getSleepSessions()) {
                result.append(getContext().getString(
                        R.string.you_slept,
                        DateTimeUtils.timeToString(sleepSession.getSleepStart()),
                        DateTimeUtils.timeToString(sleepSession.getSleepEnd())));
                result.append('\n');
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

        mActivityChart = rootView.findViewById(R.id.sleepchart);
        mSleepAmountChart = rootView.findViewById(R.id.sleepchart_pie_light_deep);
        mSleepchartInfo = rootView.findViewById(R.id.sleepchart_info);
        heartRateIcon = rootView.findViewById(R.id.heartrate_widget_icon);
        heartRateAverageLabel = rootView.findViewById(R.id.heartrate_widget_label);
        intensityTotalIcon = rootView.findViewById(R.id.intensity_widget_icon);
        intensityTotalLabel = rootView.findViewById(R.id.intensity_widget_label);

        ConstraintLayout intensityTotalWidgetLayout = rootView.findViewById(R.id.intensity_widget_layout);
        ConstraintLayout heartRateWidgetLayout = rootView.findViewById(R.id.heartrate_widget_layout);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DecimalFormat df = new DecimalFormat("###.#");
                String detailedDuration = String.format(getString(R.string.charts_min_max_heartrate_popup), heartRateMin, heartRateMax, df.format(intensityTotal));
                new ChartsActivity.ShowDurationDialog(detailedDuration, getContext()).show();
            }
        };

        heartRateWidgetLayout.setOnClickListener(listener);
        intensityTotalWidgetLayout.setOnClickListener(listener);
        intensityTotalIcon.setOnClickListener(listener);
        intensityTotalLabel.setOnClickListener(listener);


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
    protected void setupLegend(Chart chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(3);
        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);
        heartRateIcon.setVisibility(View.GONE); //hide heart icon
        intensityTotalIcon.setVisibility(View.GONE); //hide intensity icon

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
                heartRateIcon.setVisibility(View.VISIBLE);
                intensityTotalIcon.setVisibility(View.VISIBLE);
            }
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
// temporary fix for totally wrong sleep amounts
//        return super.getSleepSamples(db, device, tsFrom, tsTo);
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void renderCharts() {
        mActivityChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
        mSleepAmountChart.invalidate();
    }

    private static class MySleepChartsData extends ChartsData {
        private String totalSleep;
        private final PieData pieData;
        private final List<SleepSession> sleepSessions;

        public MySleepChartsData(String totalSleep, PieData pieData, List<SleepSession> sleepSessions) {
            this.totalSleep = totalSleep;
            this.pieData = pieData;
            this.sleepSessions = sleepSessions;
        }

        public PieData getPieData() {
            return pieData;
        }

        public CharSequence getTotalSleep() {
            return totalSleep;
        }

        public List<SleepSession> getSleepSessions() {
            return sleepSessions;
        }
    }

    private static class MyChartsData extends ChartsData {
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