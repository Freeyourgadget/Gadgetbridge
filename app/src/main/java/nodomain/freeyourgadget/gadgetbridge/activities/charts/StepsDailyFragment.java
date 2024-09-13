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
import androidx.fragment.app.FragmentManager;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class StepsDailyFragment extends StepsFragment<StepsDailyFragment.StepsData> {
    protected static final Logger LOG = LoggerFactory.getLogger(BodyEnergyFragment.class);

    private TextView mDateView;
    private ImageView stepsGauge;
    private TextView steps;
    private TextView distance;
    ImageView stepsStreaksButton;
    private LineChart stepsChart;

    protected int STEPS_GOAL;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_steps, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                getChartsHost().enableSwipeRefresh(scrollY == 0);
            });
        }

        mDateView = rootView.findViewById(R.id.steps_date_view);
        stepsGauge = rootView.findViewById(R.id.steps_gauge);
        steps = rootView.findViewById(R.id.steps_count);
        distance = rootView.findViewById(R.id.steps_distance);
        stepsChart = rootView.findViewById(R.id.steps_daily_chart);
        setupStepsChart();

        STEPS_GOAL = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        refresh();

        stepsStreaksButton = rootView.findViewById(R.id.steps_streaks_button);
        stepsStreaksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                StepStreaksDashboard stepStreaksDashboard = StepStreaksDashboard.newInstance(STEPS_GOAL, getChartsHost().getDevice());
                stepStreaksDashboard.show(fm, "steps_streaks_dashboard");
            }
        });

        return rootView;
    }

        @Override
    public String getTitle() {
        return getString(R.string.steps);
    }

    @Override
    protected StepsDailyFragment.StepsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(chartsHost.getEndDate());
        String formattedDate = new SimpleDateFormat("E, MMM dd").format(chartsHost.getEndDate());
        mDateView.setText(formattedDate);
        List<StepsDay> stepsDayList = getMyStepsDaysData(db, day, device);
        final StepsDay stepsDay;
        if (stepsDayList.isEmpty()) {
            LOG.error("Failed to get StepsDay for {}", day);
            stepsDay = new StepsDay(day, 0, 0);
        } else {
            stepsDay = stepsDayList.get(0);
        }
        List<? extends ActivitySample> samplesOfDay = getSamplesOfDay(db, day, 0, device);
        return new StepsDailyFragment.StepsData(stepsDay, samplesOfDay);
    }

    @Override
    protected void updateChartsnUIThread(StepsDailyFragment.StepsData stepsData) {
        final int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                300,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );

        stepsGauge.setImageBitmap(drawGauge(
                width,
                width / 15,
                getResources().getColor(R.color.steps_color),
                (int) stepsData.todayStepsDay.steps,
                STEPS_GOAL
        ));

        steps.setText(String.format(String.valueOf(stepsData.todayStepsDay.steps)));
        distance.setText(getString(R.string.steps_distance_unit, stepsData.todayStepsDay.distance));

        // Chart
        final List<LegendEntry> legendEntries = new ArrayList<>(1);
        final LegendEntry stepsEntry = new LegendEntry();
        stepsEntry.label = getString(R.string.steps);
        stepsEntry.formColor = getResources().getColor(R.color.steps_color);
        legendEntries.add(stepsEntry);
        stepsChart.getLegend().setTextColor(TEXT_COLOR);
        stepsChart.getLegend().setCustom(legendEntries);

        final List<Entry> lineEntries = new ArrayList<>();
        final TimestampTranslation tsTranslation = new TimestampTranslation();
        int sum = 0;
        for (final ActivitySample sample : stepsData.samples) {
            if (sample.getSteps() > 0) {
                sum += sample.getSteps();
            }
            lineEntries.add(new Entry(tsTranslation.shorten(sample.getTimestamp()), sum));
        }

        stepsChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation, "HH:mm"));

        if (sum < STEPS_GOAL) {
            stepsChart.getAxisLeft().setAxisMaximum(STEPS_GOAL);
        } else {
            stepsChart.getAxisLeft().resetAxisMaximum();
        }

        final LineDataSet lineDataSet = new LineDataSet(lineEntries, getString(R.string.steps));
        lineDataSet.setColor(getResources().getColor(R.color.steps_color));
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setCircleColor(getResources().getColor(R.color.steps_color));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillAlpha(60);
        lineDataSet.setFillColor(getResources().getColor(R.color.steps_color ));

        final LimitLine goalLine = new LimitLine(STEPS_GOAL);
        goalLine.setLineColor(getResources().getColor(R.color.steps_color));
        goalLine.setLineWidth(1.5f);
        goalLine.enableDashedLine(15f, 10f, 0f);
        stepsChart.getAxisLeft().removeAllLimitLines();
        stepsChart.getAxisLeft().addLimitLine(goalLine);
        stepsChart.getAxisLeft().setAxisMaximum(Math.max(lineDataSet.getYMax(), STEPS_GOAL) + 2000);

        final List<ILineDataSet> lineDataSets = new ArrayList<>();
        lineDataSets.add(lineDataSet);
        final LineData lineData = new LineData(lineDataSets);
        stepsChart.setData(lineData);
    }

    @Override
    protected void renderCharts() {
        stepsChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {}

    private void setupStepsChart() {
        stepsChart.getDescription().setEnabled(false);
        stepsChart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = stepsChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(86400f);
        //xAxisBottom.setLabelCount(7, true);

        final YAxis yAxisLeft = stepsChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisRight = stepsChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

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
                90,
                360,
                false,
                paint);
        paint.setStrokeWidth(barWidth);
        paint.setColor(filledColor);
        canvas.drawArc(
                barMargin,
                barMargin,
                width - barMargin,
                height - barMargin,
                270,
                360 * filledFactor,
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

    protected static class StepsData extends ChartsData {
        StepsDay todayStepsDay;
        List<? extends ActivitySample> samples;

        public StepsData(final StepsDay todayStepsDay, final List<? extends ActivitySample> samplesOfDay) {
            this.todayStepsDay = todayStepsDay;
            this.samples = samplesOfDay;
        }
    }
}
