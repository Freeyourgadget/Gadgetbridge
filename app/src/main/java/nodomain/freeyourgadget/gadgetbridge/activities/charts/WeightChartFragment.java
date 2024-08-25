/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class WeightChartFragment extends AbstractChartFragment<WeightChartFragment.WeightChartsData> {
    private int colorBackground;
    private int colorSecondaryText;

    private int totalDays;
    private boolean imperialUnits;
    private int weightTargetKg;

    private LineChart chart;
    private TextView textTimeSpan;
    private TextView textWeightLatest;
    private TextView textWeightTarget;

    @Override
    public String getTitle() {
        return getString(R.string.menuitem_weight);
    }

    @Override
    protected void init() {
        GBPrefs prefs = GBApplication.getPrefs();

        colorBackground = GBApplication.getBackgroundColor(requireContext());
        colorSecondaryText = GBApplication.getSecondaryTextColor(requireContext());

        if (prefs.getBoolean("charts_range", true))
            totalDays = 30;
        else
            totalDays = 7;

        String unitSystem = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getString(R.string.p_unit_metric));

        if (unitSystem.equals(getString(R.string.p_unit_imperial)))
            imperialUnits = true;
        else
            imperialUnits = false;

        weightTargetKg = prefs.getInt(ActivityUser.PREF_USER_GOAL_WEIGHT_KG, ActivityUser.defaultUserGoalWeightKg);
    }

    @Override
    protected WeightChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        long tsStart = getTSStart() * 1000L;
        long tsEnd = getTSEnd() * 1000L;

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        TimeSampleProvider<? extends WeightSample> provider = coordinator.getWeightSampleProvider(device, db.getDaoSession());
        List<? extends WeightSample> samples = provider.getAllSamples(tsStart, tsEnd);
        WeightSample latestSample = provider.getLatestSample();

        return createChartsData(samples, latestSample);
    }

    @Override
    protected void renderCharts() {
        chart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
    }

    @Override
    protected void setupLegend(Chart<?> chart) {}

    @Override
    protected void updateChartsnUIThread(WeightChartsData chartsData) {
        chart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        chart.getXAxis().setValueFormatter(chartsData.getXValueFormatter());
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.setData(chartsData.getData());

        Date dateStart = DateTimeUtils.parseTimeStamp(getTSStart());
        Date dateEnd = DateTimeUtils.parseTimeStamp(getTSEnd());
        SimpleDateFormat format = new SimpleDateFormat("E, MMM dd");
        WeightSample latestSample = chartsData.getLatestSample();

        textTimeSpan.setText(format.format(dateStart) + " - " + format.format(dateEnd));

        if (latestSample != null)
            textWeightLatest.setText(formatWeight(weightFromKg(latestSample.getWeightKg())));

        textWeightTarget.setText(formatWeight(weightFromKg(weightTargetKg)));
    }

    @Override
    protected int getTSStart() {
        return DateTimeUtils.shiftDays(getTSEnd(), -totalDays + 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weightchart, container, false);

        chart = rootView.findViewById(R.id.weight_chart);
        textTimeSpan = rootView.findViewById(R.id.weight_time_span_text);
        textWeightLatest = rootView.findViewById(R.id.weight_latest_text);
        textWeightTarget = rootView.findViewById(R.id.weight_target_text);

        configureBarLineChartDefaults(chart);
        chart.setBackgroundColor(colorBackground);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);

        LimitLine targetLine = new LimitLine(weightFromKg(weightTargetKg));
        targetLine.setTextColor(colorSecondaryText);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(colorSecondaryText);
        xAxis.setDrawLabels(true);
        xAxis.setDrawLimitLinesBehindData(true);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(colorSecondaryText);
        yAxis.addLimitLine(targetLine);
        yAxis.setDrawGridLines(true);

        refresh();

        return rootView;
    }

    private WeightChartsData createChartsData(List<? extends WeightSample> samples, WeightSample latestSample) {
        List<Entry> entries = new ArrayList<>();
        TimestampTranslation tsTranslation = new TimestampTranslation();

        for (WeightSample sample : samples) {
            int tsSeconds = (int)(sample.getTimestamp() / 1000L);
            float weight = weightFromKg(sample.getWeightKg());

            entries.add(new Entry(tsTranslation.shorten(tsSeconds), weight));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.menuitem_weight));
        dataSet.setLineWidth(2.2f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setCircleRadius(5);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10);
        dataSet.setValueTextColor(colorSecondaryText);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                return formatWeight(entry.getY());
            }
        });

        return new WeightChartsData(new LineData(dataSet), tsTranslation, latestSample);
    }

    private float weightFromKg(float weight) {
        // Convert to lbs
        if (imperialUnits)
            weight *= 2.2046226f;

        return weight;
    }

    private String formatWeight(float weight) {
        int weightString = imperialUnits ? R.string.weight_lbs : R.string.weight_kg;

        return getString(weightString, weight);
    }

    protected static class WeightChartsData extends DefaultChartsData<LineData> {
        private final WeightSample latestSample;

        public WeightChartsData(LineData lineData, TimestampTranslation tsTranslation, WeightSample latestSample) {
            super(lineData, new DateFormatter(tsTranslation));

            this.latestSample = latestSample;
        }

        private WeightSample getLatestSample() {
            return latestSample;
        }
    }

    private static class DateFormatter extends ValueFormatter {
        private TimestampTranslation translation;
        private SimpleDateFormat format = new SimpleDateFormat("dd.MM.");
        private Calendar calendar = GregorianCalendar.getInstance();

        public DateFormatter(TimestampTranslation translation) {
            this.translation = translation;
        }

        @Override
        public String getFormattedValue(float value) {
            calendar.clear();
            calendar.setTimeInMillis(translation.toOriginalValue((int)value) * 1000L);

            return format.format(calendar.getTime());
        }
    }
}
