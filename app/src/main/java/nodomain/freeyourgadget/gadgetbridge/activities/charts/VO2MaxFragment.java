package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;


public class VO2MaxFragment extends AbstractChartFragment<VO2MaxFragment.VO2MaxData> {
    protected static final Logger LOG = LoggerFactory.getLogger(VO2MaxFragment.class);

    private TextView mDateView;
    private TextView vo2MaxGeneralValue;
    private TextView vo2MaxRunningValue;
    private TextView vo2MaxCyclingValue;
    private ImageView vo2MaxGeneralGauge;
    private ImageView vo2MaxRunningGauge;
    private ImageView vo2MaxCyclingGauge;
    protected GaugeDrawer gaugeDrawer = new GaugeDrawer();
    private LineChart vo2MaxChart;
    private RelativeLayout vo2maxCyclingWrapper;
    private RelativeLayout vo2maxRunningWrapper;
    private RelativeLayout vo2maxGeneralWrapper;
    private GridLayout tilesGridWrapper;
    private int tsFrom;
    GBDevice device;

    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_vo2max, container, false);

        mDateView = rootView.findViewById(R.id.vo2max_date_view);
        vo2MaxGeneralValue = rootView.findViewById(R.id.vo2max_general_gauge_value);
        vo2MaxRunningValue = rootView.findViewById(R.id.vo2max_running_gauge_value);
        vo2MaxCyclingValue = rootView.findViewById(R.id.vo2max_cycling_gauge_value);
        vo2MaxGeneralGauge = rootView.findViewById(R.id.vo2max_general_gauge);
        vo2MaxRunningGauge = rootView.findViewById(R.id.vo2max_running_gauge);
        vo2MaxCyclingGauge = rootView.findViewById(R.id.vo2max_cycling_gauge);
        vo2MaxChart = rootView.findViewById(R.id.vo2max_chart);
        vo2maxCyclingWrapper = rootView.findViewById(R.id.vo2max_cycling_card_layout);
        vo2maxGeneralWrapper = rootView.findViewById(R.id.vo2max_general_card_layout);
        vo2maxRunningWrapper = rootView.findViewById(R.id.vo2max_running_card_layout);
        tilesGridWrapper = rootView.findViewById(R.id.tiles_grid_wrapper);
        device = getChartsHost().getDevice();
        if (!supportsVO2MaxCycling(device)) {
            tilesGridWrapper.removeView(vo2maxCyclingWrapper);
        }
        if (!supportsVO2MaxRunning(device)) {
            tilesGridWrapper.removeView(vo2maxRunningWrapper);
        }
        if (!supportsVO2MaxGeneral(device)) {
            tilesGridWrapper.removeView(vo2maxGeneralWrapper);
        }
        setupVO2MaxChart();
        refresh();


        return rootView;
    }

    public boolean supportsVO2MaxCycling(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator != null && coordinator.supportsVO2MaxCycling();
    }

    public boolean supportsVO2MaxGeneral(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator != null && coordinator.supportsVO2MaxGeneral();
    }

    public boolean supportsVO2MaxRunning(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator != null && coordinator.supportsVO2MaxRunning();
    }

    @Override
    public String getTitle() {
        return getString(R.string.vo2max);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    protected VO2MaxData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        mDateView.setText(formattedDate);
        List<VO2MaxRecord> records = new ArrayList<>();
        int tsEnd = getTSEnd();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(tsEnd * 1000L); //we need today initially, which is the end of the time range
        day.set(Calendar.HOUR_OF_DAY, 0); //and we set time for the start and end of the same day
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.add(Calendar.DAY_OF_YEAR,  -30);
        tsFrom = (int) (day.getTimeInMillis() / 1000);
        List<? extends Vo2MaxSample> samples = getAllSamples(db, device, tsFrom, tsEnd);
        for (Vo2MaxSample sample : samples) {
            records.add(new VO2MaxRecord(sample.getTimestamp() / 1000, sample.getValue(), sample.getType()));
        }
        Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues = new HashMap<>();
        for (Vo2MaxSample.Type type : Vo2MaxSample.Type.values()) {
            Vo2MaxSample sample = getLatestVo2MaxSample(db, device, type);
            if (sample != null) {
                latestValues.put(type, new VO2MaxRecord(sample.getTimestamp() / 1000, sample.getValue(), type));
            }
        }
        return new VO2MaxData(records, latestValues);
    }

    @Override
    protected void updateChartsnUIThread(VO2MaxData vo2MaxData) {
        TimestampTranslation tsTranslation = new TimestampTranslation();
        List<Entry> runningEntries = new ArrayList<>();
        List<Entry> cyclingEntries = new ArrayList<>();
        List<Entry> generalEntries = new ArrayList<>();
        vo2MaxData.records.forEach((record) -> {
            float nd = (float) (record.timestamp - this.tsFrom) / (60 * 60 * 24);
            switch (record.type) {
                case RUNNING:
                    runningEntries.add(new Entry(nd, record.value));
                    break;
                case CYCLING:
                    cyclingEntries.add(new Entry(nd, record.value));
                    break;
                case GENERAL:
                    generalEntries.add(new Entry(nd, record.value));
                    break;
            }
        });
        final int[] colors = {
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_poor_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_good_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_excellent_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_superior_color),
        };
        final float[] segments = {
                0.20F,
                0.20F,
                0.20F,
                0.20F,
                0.20F,
        };
        float[] vo2MaxRanges = {
                55.4F,
                51.1F,
                45.4F,
                41.7F,
                0.0F,
        };

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        if (supportsVO2MaxGeneral(device)) {
            VO2MaxRecord latestGeneralRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.GENERAL);
            float generalVO2MaxValue = calculateVO2maxGaugeValue(vo2MaxRanges, latestGeneralRecord != null ? latestGeneralRecord.value : 0);
            gaugeDrawer.drawSegmentedGauge(vo2MaxGeneralGauge, colors, segments, generalVO2MaxValue, false, true);
            vo2MaxGeneralValue.setText(String.valueOf(latestGeneralRecord != null ? Math.round(latestGeneralRecord.value) : "-"));
            lineDataSets.add(createDataSet(generalEntries, getResources().getColor(R.color.vo2max_general_char_line_color), getString(R.string.vo2_max_general)));
        }
        if (supportsVO2MaxRunning(device)) {
            VO2MaxRecord latestRunningRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.RUNNING);
            float runningVO2MaxValue = calculateVO2maxGaugeValue(vo2MaxRanges, latestRunningRecord != null ? latestRunningRecord.value : 0);
            vo2MaxRunningValue.setText(String.valueOf(latestRunningRecord != null ? Math.round(latestRunningRecord.value) : "-"));
            gaugeDrawer.drawSegmentedGauge(vo2MaxRunningGauge, colors, segments, runningVO2MaxValue, false, true);
            lineDataSets.add(createDataSet(runningEntries, getResources().getColor(R.color.vo2max_running_char_line_color), getString(R.string.vo2_max_running)));
        }
        if (supportsVO2MaxCycling(device)) {
            VO2MaxRecord latestCyclingRecord = vo2MaxData.getLatestValue(Vo2MaxSample.Type.CYCLING);
            float cyclingVO2MaxValue = calculateVO2maxGaugeValue(vo2MaxRanges, latestCyclingRecord != null ? latestCyclingRecord.value : 0);
            gaugeDrawer.drawSegmentedGauge(vo2MaxCyclingGauge, colors, segments, cyclingVO2MaxValue, false, true);
            vo2MaxCyclingValue.setText(String.valueOf(latestCyclingRecord != null ? Math.round(latestCyclingRecord.value) : "-"));
            lineDataSets.add(createDataSet(cyclingEntries, getResources().getColor(R.color.vo2max_cycling_char_line_color), getString(R.string.vo2_max_cycling)));
        }
        final LineData lineData = new LineData(lineDataSets);
        vo2MaxChart.getXAxis().setValueFormatter(getVO2MaxLineChartValueFormatter());
        vo2MaxChart.setData(lineData);
    }

    ValueFormatter getVO2MaxLineChartValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar day = Calendar.getInstance();
                day.setTimeInMillis(tsFrom * 1000L);
                day.add(Calendar.DAY_OF_YEAR, (int) value);
                return new SimpleDateFormat("dd/MM").format(day.getTime());
            }
        };
    }

    private float calculateVO2maxGaugeValue(float[] vo2MaxRanges, float vo2MaxValue) {
        float value = -1;
        for (int i = 0; i < vo2MaxRanges.length; i++) {
            if (vo2MaxValue - vo2MaxRanges[i] > 0) {
                float rangeValue = i - 1 >= 0 ? vo2MaxRanges[i-1] : 60F;
                float rangeDiff = rangeValue - vo2MaxRanges[i];
                float valueDiff = vo2MaxValue - vo2MaxRanges[i];
                float multiplayer = valueDiff / rangeDiff;
                value = (4 - i) * 0.2F + 0.2F * (multiplayer > 1 ? 1 : multiplayer) ;
                break;
            }
        }
        return value;
    }

    protected LineDataSet createDataSet(final List<Entry> values, int color, String label) {
        final LineDataSet lineDataSet = new LineDataSet(values, label);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setCircleColor(color);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(true);
        lineDataSet.setValueTextSize(10f);
        lineDataSet.setValueTextColor(CHART_TEXT_COLOR);
        lineDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.ROOT, "%d", Math.round(value));
            }
        });
        return lineDataSet;
    }

    @Override
    protected void renderCharts() {
        vo2MaxChart.invalidate();
    }


    public List<? extends Vo2MaxSample> getAllSamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends Vo2MaxSample> sampleProvider = coordinator.getVo2MaxSampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    public Vo2MaxSample getLatestVo2MaxSample(final DBHandler db, final GBDevice device, Vo2MaxSample.Type type) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final Vo2MaxSampleProvider sampleProvider = (Vo2MaxSampleProvider) coordinator.getVo2MaxSampleProvider(device, db.getDaoSession());
        return  sampleProvider.getLatestSample(type, getTSEnd() * 1000L);
    }

    private void setupVO2MaxChart() {
        final XAxis xAxisBottom = vo2MaxChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(31f);
        xAxisBottom.setGranularity(1f);
        xAxisBottom.setGranularityEnabled(true);

        final YAxis yAxisLeft = vo2MaxChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = vo2MaxChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    protected void setupLegend(Chart<?> chart) {}

    protected static class VO2MaxRecord {
        float value;
        long timestamp;
        Vo2MaxSample.Type type;

        protected VO2MaxRecord(long timestamp, float value, Vo2MaxSample.Type type) {
            this.timestamp = timestamp;
            this.value = value;
            this.type = type;
        }
    }

    protected static class VO2MaxData extends ChartsData {
        private final List<? extends VO2MaxRecord> records;
        private final Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues;

        protected VO2MaxData(List<? extends VO2MaxRecord> records, Map<Vo2MaxSample.Type, VO2MaxRecord> latestValues) {
            this.records = records;
            this.latestValues = latestValues;
        }

        @Nullable
        public VO2MaxRecord getLatestValue(Vo2MaxSample.Type type) {
            return this.latestValues.get(type);
        }
    }
}