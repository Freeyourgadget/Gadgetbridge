package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;

public class TemperatureDailyFragment extends AbstractChartFragment<TemperatureDailyFragment.TemperatureChartData> {

    protected static final Logger LOG = LoggerFactory.getLogger(TemperatureDailyFragment.class);

    protected int TEMPERATURE_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int LEGEND_TEXT_COLOR;

    private TextView dateView;
    private TextView tempAverage;
    private TextView tempMinimum;
    private TextView tempMaximum;
    private LineChart tempLineChart;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_temperature, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        dateView = rootView.findViewById(R.id.temp_date_view);
        tempLineChart = rootView.findViewById(R.id.temp_line_chart);
        tempAverage = rootView.findViewById(R.id.temp_average);
        tempMinimum = rootView.findViewById(R.id.temp_minimum);
        tempMaximum = rootView.findViewById(R.id.temp_maximum);

        setupChart();
        refresh();
        setupLegend(tempLineChart);

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_temperature);
    }

    @Override
    protected void init() {
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        DESCRIPTION_COLOR = LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        TEMPERATURE_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    protected TemperatureChartData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        int startTs = getTSStart();
        int endTs = getTSEnd();

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends TemperatureSample> sampleProvider = coordinator.getTemperatureSampleProvider(device, db.getDaoSession());

        final List<? extends TemperatureSample> samples = sampleProvider.getAllSamples(startTs  * 1000L, endTs  * 1000L);
        LOG.info("Got {} temperature samples", samples.size());

        return new TemperatureChartData(samples);
    }

    @Override
    protected void renderCharts() {
        tempLineChart.invalidate();
    }

    private void setupChart() {
        tempLineChart.setBackgroundColor(BACKGROUND_COLOR);
        tempLineChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        tempLineChart.getDescription().setEnabled(false);


        XAxis x = tempLineChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(86400f);

        YAxis y = tempLineChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(true);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setEnabled(true);
        y.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        y.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());

        YAxis yAxisRight = tempLineChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        yAxisRight.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());

        refresh();
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(1);
        LegendEntry dataEntry = new LegendEntry();
        dataEntry.label = getTitle();
        dataEntry.formColor = TEMPERATURE_COLOR;
        legendEntries.add(dataEntry);

        if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            LegendEntry dataAverageEntry = new LegendEntry();
            dataAverageEntry.label = getString(R.string.hr_average);
            dataAverageEntry.formColor = Color.CYAN;
            legendEntries.add(dataAverageEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
    }

    @Override
    protected void updateChartsnUIThread(TemperatureDailyFragment.TemperatureChartData data) {
        Date date = new Date(getTSEnd() * 1000L);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        dateView.setText(formattedDate);

        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final List<Entry> lineEntries = new ArrayList<>();
        List<? extends TemperatureSample> samples = data.samples;
        final Accumulator accumulator = new Accumulator();

        for (int i =0; i < samples.size(); i++) {
            TemperatureSample sample = samples.get(i);
            int timestamp_in_seconds = (int) (sample.getTimestamp() / 1000L);
            lineEntries.add(new Entry(tsTranslation.shorten(timestamp_in_seconds), sample.getTemperature()));
            accumulator.add(sample.getTemperature());
        }

        LineDataSet dataSet = new LineDataSet(lineEntries, "Heart Rate");
        dataSet.setLineWidth(1.5f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(true);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setColor(TEMPERATURE_COLOR);
        dataSet.setValueTextColor(CHART_TEXT_COLOR);

        final double average = accumulator.getCount() > 0 ? accumulator.getAverage() : -1;
        final double minimum = accumulator.getCount() > 0 ? accumulator.getMin() : -1;
        final double maximum = accumulator.getCount() > 0 ? accumulator.getMax() : -1;

        tempAverage.setText(average > 0 ? String.format(Locale.ROOT, "%.1f", average) : "-");
        tempMinimum.setText(minimum > 0 ? String.format(Locale.ROOT, "%.1f", minimum) : "-");
        tempMaximum.setText(maximum > 0 ? String.format(Locale.ROOT, "%.1f", maximum) : "-");

        if (minimum > 0) {
            long axisMin = Math.max(Math.round(minimum) - 3, 0);
            tempLineChart.getAxisLeft().setAxisMinimum(axisMin);
            tempLineChart.getAxisRight().setAxisMinimum(axisMin);
        }
        if (maximum > 0) {
            tempLineChart.getAxisLeft().setAxisMaximum(Math.round(maximum) + 3);
            tempLineChart.getAxisRight().setAxisMaximum(Math.round(maximum) + 3);
        }

        tempLineChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));
        tempLineChart.setData(new LineData(dataSet));

        tempLineChart.getAxisLeft().removeAllLimitLines();

        if (average > 0 && GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            final LimitLine averageLine = new LimitLine((float) average);
            averageLine.setLineWidth(1.5f);
            averageLine.enableDashedLine(15f, 10f, 0f);
            averageLine.setLineColor(Color.CYAN);
            tempLineChart.getAxisLeft().addLimitLine(averageLine);
        }

    }

    protected static class TemperatureChartData extends ChartsData {
        public List<? extends TemperatureSample> samples;

        protected TemperatureChartData(List<? extends TemperatureSample> samples) {
            this.samples = samples;
        }
    }
}
