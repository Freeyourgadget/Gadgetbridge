package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class RespiratoryRateDailyFragment extends RespiratoryRateFragment<RespiratoryRateFragment.RespiratoryRateDay> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;
    private TextView sleepAvg;
    private TextView awakeAvg;
    private TextView lowest;
    private TextView highest;
    private LineChart respiratoryRateChart;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_respiratory_rate, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.rr_date_view);
        awakeAvg = rootView.findViewById(R.id.awake_avg);
        sleepAvg = rootView.findViewById(R.id.sleep_avg);
        lowest = rootView.findViewById(R.id.day_lowest);
        highest = rootView.findViewById(R.id.day_highest);
        respiratoryRateChart = rootView.findViewById(R.id.respiratory_rate_line_chart);
        setupRespiratoryRateChart();
        refresh();

        return rootView;
    }

        @Override
    public String getTitle() {
        return getString(R.string.respiratoryrate);
    }

    @Override
    protected RespiratoryRateDay refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(chartsHost.getEndDate());
        mDateView.setText(formattedDate);
        List<RespiratoryRateDay> stepsDayList = getMyRespiratoryRateDaysData(db, day, device);
        final RespiratoryRateDay RespiratoryRateDay;
        if (stepsDayList.isEmpty()) {
            LOG.error("Failed to get RespiratoryRateDay for {}", day);
            List<? extends AbstractRespiratoryRateSample> s = new ArrayList<>();
            RespiratoryRateDay = new RespiratoryRateDay(day, new ArrayList<>(), new ArrayList<>());
        } else {
            RespiratoryRateDay = stepsDayList.get(0);
        }
        return RespiratoryRateDay;
    }

    @Override
    protected void updateChartsnUIThread(RespiratoryRateFragment.RespiratoryRateDay respiratoryRateDay) {
        awakeAvg.setText(String.format(String.valueOf(respiratoryRateDay.awakeRateAvg)));
        sleepAvg.setText(String.valueOf(respiratoryRateDay.sleepRateAvg));
        lowest.setText(String.valueOf(respiratoryRateDay.rateLowest));
        highest.setText(String.valueOf(respiratoryRateDay.rateHighest));

        // Chart
        final List<LegendEntry> legendEntries = new ArrayList<>(1);
        final LegendEntry respiratoryRateEntry = new LegendEntry();
        respiratoryRateEntry.label = getString(R.string.respiratoryrate);
        respiratoryRateEntry.formColor = getResources().getColor(R.color.respiratory_rate_color);
        legendEntries.add(respiratoryRateEntry);
        respiratoryRateChart.getLegend().setTextColor(TEXT_COLOR);
        respiratoryRateChart.getLegend().setCustom(legendEntries);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        List<Entry> lineEntries = new ArrayList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        int lastTsShorten = 0;
        for (final AbstractRespiratoryRateSample sample : respiratoryRateDay.respiratoryRateSamples) {
            int ts = (int) (sample.getTimestamp() / 1000L);
            int tsShorten = tsTranslation.shorten(ts);
            if (lastTsShorten == 0 || (tsShorten - lastTsShorten) <= 300) {
                lineEntries.add(new Entry(tsShorten, (int) sample.getRespiratoryRate()));
            } else {
                if (!lineEntries.isEmpty()) {
                    List<Entry> clone = new ArrayList<>(lineEntries.size());
                    clone.addAll(lineEntries);
                    lineDataSets.add(createDataSet(clone));
                    lineEntries.clear();
                }
            }
            lastTsShorten = tsShorten;
            lineEntries.add(new Entry(tsShorten, (int) sample.getRespiratoryRate()));
        }

        if (!lineEntries.isEmpty()) {
            lineDataSets.add(createDataSet(lineEntries));
        }

        respiratoryRateChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));
        if (respiratoryRateDay.rateLowest > 0 && respiratoryRateDay.rateHighest > 0) {
            final YAxis yAxisLeft = respiratoryRateChart.getAxisLeft();
            yAxisLeft.setAxisMaximum(Math.max(respiratoryRateDay.rateHighest + 3, 20));
        }

        final LineDataSet lineDataSet = new LineDataSet(lineEntries, getString(R.string.respiratoryrate));
        lineDataSet.setColor(getResources().getColor(R.color.respiratory_rate_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.respiratory_rate_color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineDataSets.add(lineDataSet);
        final LineData lineData = new LineData(lineDataSets);
        respiratoryRateChart.setData(lineData);
    }

    protected LineDataSet createDataSet(final List<Entry> values) {
        final LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.respiratoryrate));
        lineDataSet.setColor(getResources().getColor(R.color.respiratory_rate_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.respiratory_rate_color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        return lineDataSet;
    }

    @Override
    protected void renderCharts() {
        respiratoryRateChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    private void setupRespiratoryRateChart() {
        respiratoryRateChart.getDescription().setEnabled(false);
        respiratoryRateChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = respiratoryRateChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        xAxisBottom.setLabelCount(7, true);

        final YAxis yAxisLeft = respiratoryRateChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setAxisMaximum(20);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = respiratoryRateChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }
}
