/*  Copyright (C) 2015-2017 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Vebryn

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;


public class StatsChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private HorizontalBarChart mStatsChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples = getSamples(db, device);

        MySleepChartsData mySleepChartsData = refreshSleepAmounts(device, samples);
        DefaultChartsData chartsData = refresh(device, samples);

        return new MyChartsData(mySleepChartsData, chartsData);
    }

    private MySleepChartsData refreshSleepAmounts(GBDevice mGBDevice, List<? extends ActivitySample> samples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        analysis.calculateActivityAmounts(samples);
        BarData data = new BarData();
        List<BarEntry> entries = new ArrayList<>();
        XAxisValueFormatter customXAxis = new XAxisValueFormatter();

        for (Map.Entry<Float, Float> entry : analysis.statsQuantified.entrySet()) {
            entries.add(new BarEntry(entry.getKey(), entry.getValue()));
            /*float realValue = entry.getKey() * analysis.maxSpeedQuantifier;
            String customLabel = Math.round(realValue * (1 - analysis.roundPrecision) * 10f) / 10f + " - " + Math.round(realValue * (1 + analysis.roundPrecision) * 10f) / 10f;*/
            customXAxis.add("" + entry.getKey() * analysis.maxSpeedQuantifier);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(getColorFor(ActivityKind.TYPE_ACTIVITY));
        //set.setDrawValues(false);
        //data.setBarWidth(0.1f);
        data.addDataSet(set);

        // set X axis
        customXAxis.sort();
        XAxis left = mStatsChart.getXAxis();
        left.setValueFormatter(customXAxis);

        // display precision
        //mStatsChart.getDescription().setText(Math.round(analysis.roundPrecision * 100) + "%");

        return new MySleepChartsData("", data);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;
        mStatsChart.setData(mcd.getPieData().getPieData());
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_statschart, container, false);

        mStatsChart = (HorizontalBarChart) rootView.findViewById(R.id.statschart);
        setupStatsChart();

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();

        return rootView;
    }

    private void setupStatsChart() {
        mStatsChart.setBackgroundColor(BACKGROUND_COLOR);
        mStatsChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mStatsChart.setNoDataText("");
        mStatsChart.getLegend().setEnabled(false);
        mStatsChart.setTouchEnabled(false);
        mStatsChart.getDescription().setText("");
    }

    @Override
    protected void setupLegend(Chart chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(3);
        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsHeartrate(getChartsHost().getDevice())) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
// temporary fix for totally wrong sleep amounts
//        return super.getSleepSamples(db, device, tsFrom, tsTo);
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void renderCharts() {
        mStatsChart.invalidate();
    }

    private static class MySleepChartsData extends ChartsData {
        private String totalSleep;
        private final BarData pieData;

        public MySleepChartsData(String totalSleep, BarData pieData) {
            this.totalSleep = totalSleep;
            this.pieData = pieData;
        }

        public BarData getPieData() {
            return pieData;
        }

        public CharSequence getTotalSleep() {
            return totalSleep;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final DefaultChartsData<CombinedData> chartsData;
        private final MySleepChartsData pieData;

        public MyChartsData(MySleepChartsData pieData, DefaultChartsData<CombinedData> chartsData) {
            this.pieData = pieData;
            this.chartsData = chartsData;
        }

        public MySleepChartsData getPieData() {
            return pieData;
        }

        public DefaultChartsData<CombinedData> getChartsData() {
            return chartsData;
        }
    }
}