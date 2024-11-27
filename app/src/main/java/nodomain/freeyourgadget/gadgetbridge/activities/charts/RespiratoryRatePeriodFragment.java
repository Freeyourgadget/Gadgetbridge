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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class RespiratoryRatePeriodFragment extends RespiratoryRateFragment<RespiratoryRatePeriodFragment.RespiratoryRateData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;
    private TextView sleepAvg;
    private TextView awakeAvg;
    private LineChart respiratoryRateChart;

    public static RespiratoryRatePeriodFragment newInstance (int totalDays) {
        RespiratoryRatePeriodFragment fragmentFirst = new RespiratoryRatePeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_respiratory_rate_period, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.rr_date_view);
        sleepAvg = rootView.findViewById(R.id.sleep_avg);
        awakeAvg = rootView.findViewById(R.id.awake_avg);
        respiratoryRateChart = rootView.findViewById(R.id.respiratory_rate_line_chart);
        setupRespiratoryRateChart();
        refresh();

        return rootView;
    }

    protected void setupRespiratoryRateChart() {
        respiratoryRateChart.getDescription().setEnabled(false);
        if (TOTAL_DAYS <= 7) {
            respiratoryRateChart.setTouchEnabled(false);
            respiratoryRateChart.setPinchZoom(false);
        }

        respiratoryRateChart.getDescription().setEnabled(false);
        respiratoryRateChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = respiratoryRateChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setLabelCount(7, true);
        xAxisBottom.setAxisMinimum(0);
        xAxisBottom.setAxisMaximum(TOTAL_DAYS - 1);

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

        @Override
    public String getTitle() {
        return getString(R.string.respiratoryrate);
    }

    @Override
    protected RespiratoryRateData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        Date to = new Date((long) this.getTSEnd() * 1000);
        Date from = DateUtils.addDays(to,-(TOTAL_DAYS - 1));
        String toFormattedDate = new SimpleDateFormat("E, MMM dd").format(to);
        String fromFormattedDate = new SimpleDateFormat("E, MMM dd").format(from);
        mDateView.setText(fromFormattedDate + " - " + toFormattedDate);
        day.setTime(to);
        List<RespiratoryRateDay> respiratoryRateDaysData = getMyRespiratoryRateDaysData(db, day, device);
        return new RespiratoryRateData(respiratoryRateDaysData);
    }

    protected LineDataSet createDataSet(final List<Entry> values, String label, int color) {
        final LineDataSet lineDataSet = new LineDataSet(values, label);
        lineDataSet.setColor(getResources().getColor(color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setCircleColor(getResources().getColor(color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        return lineDataSet;
    }

    @Override
    protected void updateChartsnUIThread(RespiratoryRateData respiratoryRateData) {
        respiratoryRateChart.setData(null);
        sleepAvg.setText(String.valueOf(respiratoryRateData.sleepRateAvg));
        awakeAvg.setText(String.valueOf(respiratoryRateData.awakeRateAvg));

        List<Entry> lineAwakeRateAvgEntries = new ArrayList<>();
        List<Entry> lineSleepRateEntries = new ArrayList<>();
        for (int i = 0; i < TOTAL_DAYS; i++) {
            RespiratoryRateDay day = respiratoryRateData.days.get(i);
            if (day.awakeRateAvg > 0) {
                lineAwakeRateAvgEntries.add(new Entry(i, day.awakeRateAvg));
            }
            if (day.sleepRateAvg > 0) {
                lineSleepRateEntries.add(new Entry(i, day.sleepRateAvg));
            }
        }

        LineDataSet awakeDataSet = createDataSet(lineAwakeRateAvgEntries, getString(R.string.sleep_colored_stats_awake_avg), R.color.respiratory_rate_color);
        LineDataSet sleepDataSet = createDataSet(lineSleepRateEntries, getString(R.string.sleep_avg), R.color.chart_light_sleep_light);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        lineDataSets.add(awakeDataSet);
        lineDataSets.add(sleepDataSet);

        List<LegendEntry> legendEntries = new ArrayList<>(1);
        LegendEntry awakeEntry = new LegendEntry();
        awakeEntry.label = getString(R.string.sleep_colored_stats_awake_avg);
        awakeEntry.formColor = getResources().getColor(R.color.respiratory_rate_color);
        LegendEntry sleepEntry = new LegendEntry();
        sleepEntry.label = getString(R.string.sleep_avg);
        sleepEntry.formColor = getResources().getColor(R.color.chart_light_sleep_light);
        legendEntries.add(awakeEntry);
        legendEntries.add(sleepEntry);
        respiratoryRateChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        respiratoryRateChart.getLegend().setCustom(legendEntries);
        final LineData lineData = new LineData(lineDataSets);
        respiratoryRateChart.setData(lineData);
        final XAxis x = respiratoryRateChart.getXAxis();
        x.setValueFormatter(getRespiratoryRateChartDayValueFormatter(respiratoryRateData));
    }

    ValueFormatter getRespiratoryRateChartDayValueFormatter(RespiratoryRateData RespiratoryRateData) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                RespiratoryRateFragment.RespiratoryRateDay day = RespiratoryRateData.days.get((int) value);
                String pattern = TOTAL_DAYS > 7 ? "dd" : "EEE";
                SimpleDateFormat formatLetterDay = new SimpleDateFormat(pattern, Locale.getDefault());
                return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
            }
        };
    }

    @Override
    protected void renderCharts() {
        respiratoryRateChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    protected static class RespiratoryRateData extends ChartsData {
        List<RespiratoryRateDay> days;
        int awakeRateAvg;
        int sleepRateAvg;

        protected RespiratoryRateData(List<RespiratoryRateDay> days) {
            this.days = days;
            int awakeTotal = 0;
            int sleepTotal = 0;
            int awakeCounter = 0;
            int sleepCounter = 0;
            for(RespiratoryRateDay day: days) {
                if (day.awakeRateAvg > 0) {
                    awakeTotal += day.awakeRateAvg;
                    awakeCounter++;
                }
                if (day.sleepRateAvg > 0) {
                    sleepTotal += day.sleepRateAvg;
                    sleepCounter++;
                }
            }
            if (awakeTotal > 0) {
                this.awakeRateAvg = Math.round((float) awakeTotal / awakeCounter);
            }
            if (sleepTotal > 0) {
                this.sleepRateAvg = Math.round((float) sleepTotal / sleepCounter);
            }
        }
    }
}
