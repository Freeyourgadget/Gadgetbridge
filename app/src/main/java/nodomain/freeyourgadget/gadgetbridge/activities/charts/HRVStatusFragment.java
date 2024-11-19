/*  Copyright (C) 2017-2024 Andreas Shimokawa, Daniele Gobbetti, José Rebelo

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

import static java.util.stream.Collectors.toCollection;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardHrvWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class HRVStatusFragment extends AbstractChartFragment<HRVStatusFragment.HRVStatusWeeklyData> {
    protected static final Logger LOG = LoggerFactory.getLogger(HRVStatusFragment.class);
    protected final int TOTAL_DAYS = 7;

    protected GaugeDrawer gaugeDrawer;
    private ImageView mHRVStatusGauge;
    private LineChart mWeeklyHRVStatusChart;
    private TextView mHRVStatusSevenDaysAvg;
    private TextView mHRVStatusSevenDaysAvgStatus; // Balanced, Unbalanced, Low
    private TextView mHRVStatusLastNight;
    private TextView mHRVStatusLastNight5MinHighest;
    private TextView mHRVStatusDayAvg;
    private TextView mHRVStatusBaseline;
    private TextView mDateView;
    private TextView mHRVGaugeValue;
    private TextView mHRVGaugeStatus;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_hrv_status, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mWeeklyHRVStatusChart = rootView.findViewById(R.id.hrv_weekly_line_chart);
        mHRVStatusLastNight = rootView.findViewById(R.id.hrv_status_last_night);
        mHRVStatusSevenDaysAvg = rootView.findViewById(R.id.hrv_status_seven_days_avg);
        mHRVStatusSevenDaysAvgStatus = rootView.findViewById(R.id.hrv_status_seven_days_avg_rate);
        mHRVStatusLastNight5MinHighest = rootView.findViewById(R.id.hrv_status_last_night_highest_5);
        mHRVStatusDayAvg = rootView.findViewById(R.id.hrv_status_day_avg);
        mHRVStatusBaseline = rootView.findViewById(R.id.hrv_status_baseline);
        mDateView = rootView.findViewById(R.id.hrv_status_date_view);
        mHRVStatusGauge = rootView.findViewById(R.id.hrv_status_gauge_bar);
        mHRVGaugeValue = rootView.findViewById(R.id.hrv_gauge_value);
        mHRVGaugeStatus = rootView.findViewById(R.id.hrv_gauge_status);

        gaugeDrawer = new GaugeDrawer();
        setupLineChart();
        refresh();

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.pref_header_hrv_status);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    protected HRVStatusWeeklyData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());

        List<HRVStatusDayData> weeklyData = getWeeklyData(db, day, device);
        return new HRVStatusWeeklyData(weeklyData);
    }

    @Override
    protected void renderCharts() {
        mWeeklyHRVStatusChart.invalidate();
    }

    protected LineDataSet createDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.hrv_status_day_avg));
        lineDataSet.setColor(getResources().getColor(R.color.hrv_status_char_line_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setCircleColor(getResources().getColor(R.color.hrv_status_char_line_color));
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
    }

    @Override
    protected void updateChartsnUIThread(HRVStatusWeeklyData weeklyData) {
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        mDateView.setText(formattedDate);

        mWeeklyHRVStatusChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        List<Entry> lineEntries = new ArrayList<>();
        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        weeklyData.getDaysData().forEach((HRVStatusDayData day) -> {
            if (day.status.getNum() > 0) {
                lineEntries.add(new Entry(day.i, day.dayAvg));
            } else {
                if (!lineEntries.isEmpty()) {
                    List<Entry> clone = new ArrayList<>(lineEntries.size());
                    clone.addAll(lineEntries);
                    lineDataSets.add(createDataSet(clone));
                    lineEntries.clear();
                }
            }
        });
        if (!lineEntries.isEmpty()) {
            lineDataSets.add(createDataSet(lineEntries));
        }

        List<LegendEntry> legendEntries = new ArrayList<>(1);
        LegendEntry activityEntry = new LegendEntry();
        activityEntry.label = getString(R.string.hrv_status_day_avg_legend);
        activityEntry.formColor = getResources().getColor(R.color.hrv_status_char_line_color);
        legendEntries.add(activityEntry);
        mWeeklyHRVStatusChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mWeeklyHRVStatusChart.getLegend().setCustom(legendEntries);

        final LineData lineData = new LineData(lineDataSets);
        mWeeklyHRVStatusChart.setData(lineData);

        final XAxis x = mWeeklyHRVStatusChart.getXAxis();
        x.setValueFormatter(getHRVStatusChartDayValueFormatter(weeklyData));

        HRVStatusDayData today = weeklyData.getCurrentDay();
        mHRVStatusSevenDaysAvg.setText(today.weeklyAvg > 0 ? getString(R.string.hrv_status_unit, today.weeklyAvg) : "-");
        mHRVStatusLastNight.setText(today.lastNight > 0 ? getString(R.string.hrv_status_unit, today.lastNight) : "-");
        mHRVStatusLastNight5MinHighest.setText(today.lastNight5MinHigh > 0 ? getString(R.string.hrv_status_unit, today.lastNight5MinHigh) : "-");
        mHRVStatusDayAvg.setText(today.dayAvg > 0 ? getString(R.string.hrv_status_unit, today.dayAvg) : "-");
        mHRVStatusBaseline.setText(today.baseLineBalancedLower > 0 && today.baseLineBalancedUpper > 0 ? getString(R.string.hrv_status_baseline, today.baseLineBalancedLower, today.baseLineBalancedUpper) : "-");
        switch (today.status) {
            case NONE:
                mHRVStatusSevenDaysAvgStatus.setText("-");
                mHRVStatusSevenDaysAvgStatus.setTextColor(TEXT_COLOR);
                mHRVGaugeStatus.setText("");
                mHRVGaugeStatus.setTextColor(TEXT_COLOR);
                break;
            case POOR:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_poor));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_poor));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_poor));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_poor));
                break;
            case LOW:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_low));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_low));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_low));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_low));
                break;
            case UNBALANCED:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_unbalanced));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_unbalanced));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_unbalanced));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_unbalanced));
                break;
            case BALANCED:
                mHRVStatusSevenDaysAvgStatus.setText(getString(R.string.hrv_status_balanced));
                mHRVStatusSevenDaysAvgStatus.setTextColor(getResources().getColor(R.color.hrv_status_balanced));
                mHRVGaugeStatus.setText(getString(R.string.hrv_status_balanced));
                mHRVGaugeStatus.setTextColor(getResources().getColor(R.color.hrv_status_balanced));
                break;
        }
        float value = DashboardHrvWidget.calculateGaugeValue(today.weeklyAvg, today.baseLineLowUpper, today.baseLineBalancedLower, today.baseLineBalancedUpper);
        final String valueText = value > 0 ? getString(R.string.hrv_status_unit, today.weeklyAvg) : getString(R.string.stats_empty_value);
        mHRVGaugeValue.setText(valueText);
        gaugeDrawer.drawSegmentedGauge(mHRVStatusGauge, DashboardHrvWidget.getColors(), DashboardHrvWidget.getSegments(), value, false, true);
    }

    private List<HRVStatusDayData> getWeeklyData(DBHandler db, Calendar day, GBDevice device) {
        day = DateTimeUtils.dayStart(day);
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);

        List<HRVStatusDayData> weeklyData = new ArrayList<>();
        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            int startTs = (int) (day.getTimeInMillis() / 1000);
            int endTs = startTs + 24 * 60 * 60 - 1;
            Optional<? extends HrvSummarySample> latestSummarySample = getSamples(db, device, startTs, endTs)
                    .stream()
                    .max(Comparator.comparingLong(HrvSummarySample::getTimestamp));
            List<? extends HrvValueSample> valueSamples = getHrvValueSamples(db, device, startTs, endTs);

            int avgHRV = (int) valueSamples.stream().mapToInt(HrvValueSample::getValue).average().orElse(0);
            if (latestSummarySample.isPresent()) {
                final HrvSummarySample sample = latestSummarySample.get();
                Calendar finalDay = (Calendar) day.clone();
                weeklyData.add(new HRVStatusDayData(
                        finalDay,
                        counter,
                        sample.getTimestamp(),
                        avgHRV,
                        sample.getWeeklyAverage() != null ? sample.getWeeklyAverage() : 0,
                        sample.getLastNightAverage() != null ? sample.getLastNightAverage() : 0,
                        sample.getLastNight5MinHigh() != null ? sample.getLastNight5MinHigh() : 0,
                        sample.getBaselineLowUpper() != null ? sample.getBaselineLowUpper() : 0,
                        sample.getBaselineBalancedLower() != null ? sample.getBaselineBalancedLower() : 0,
                        sample.getBaselineBalancedUpper() != null ? sample.getBaselineBalancedUpper() : 0,
                        sample.getStatus() != null ? sample.getStatus() : HrvSummarySample.Status.NONE
                ));
            } else {
                HRVStatusDayData d = new HRVStatusDayData(
                        (Calendar) day.clone(),
                        counter,
                        0,
                        avgHRV,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        HrvSummarySample.Status.NONE
                );
                weeklyData.add(d);
            }

            day.add(Calendar.DATE, 1);
        }
        return weeklyData;
    }

    private List<? extends HrvSummarySample> getSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends HrvSummarySample> sampleProvider = coordinator.getHrvSummarySampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    public List<? extends HrvValueSample> getHrvValueSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends HrvValueSample> sampleProvider = coordinator.getHrvValueSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    private void setupLineChart() {
        mWeeklyHRVStatusChart.getDescription().setEnabled(false);
        mWeeklyHRVStatusChart.setTouchEnabled(false);
        mWeeklyHRVStatusChart.setPinchZoom(false);
        mWeeklyHRVStatusChart.setDoubleTapToZoomEnabled(false);


        final XAxis xAxisBottom = mWeeklyHRVStatusChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setAxisMaximum(6 + 0.5f);
        xAxisBottom.setAxisMinimum(0 - 0.5f);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = mWeeklyHRVStatusChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(120);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = mWeeklyHRVStatusChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    ValueFormatter getHRVStatusChartDayValueFormatter(HRVStatusWeeklyData weeklyData) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatHRVStatusChartValue((long) value, weeklyData);
            }
        };
    }

    protected String formatHRVStatusChartValue(long value, HRVStatusWeeklyData weeklyData) {
        HRVStatusDayData day = weeklyData.getDay((int) value);

        SimpleDateFormat formatLetterDay = new SimpleDateFormat("EEE", Locale.getDefault());
        return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
    }

    protected void setupLegend(Chart<?> chart) {}

    protected static class HRVStatusWeeklyData extends ChartsData {
        private final List<HRVStatusDayData> data;

        public HRVStatusWeeklyData(final List<HRVStatusDayData> chartsData) {
            this.data = chartsData;
        }

        public HRVStatusDayData getDay(int i) {
            return this.data.get(i);
        }

        public HRVStatusDayData getCurrentDay() {
            return this.data.get(this.data.size() - 1);
        }

        public List<HRVStatusDayData> getDaysData() {
            return data;
        }
    }

    protected static class HRVStatusDayData {
        public Integer i;
        public long timestamp;
        public Integer weeklyAvg;
        public Integer lastNight;
        public Integer lastNight5MinHigh;
        public Integer dayAvg;
        public Integer baseLineBalancedLower;
        public Integer baseLineBalancedUpper;
        public Integer baseLineLowUpper;
        public HrvSummarySample.Status status;
        public Calendar day;

        public HRVStatusDayData(Calendar day,
                                int i, long timestamp,
                                Integer dayAvg,
                                Integer weeklyAvg,
                                Integer lastNight,
                                Integer lastNight5MinHigh,
                                Integer baseLineLowUpper,
                                Integer baseLineBalancedLower,
                                Integer baseLineBalancedUpper,
                                HrvSummarySample.Status status) {
            this.lastNight = lastNight;
            this.weeklyAvg = weeklyAvg;
            this.lastNight5MinHigh = lastNight5MinHigh;
            this.i = i;
            this.timestamp = timestamp;
            this.status = status;
            this.day = day;
            this.dayAvg = dayAvg;
            this.baseLineLowUpper = baseLineLowUpper;
            this.baseLineBalancedLower = baseLineBalancedLower;
            this.baseLineBalancedUpper = baseLineBalancedUpper;
        }
    }
}
