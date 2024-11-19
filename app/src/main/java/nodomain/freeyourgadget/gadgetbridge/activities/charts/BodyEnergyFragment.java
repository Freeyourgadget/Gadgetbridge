package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;


public class BodyEnergyFragment extends AbstractChartFragment<BodyEnergyFragment.BodyEnergyData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;
    private ImageView bodyEnergyGauge;
    private TextView bodyEnergyGained;
    private TextView bodyEnergyLost;
    private LineChart bodyEnergyChart;

    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int TEXT_COLOR;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_body_energy, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.body_energy_date_view);
        bodyEnergyGauge = rootView.findViewById(R.id.body_energy_gauge);
        bodyEnergyGained = rootView.findViewById(R.id.body_energy_gained);
        bodyEnergyLost = rootView.findViewById(R.id.body_energy_lost);
        bodyEnergyChart = rootView.findViewById(R.id.body_energy_chart);
        setupBodyEnergyLevelChart();
        refresh();


        return rootView;
    }


    @Override
    public String getTitle() {
        return getString(R.string.body_energy);
    }

    @Override
    protected void init() {
        TEXT_COLOR = GBApplication.getTextColor(requireContext());
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(requireContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
    }

    @Override
    protected BodyEnergyData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends BodyEnergySample> samples = getBodyEnergySamples(db, device, getTSStart(), getTSEnd());
        return new BodyEnergyData(samples);
    }

    @Override
    protected void updateChartsnUIThread(BodyEnergyData bodyEnergyData) {
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(getEndDate());
        mDateView.setText(formattedDate);

        List<Entry> lineEntries = new ArrayList<>();
        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        final AtomicInteger gainedValue = new AtomicInteger(0);
        final AtomicInteger drainedValue = new AtomicInteger(0);
        int newestValue = 0;
        long referencedTimestamp;
        if (!bodyEnergyData.samples.isEmpty()) {
            newestValue = bodyEnergyData.samples.get(bodyEnergyData.samples.size() - 1).getEnergy();
            referencedTimestamp = bodyEnergyData.samples.get(0).getTimestamp();
            final AtomicInteger[] lastValue = {new AtomicInteger(0)};
            bodyEnergyData.samples.forEach((sample) -> {
                if (sample.getEnergy() < lastValue[0].intValue()) {
                    drainedValue.set(drainedValue.get() + lastValue[0].intValue() - sample.getEnergy());
                } else if (lastValue[0].intValue() > 0 && sample.getEnergy() > lastValue[0].intValue()) {
                    gainedValue.set(gainedValue.get() + sample.getEnergy() - lastValue[0].intValue());
                }
                lastValue[0].set(sample.getEnergy());
                float x = (float) sample.getTimestamp() / 1000 - (float) referencedTimestamp / 1000;
                lineEntries.add(new Entry(x, sample.getEnergy()));
            });
        }

        final LineDataSet lineDataSet = new LineDataSet(lineEntries, getString(R.string.body_energy_legend_level));
        lineDataSet.setColor(getResources().getColor(R.color.body_energy_level_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.body_energy_level_color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillAlpha(60);
        lineDataSet.setFillColor(getResources().getColor(R.color.body_energy_level_color ));

        List<LegendEntry> legendEntries = new ArrayList<>(1);
        LegendEntry activityEntry = new LegendEntry();
        activityEntry.label = getString(R.string.body_energy_legend_level);
        activityEntry.formColor = getResources().getColor(R.color.body_energy_level_color);
        legendEntries.add(activityEntry);
        bodyEnergyChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        bodyEnergyChart.getLegend().setCustom(legendEntries);

        lineDataSets.add(lineDataSet);
        final LineData lineData = new LineData(lineDataSets);
        bodyEnergyChart.setData(lineData);

        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );

        bodyEnergyGauge.setImageBitmap(drawGauge(
                width,
                width / 15,
                getResources().getColor(R.color.body_energy_level_color),
                newestValue,
                100
        ));
        bodyEnergyGained.setText(String.format("+ %s", gainedValue.intValue()));
        bodyEnergyLost.setText(String.format("- %s", drainedValue));
    }

    @Override
    protected void renderCharts() {
        bodyEnergyChart.invalidate();
    }


    public List<? extends BodyEnergySample> getBodyEnergySamples(final DBHandler db, final GBDevice device, int tsFrom, int tsTo) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(tsTo * 1000L); //we need today initially, which is the end of the time range
        day.set(Calendar.HOUR_OF_DAY, 0); //and we set time for the start and end of the same day
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        tsFrom = (int) (day.getTimeInMillis() / 1000);
        tsTo = tsFrom + 24 * 60 * 60 - 1;

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();
        final TimeSampleProvider<? extends BodyEnergySample> sampleProvider = coordinator.getBodyEnergySampleProvider(device, db.getDaoSession());
        return sampleProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
    }

    protected void setupLegend(Chart<?> chart) {}

    Bitmap drawGauge(int width, int barWidth, @ColorInt int filledColor, int value, int maxValue) {
        int height = width;
        int barMargin = (int) Math.ceil(barWidth / 2f);
        float filledFactor = (float) value / maxValue;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(barWidth);
        paint.setColor(getResources().getColor(R.color.gauge_line_color));
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                width - barMargin,
                120,
                300,
                false,
                paint);
        paint.setStrokeWidth(barWidth);
        paint.setColor(filledColor);
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                height - barMargin,
                120,
                300 * filledFactor,
                false,
                paint
        );

        Paint textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        float textPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.06f, requireContext().getResources().getDisplayMetrics());
        textPaint.setTextSize(textPixels);
        textPaint.setTextAlign(Paint.Align.CENTER);
        int yPos = (int) ((float) height / 2 - ((textPaint.descent() + textPaint.ascent()) / 2)) ;
        canvas.drawText(String.valueOf(value), width / 2f, yPos, textPaint);
        Paint textLowerPaint = new Paint();
        textLowerPaint.setColor(TEXT_COLOR);
        textLowerPaint.setTextAlign(Paint.Align.CENTER);
        float textLowerPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, width * 0.025f, requireContext().getResources().getDisplayMetrics());
        textLowerPaint.setTextSize(textLowerPixels);
        int yPosLowerText = (int) ((float) height / 2 - textPaint.ascent()) ;
        canvas.drawText(String.valueOf(maxValue), width / 2f, yPosLowerText, textLowerPaint);

        return bitmap;
    }

    private void setupBodyEnergyLevelChart() {
        bodyEnergyChart.getDescription().setEnabled(false);
        bodyEnergyChart.setTouchEnabled(false);
        bodyEnergyChart.setPinchZoom(false);
        bodyEnergyChart.setDoubleTapToZoomEnabled(false);


        final XAxis xAxisBottom = bodyEnergyChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        xAxisBottom.setLabelCount(7, true);
        xAxisBottom.setValueFormatter(getBodyEnergyChartXValueFormatter());

        final YAxis yAxisLeft = bodyEnergyChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMaximum(100);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = bodyEnergyChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);

    }

    ValueFormatter getBodyEnergyChartXValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                long timestamp = (long) (value * 1000);
                Date date = new Date ();
                date.setTime(timestamp);
                SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                return df.format(date);
            }
        };
    }

    protected static class BodyEnergyData extends ChartsData {
        private final List<? extends BodyEnergySample> samples;

        protected BodyEnergyData(List<? extends BodyEnergySample> samples) {
            this.samples = samples;
        }
    }
}