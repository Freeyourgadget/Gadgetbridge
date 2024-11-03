/*  Copyright (C) 2023 Alicia Hormann

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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class TemperatureChartFragment extends AbstractChartFragment<TemperatureChartFragment.TemperatureChartsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(TemperatureChartFragment.class);

    private LineChart mTemperatureChart;
    private int BACKGROUND_COLOR;
    private int DESCRIPTION_COLOR;
    private int CHART_TEXT_COLOR;

    protected final int TOTAL_DAYS = getRangeDays();


    @Override
    protected void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(requireContext());
        DESCRIPTION_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());

    }
    private int getRangeDays() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return 30;
        } else {
            return 7;
        }
    }

    @Override
    protected TemperatureChartsData refreshInBackground(final ChartsHost chartsHost, final DBHandler db, final GBDevice device) {
        final List<? extends TemperatureSample> samples = getSamples(db, device);

        LOG.info("Got {} temperature samples", samples.size());
        return new TemperatureChartsDataBuilder(samples).build();
    }


    @Override
    protected void updateChartsnUIThread(final TemperatureChartsData temperatureData) {
        mTemperatureChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mTemperatureChart.getXAxis().setValueFormatter(temperatureData.getXValueFormatter());
        mTemperatureChart.getXAxis().setAvoidFirstLastClipping(true);

        // Using approximately the range of survivable body-temperatures (in celsius), rounded to multiples of 5
        mTemperatureChart.getAxisLeft().setAxisMinimum(30f);
        mTemperatureChart.getAxisLeft().setAxisMaximum(45f);

        mTemperatureChart.setData(temperatureData.getData());
    }

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_temperature);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_temperaturechart, container, false);

        mTemperatureChart = rootView.findViewById(R.id.temperature_line_chart);

        setupLineChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    private void setupLineChart() {
        mTemperatureChart.setBackgroundColor(BACKGROUND_COLOR);
        mTemperatureChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mTemperatureChart);

        final XAxis x = mTemperatureChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        final YAxis yAxisLeft = mTemperatureChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawTopYLabelEntry(false);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setEnabled(true);

        final YAxis yAxisRight = mTemperatureChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawLabels(false);
    }

    @Override
    protected void setupLegend(final Chart<?> chart) {
    }

    @Override
    protected void renderCharts() {
        mTemperatureChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
    }

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    @Override
    protected int getTSStart() {
        return getTSEnd() - TOTAL_DAYS*24*60*60;
    }

    private List<? extends TemperatureSample> getSamples(final DBHandler db, final GBDevice device) {
        final int tsStart = getTSStart();
        final int tsEnd = getTSEnd();
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends TemperatureSample> sampleProvider = coordinator.getTemperatureSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsStart * 1000L, tsEnd * 1000L);
    }

    protected class TemperatureChartsDataBuilder {
        private final List<? extends TemperatureSample> samples;

        public TemperatureChartsDataBuilder(final List<? extends TemperatureSample> samples) {
            this.samples = samples;
        }

        public TemperatureChartsData build() {
            TimestampTranslation tsTranslation = new TimestampTranslation();
            List<Entry> entries = new ArrayList<Entry>();
            long firstTs = 0;

            for (TemperatureSample sample : samples) {
                int timestamp_in_seconds = (int) (sample.getTimestamp() / 1000L);
                entries.add(new Entry(tsTranslation.shorten(timestamp_in_seconds), sample.getTemperature()));
                if (firstTs == 0) {
                    firstTs = sample.getTimestamp();
                }
            }

            LineDataSet dataSet = new LineDataSet(entries, getString(R.string.menuitem_temperature));
            dataSet.setLineWidth(2.2f);
            dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            dataSet.setCubicIntensity(0.1f);
            dataSet.setDrawCircles(true);
            dataSet.setCircleRadius(5f);
            dataSet.setDrawCircleHole(false);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(CHART_TEXT_COLOR);
            dataSet.setHighlightEnabled(true);
            dataSet.setValueFormatter(new MyValueFormatter());
            LineData lineData = new LineData(dataSet);

            return new TemperatureChartsData(lineData, tsTranslation);
        }
    }

    protected static class TemperatureChartsData extends DefaultChartsData<LineData> {
        public TemperatureChartsData(LineData lineData, TimestampTranslation tsTranslation) {
            super(lineData, new dateFormatter(tsTranslation));
        }
    }


    protected static class dateFormatter extends ValueFormatter {
        private final TimestampTranslation tsTranslation;
        SimpleDateFormat annotationDateFormat = new SimpleDateFormat("dd.MM.");
        Calendar cal = GregorianCalendar.getInstance();

        public dateFormatter(TimestampTranslation tsTranslation) {
            this.tsTranslation = tsTranslation;
        }

        @Override
        public String getFormattedValue(float value) {
            cal.clear();
            int ts = (int) value;
            cal.setTimeInMillis(tsTranslation.toOriginalValue(ts) * 1000L);
            Date date = cal.getTime();
            return annotationDateFormat.format(date);
        }
    }

    protected static class MyValueFormatter extends ValueFormatter {
        private final DecimalFormat formatter = new DecimalFormat("0.00");

        @Override
        public String getPointLabel(Entry entry) {
            return formatter.format(entry.getY());
        }
    }

}
