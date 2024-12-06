package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class HeartRateDailyFragment extends AbstractChartFragment<HeartRateDailyFragment.HeartRateData> {

    protected int HEARTRATE_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int LEGEND_TEXT_COLOR;

    private TextView mDateView;
    private TextView hrResting;
    private TextView hrAverage;
    private TextView hrMinimum;
    private TextView hrMaximum;
    private LineChart hrLineChart;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_heart_rate, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.hr_date_view);
        hrLineChart = rootView.findViewById(R.id.heart_rate_line_chart);
        hrResting = rootView.findViewById(R.id.hr_resting);
        hrAverage = rootView.findViewById(R.id.hr_average);
        hrMinimum = rootView.findViewById(R.id.hr_minimum);
        hrMaximum = rootView.findViewById(R.id.hr_maximum);
        final LinearLayout heartRateRestingWrapper = rootView.findViewById(R.id.hr_resting_wrapper);

        setupChart();
        refresh();
        setupLegend(hrLineChart);

        if (!supportsHeartRateRestingMeasurement()) {
            heartRateRestingWrapper.setVisibility(View.GONE);
        }

        return rootView;
    }

    public boolean supportsHeartRateRestingMeasurement() {
        final GBDevice device = getChartsHost().getDevice();
        return device.getDeviceCoordinator().supportsHeartRateRestingMeasurement(device);
    }

    protected List<? extends AbstractActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = device.getDeviceCoordinator().getSampleProvider(device, db.getDaoSession());
        return provider.getAllActivitySamplesHighRes(tsFrom, tsTo);
    }

    @Override
    public String getTitle() {
        return getString(R.string.heart_rate);
    }

    @Override
    protected void init() {
        Prefs prefs = GBApplication.getPrefs();
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
        DESCRIPTION_COLOR = LEGEND_TEXT_COLOR = GBApplication.getTextColor(getContext());
        if (prefs.getBoolean("chart_heartrate_color", false)) {
            HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_alternative);
        }else{
            HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate);
        }
    }

    @Override
    protected HeartRateDailyFragment.HeartRateData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        day.add(Calendar.DATE, 0);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000);
        int endTs = startTs + 24 * 60 * 60 - 1;

        List<? extends ActivitySample> samples = getActivitySamples(db, device, startTs, endTs);

        int restingHeartRate = -1;
        if (supportsHeartRateRestingMeasurement()) {
            restingHeartRate = device.getDeviceCoordinator()
                    .getHeartRateRestingSampleProvider(device, db.getDaoSession())
                    .getAllSamples(startTs * 1000L, endTs * 1000L)
                    .stream()
                    .max(Comparator.comparingLong(HeartRateSample::getTimestamp))
                    .map(HeartRateSample::getHeartRate)
                    .orElse(-1);
        }

        return new HeartRateData(samples, restingHeartRate);
    }

    @Override
    protected void renderCharts() {
        hrLineChart.invalidate();
    }

    private void setupChart() {
        hrLineChart.setBackgroundColor(BACKGROUND_COLOR);
        hrLineChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        hrLineChart.getDescription().setEnabled(false);


        XAxis x = hrLineChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setAxisMinimum(0f);
        x.setAxisMaximum(86400f);

        YAxis y = hrLineChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setDrawTopYLabelEntry(true);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setEnabled(true);
        y.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        y.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());

        YAxis yAxisRight = hrLineChart.getAxisRight();
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
        LegendEntry hrEntry = new LegendEntry();
        hrEntry.label = getTitle();
        hrEntry.formColor = HEARTRATE_COLOR;
        legendEntries.add(hrEntry);

        if (GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            LegendEntry hrAverageEntry = new LegendEntry();
            hrAverageEntry.label = getString(R.string.hr_average);
            hrAverageEntry.formColor = Color.RED;
            legendEntries.add(hrAverageEntry);
        }

        //if (supportsHeartRateRestingMeasurement()) {
        //    LegendEntry hrRestingEntry = new LegendEntry();
        //    hrRestingEntry.label = getString(R.string.hr_resting);
        //    hrRestingEntry.formColor = Color.GRAY;
        //    legendEntries.add(hrRestingEntry);
        //}

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
    }

    protected LineDataSet createHeartRateDataSet(final List<Entry> values) {
        LineDataSet dataSet = new LineDataSet(values, "Heart Rate");
        dataSet.setLineWidth(1.5f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(true);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setColor(HEARTRATE_COLOR);
        dataSet.setValueTextColor(CHART_TEXT_COLOR);
        return dataSet;
    }

    @Override
    protected void updateChartsnUIThread(HeartRateDailyFragment.HeartRateData data) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        day.add(Calendar.DATE, 0);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.HOUR, 0);
        int startTs = (int) (day.getTimeInMillis() / 1000);
        int endTs = startTs + 24 * 60 * 60 - 1;
        Date date = new Date((long) endTs * 1000);
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(date);
        mDateView.setText(formattedDate);

        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        final List<Entry> lineEntries = new ArrayList<>();
        List<? extends ActivitySample> samples = data.samples;
        final Accumulator accumulator = new Accumulator();

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        int lastTsShorten = 0;
        for (int i =0; i < samples.size(); i++) {
            final ActivitySample sample = samples.get(i);
            final int tsShorten = tsTranslation.shorten(sample.getTimestamp());
            if (!heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                continue;
            }
            if (lastTsShorten == 0 || (tsShorten - lastTsShorten) <= 60 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                lineEntries.add(new Entry(tsShorten, sample.getHeartRate()));
            } else {
                if (!lineEntries.isEmpty()) {
                    List<Entry> clone = new ArrayList<>(lineEntries.size());
                    clone.addAll(lineEntries);
                    lineDataSets.add(createHeartRateDataSet(clone));
                    lineEntries.clear();
                }
            }
            lastTsShorten = tsShorten;
            lineEntries.add(new Entry(tsShorten, sample.getHeartRate()));
            accumulator.add(sample.getHeartRate());
        }

        if (!lineEntries.isEmpty()) {
            lineDataSets.add(createHeartRateDataSet(lineEntries));
        }

        final int average = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getAverage()) : -1;
        final int minimum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMin()) : -1;
        final int maximum = accumulator.getCount() > 0 ? (int) Math.round(accumulator.getMax()) : -1;

        hrAverage.setText(average > 0 ? getString(R.string.bpm_value_unit, average) : "-");
        hrMinimum.setText(minimum > 0 ? getString(R.string.bpm_value_unit, minimum) : "-");
        hrMaximum.setText(maximum > 0 ? getString(R.string.bpm_value_unit, maximum) : "-");
        hrResting.setText(data.restingHeartRate > 0 ? getString(R.string.bpm_value_unit, data.restingHeartRate) : "-");

        if (minimum > 0) {
            hrLineChart.getAxisLeft().setAxisMinimum(Math.max(minimum - 30, 0));
            hrLineChart.getAxisRight().setAxisMinimum(Math.max(minimum - 30, 0));
        }
        if (maximum > 0) {
            hrLineChart.getAxisLeft().setAxisMaximum(maximum + 30);
            hrLineChart.getAxisRight().setAxisMaximum(maximum + 30);
        }

        hrLineChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));
        hrLineChart.setData(new LineData(lineDataSets));

        hrLineChart.getAxisLeft().removeAllLimitLines();

        if (average > 0 && GBApplication.getPrefs().getBoolean("charts_show_average", true)) {
            final LimitLine averageLine = new LimitLine(average);
            averageLine.setLineWidth(1.5f);
            averageLine.enableDashedLine(15f, 10f, 0f);
            averageLine.setLineColor(Color.RED);
            hrLineChart.getAxisLeft().addLimitLine(averageLine);
        }

        //if (data.restingHeartRate > 0) {
        //    final LimitLine restingLine = new LimitLine(data.restingHeartRate);
        //    restingLine.setLineWidth(1.5f);
        //    restingLine.enableDashedLine(15f, 10f, 0f);
        //    restingLine.setLineColor(Color.GRAY);
        //    hrLineChart.getAxisLeft().addLimitLine(restingLine);
        //}
    }

    protected static class HeartRateData extends ChartsData {
        public List<? extends ActivitySample> samples;
        public int restingHeartRate;

        protected HeartRateData(List<? extends ActivitySample> samples, int restingHeartRate) {
            this.samples = samples;
            this.restingHeartRate = restingHeartRate;
        }
    }
}
