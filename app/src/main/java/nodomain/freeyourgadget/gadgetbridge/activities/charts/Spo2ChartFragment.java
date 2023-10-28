/*  Copyright (C) 2023 José Rebelo, MartinJM

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

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

// Based on StressChartFragment

public class Spo2ChartFragment extends AbstractChartFragment<Spo2ChartFragment.Spo2ChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(Spo2ChartFragment.class);

    private LineChart mSpo2Chart;

    private int BACKGROUND_COLOR;
    private int DESCRIPTION_COLOR;
    private int CHART_TEXT_COLOR;
    private int LEGEND_TEXT_COLOR;
    private int CHART_LINE_COLOR;

    private String SPO2_AVERAGE_LABEL;

    private final Prefs prefs = GBApplication.getPrefs();

    private final boolean CHARTS_SLEEP_RANGE_24H = prefs.getBoolean("chart_sleep_range_24h", false);
    private final boolean SHOW_CHARTS_AVERAGE = prefs.getBoolean("charts_show_average", true);

    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());

        if (prefs.getBoolean("chart_heartrate_color", false)) {
            CHART_LINE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_alternative);
        } else {
            CHART_LINE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate);
        }

        SPO2_AVERAGE_LABEL = requireContext().getString(R.string.charts_legend_spo2_average);
    }

    @Override
    protected Spo2ChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        final List<? extends Spo2Sample> samples = getSamples(db, device);

        LOG.info("Got {} SpO2 samples", samples.size());

        return new Spo2ChartsDataBuilder(samples).build();
    }

    protected LineDataSet createDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, "SpO2");
        lineDataSet.setColor(CHART_LINE_COLOR);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2.2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setValueTextColor(CHART_TEXT_COLOR);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", (int) value);
            }
        });
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(final Spo2ChartsData spo2Data) {
        final DefaultChartsData<LineData> chartsData = spo2Data.getChartsData();
        mSpo2Chart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mSpo2Chart.getXAxis().setValueFormatter(chartsData.getXValueFormatter());
        mSpo2Chart.setData(chartsData.getData());
        mSpo2Chart.getAxisLeft().removeAllLimitLines();

        LOG.info("SpO2 average: " + spo2Data.getAverage());

        if (spo2Data.getAverage() > 0 && SHOW_CHARTS_AVERAGE) {
            final LimitLine averageLine = new LimitLine(spo2Data.getAverage());
            averageLine.setLineColor(Color.RED);
            averageLine.setLineWidth(0.1f);
            mSpo2Chart.getAxisLeft().addLimitLine(averageLine);
        }

        mSpo2Chart.getAxisRight().setEnabled(false);
    }

    @Override
    public String getTitle() {
        return requireContext().getString(R.string.pref_header_spo2);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_charts, container, false);

        mSpo2Chart = rootView.findViewById(R.id.activitysleepchart);

        setupLineChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    private void setupLineChart() {
        mSpo2Chart.setBackgroundColor(BACKGROUND_COLOR);
        mSpo2Chart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mSpo2Chart);

        final XAxis x = mSpo2Chart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        final YAxis yAxisLeft = mSpo2Chart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100f);
        yAxisLeft.setAxisMinimum(75f);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {
        final List<LegendEntry> legendEntries = new ArrayList<>(2);

        final LegendEntry entry = new LegendEntry();
        entry.label = requireContext().getString(R.string.pref_header_spo2);
        entry.formColor = CHART_LINE_COLOR;
        legendEntries.add(entry);

        if (SHOW_CHARTS_AVERAGE) {
            final LegendEntry averageEntry = new LegendEntry();
            averageEntry.label = SPO2_AVERAGE_LABEL;
            averageEntry.formColor = Color.RED;
            legendEntries.add(averageEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected void renderCharts() {
        mSpo2Chart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
    }

    private List<? extends Spo2Sample> getSamples(final DBHandler db, final GBDevice device) {
        final int tsStart = getTSStart();
        final int tsEnd = getTSEnd();
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends Spo2Sample> sampleProvider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsStart * 1000L, tsEnd * 1000L);
    }

    protected class Spo2ChartsDataBuilder {
        private final List<? extends Spo2Sample> samples;

        private final TimestampTranslation tsTranslation = new TimestampTranslation();

        private final List<Entry> lineEntries = new ArrayList<>();

        long averageSum;
        long averageNumSamples;

        public Spo2ChartsDataBuilder(final List<? extends Spo2Sample> samples) {
            this.samples = samples;
        }

        private void reset() {
            tsTranslation.reset();
            lineEntries.clear();

            averageSum = 0;
            averageNumSamples = 0;
        }

        private void processSamples() {
            reset();

            for (final Spo2Sample sample : samples) {
                processSample(sample);
            }
        }

        private void processSample(final Spo2Sample sample) {
            final int ts = tsTranslation.shorten((int) (sample.getTimestamp() / 1000L));
            lineEntries.add(new Entry(ts, sample.getSpo2()));

            averageSum += sample.getSpo2();
            averageNumSamples += 1;
        }

        public Spo2ChartsData build() {
            processSamples();

            final List<ILineDataSet> lineDataSets = new ArrayList<>();

            lineDataSets.add(createDataSet(lineEntries));

            final LineData lineData = new LineData(lineDataSets);
            final ValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation);
            final DefaultChartsData<LineData> chartsData = new DefaultChartsData<>(lineData, xValueFormatter);
            return new Spo2ChartsData(chartsData, Math.round((float) averageSum / averageNumSamples));
        }
    }

    protected static class Spo2ChartsData extends ChartsData {
        private final DefaultChartsData<LineData> chartsData;
        private final int average;

        public Spo2ChartsData(final DefaultChartsData<LineData> chartsData, final int average) {
            this.chartsData = chartsData;
            this.average = average;
        }

        public DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }

        public int getAverage() {
            return average;
        }
    }
}
