/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo

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

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class PaiChartFragment extends AbstractChartFragment<PaiChartFragment.PaiChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(PaiChartFragment.class);

    protected final int TOTAL_DAYS = getRangeDays();

    protected Locale mLocale;

    protected ImageView mGoalMinutesGauge;
    protected BarChart mWeekChart;
    protected TextView mDateView;
    protected TextView mLineToday;
    protected TextView mLineTotal;
    protected TextView mLineLowInc;
    protected TextView mLineLowTime;
    protected TextView mLineModerateInc;
    protected TextView mLineModerateTime;
    protected TextView mLineHighInc;
    protected TextView mLineHighTime;
    protected LinearLayout mTileLow;
    protected LinearLayout mTileModerate;
    protected LinearLayout mTileHigh;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;

    protected int PAI_TOTAL_COLOR;
    protected int PAI_DAY_COLOR;

    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = ContextCompat.getColor(requireContext(), R.color.secondarytext);

        PAI_TOTAL_COLOR = ContextCompat.getColor(requireContext(), R.color.chart_pai_weekly);
        PAI_DAY_COLOR = ContextCompat.getColor(requireContext(), R.color.chart_pai_today);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        mLocale = getResources().getConfiguration().locale;

        final View rootView = inflater.inflate(R.layout.fragment_pai_chart, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mGoalMinutesGauge = rootView.findViewById(R.id.goal_minutes_gauge);
        mWeekChart = rootView.findViewById(R.id.pai_chart_week);
        mDateView = rootView.findViewById(R.id.pai_date_view);
        mLineToday = rootView.findViewById(R.id.pai_line_today);
        mLineTotal = rootView.findViewById(R.id.pai_line_total);
        mLineLowInc = rootView.findViewById(R.id.pai_line_low_inc);
        mLineLowTime = rootView.findViewById(R.id.pai_line_low_time);
        mLineModerateInc = rootView.findViewById(R.id.pai_line_moderate_inc);
        mLineModerateTime = rootView.findViewById(R.id.pai_line_moderate_time);
        mLineHighInc = rootView.findViewById(R.id.pai_line_high_inc);
        mLineHighTime = rootView.findViewById(R.id.pai_line_high_time);
        mTileLow = rootView.findViewById(R.id.pai_tile_low);
        mTileModerate = rootView.findViewById(R.id.pai_tile_moderate);
        mTileHigh = rootView.findViewById(R.id.pai_tile_high);

        if (!getChartsHost().getDevice().getDeviceCoordinator().supportsPaiTime()) {
            mLineLowTime.setVisibility(View.GONE);
            mLineModerateTime.setVisibility(View.GONE);
            mLineHighTime.setVisibility(View.GONE);
        }

        if (!getChartsHost().getDevice().getDeviceCoordinator().supportsPaiLow()) {
            mTileLow.setVisibility(View.GONE);
        }

        setupWeekChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    private void setupWeekChart() {
        mWeekChart.setBackgroundColor(BACKGROUND_COLOR);
        mWeekChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mWeekChart.getDescription().setText("");
        mWeekChart.setFitBars(true);

        configureBarLineChartDefaults(mWeekChart);

        final XAxis x = mWeekChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        final YAxis y = mWeekChart.getAxisLeft();
        y.setDrawGridLines(true);
        y.setDrawTopYLabelEntry(true);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setDrawZeroLine(true);
        y.setSpaceBottom(0);
        y.setAxisMinimum(0);
        y.setAxisMaximum(getPaiTarget());
        y.setValueFormatter(getRoundFormatter());
        y.setEnabled(true);

        final YAxis yAxisRight = mWeekChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
    }

    private int getRangeDays() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return 30;
        } else {
            return 7;
        }
    }

    private int getPaiTarget() {
        return getChartsHost().getDevice().getDeviceCoordinator().getPaiTarget();
    }

    @Override
    public String getTitle() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return getString(R.string.pai_chart_per_month);
        } else {
            return getString(R.string.pai_chart_per_week);
        }
    }

    @Override
    protected PaiChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        final Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        //NB: we could have omitted the day, but this way we can move things to the past easily
        final DayData dayData = refreshDayData(db, day, device);
        final WeekChartsData<BarData> weekBeforeData = refreshWeekBeforeData(db, mWeekChart, day, device);

        return new PaiChartsData(dayData, weekBeforeData);
    }

    @Override
    protected void updateChartsnUIThread(final PaiChartsData pcd) {
        setupLegend(mWeekChart);
        int[] colors = new int[] {
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_pai_weekly),
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_pai_today)
        };
        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );
        mGoalMinutesGauge.setImageBitmap(GaugeDrawer.drawCircleGaugeSegmented(
                width,
                width / 15,
                colors,
                pcd.getDayData().getGaugeSegments(),
                false,
                String.valueOf(pcd.getDayData().getTotal()),
                String.valueOf(getPaiTarget()),
                getContext()
        ));

        // set custom renderer for 30days bar charts
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            mWeekChart.setRenderer(new AngledLabelsChartRenderer(mWeekChart, mWeekChart.getAnimator(), mWeekChart.getViewPortHandler()));
        }

        mWeekChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mWeekChart.setData(pcd.getWeekBeforeData().getData());
        mWeekChart.getXAxis().setValueFormatter(pcd.getWeekBeforeData().getXValueFormatter());

        mDateView.setText(DateTimeUtils.formatDate(pcd.getDayData().day.getTime()));
        mLineToday.setText(requireContext().getString(R.string.pai_plus_num, pcd.getDayData().today));
        mLineTotal.setText(String.valueOf(pcd.getDayData().total));
        mLineLowInc.setText(String.valueOf(pcd.getDayData().paiLow));
        mLineLowTime.setText(requireContext().getString(R.string.num_min, pcd.getDayData().minutesLow));
        mLineModerateInc.setText(String.valueOf(pcd.getDayData().paiModerate));
        mLineModerateTime.setText(requireContext().getString(R.string.num_min, pcd.getDayData().minutesModerate));
        mLineHighInc.setText(String.valueOf(pcd.getDayData().paiHigh));
        mLineHighTime.setText(requireContext().getString(R.string.num_min, pcd.getDayData().minutesHigh));
    }

    @Override
    protected void renderCharts() {
        mWeekChart.invalidate();
    }

    protected String getWeeksChartsLabel(final Calendar day) {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            // month, show day date
            return String.valueOf(day.get(Calendar.DAY_OF_MONTH));
        } else {
            // week, show short day name
            return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, mLocale);
        }
    }

    protected WeekChartsData<BarData> refreshWeekBeforeData(final DBHandler db,
                                                            final BarChart barChart,
                                                            Calendar day,
                                                            final GBDevice device) {
        day = (Calendar) day.clone(); // do not modify the caller's argument
        day.add(Calendar.DATE, -TOTAL_DAYS + 1);

        List<BarEntry> entries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();

        int maxPai = -1;

        for (int counter = 0; counter < TOTAL_DAYS; counter++) {
            final Optional<? extends PaiSample> sampleOpt = getSamplePaiForDay(db, device, day);

            if (sampleOpt.isPresent()) {
                final PaiSample sample = sampleOpt.get();
                final int paiToday = Math.round(sample.getPaiToday());
                final int paiTotal = Math.round(sample.getPaiTotal());

                if (paiTotal > maxPai) {
                    maxPai = paiTotal;
                }

                final float[] paiBar = new float[]{
                        paiTotal - paiToday,
                        paiToday
                };

                entries.add(new BarEntry(counter, paiBar));
            } else {
                entries.add(new BarEntry(counter, new float[]{0.0f, 0.0f}));
            }
            labels.add(getWeeksChartsLabel(day));
            day.add(Calendar.DATE, 1);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(PAI_TOTAL_COLOR, PAI_DAY_COLOR);
        set.setValueFormatter(getRoundFormatter());

        BarData barData = new BarData(set);
        barData.setValueTextColor(Color.GRAY); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);
        barData.setValueTextSize(10f);

        barChart.getAxisLeft().setAxisMaximum(Math.max(maxPai, getPaiTarget()));

        return new WeekChartsData(barData, new PreformattedXIndexLabelFormatter(labels));
    }

    protected DayData refreshDayData(final DBHandler db,
                                     final Calendar day,
                                     final GBDevice device) {
        final Optional<? extends PaiSample> sampleOpt = getSamplePaiForDay(db, device, day);

        final int today;
        final int total;
        final int paiLow;
        final int paiModerate;
        final int paiHigh;
        final int minutesLow;
        final int minutesModerate;
        final int minutesHigh;

        if (sampleOpt.isPresent()) {
            final PaiSample sample = sampleOpt.get();
            today = Math.round(sample.getPaiToday());
            total = Math.round(sample.getPaiTotal());
            paiLow = Math.round(sample.getPaiLow());
            paiModerate = Math.round(sample.getPaiModerate());
            paiHigh = Math.round(sample.getPaiHigh());
            minutesLow = sample.getTimeLow();
            minutesModerate = sample.getTimeModerate();
            minutesHigh = sample.getTimeHigh();
        } else {
            today = 0;
            total = 0;
            paiLow = 0;
            paiModerate = 0;
            paiHigh = 0;
            minutesLow = 0;
            minutesModerate = 0;
            minutesHigh = 0;
        }

        final int maxPai = Math.max(getPaiTarget(), total);

        float[] segments = new float[] {
                (float) (total - today) / maxPai,
                (float) today / maxPai
        };

        return new DayData(day, segments, today, total, paiLow, paiModerate, paiHigh, minutesLow, minutesModerate, minutesHigh);
    }

    protected ValueFormatter getRoundFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(final float value) {
                return String.valueOf(Math.round(value));
            }
        };
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(2);

        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = requireContext().getString(R.string.pai_total);
        lightSleepEntry.formColor = PAI_TOTAL_COLOR;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = requireContext().getString(R.string.pai_day);
        deepSleepEntry.formColor = PAI_DAY_COLOR;
        legendEntries.add(deepSleepEntry);

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    private Optional<? extends PaiSample> getSamplePaiForDay(final DBHandler db, final GBDevice device, final Calendar day) {
        final Date dayStart = DateTimeUtils.dayStart(day.getTime());
        final Date dayEnd = DateTimeUtils.dayEnd(day.getTime());
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends PaiSample> sampleProvider = coordinator.getPaiSampleProvider(device, db.getDaoSession());
        final List<? extends PaiSample> daySamples = sampleProvider.getAllSamples(dayStart.getTime(), dayEnd.getTime());
        return Optional.ofNullable(daySamples.isEmpty() ? null : daySamples.get(daySamples.size() - 1));
    }

    protected static class DayData {
        private final Calendar day;
        private final float[] gaugeSegments;
        private final int today;
        private final int total;
        private final int paiLow;
        private final int paiModerate;
        private final int paiHigh;
        private final int minutesLow;
        private final int minutesModerate;
        private final int minutesHigh;

        DayData(final Calendar day,
                float[] gaugeSegments,
                final int today,
                final int total,
                final int paiLow,
                final int paiModerate,
                final int paiHigh,
                final int minutesLow,
                final int minutesModerate,
                final int minutesHigh) {
            this.day = day;
            this.gaugeSegments = gaugeSegments;
            this.today = today;
            this.total = total;
            this.paiLow = paiLow;
            this.paiModerate = paiModerate;
            this.paiHigh = paiHigh;
            this.minutesLow = minutesLow;
            this.minutesModerate = minutesModerate;
            this.minutesHigh = minutesHigh;
        }

        public int getTotal() {
            return total;
        }

        public float[] getGaugeSegments() {
            return gaugeSegments;
        }
    }

    protected static class WeekChartsData<T extends ChartData<?>> extends DefaultChartsData<T> {
        public WeekChartsData(final T data,
                              final PreformattedXIndexLabelFormatter xIndexLabelFormatter) {
            super(data, xIndexLabelFormatter);
        }
    }

    protected static class PaiChartsData extends ChartsData {
        private final WeekChartsData<BarData> weekBeforeData;
        private final DayData dayData;

        PaiChartsData(final DayData dayData, final WeekChartsData<BarData> weekBeforeData) {
            this.dayData = dayData;
            this.weekBeforeData = weekBeforeData;
        }

        public DayData getDayData() {
            return dayData;
        }

        public WeekChartsData<BarData> getWeekBeforeData() {
            return weekBeforeData;
        }
    }
}
