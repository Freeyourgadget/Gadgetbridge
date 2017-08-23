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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;


public class StatsChartFragment extends AbstractChartFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(StatsChartFragment.class);

    private HorizontalBarChart mStatsChart;

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples = getSamples(db, device);

        MySpeedZonesData mySpeedZonesData = refreshStats(samples);

        return new MyChartsData(mySpeedZonesData);
    }

    private MySpeedZonesData refreshStats(List<? extends ActivitySample> samples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        analysis.calculateActivityAmounts(samples);
        BarData data = new BarData();
        data.setValueTextColor(CHART_TEXT_COLOR);
        List<BarEntry> entries = new ArrayList<>();

        ActivityUser user = new ActivityUser();
        /*double distanceFactorCm;
        if (user.getGender() == user.GENDER_MALE){
            distanceFactorCm = user.getHeightCm() * user.GENDER_MALE_DISTANCE_FACTOR / 1000;
        } else {
            distanceFactorCm = user.getHeightCm() * user.GENDER_FEMALE_DISTANCE_FACTOR / 1000;
        }*/

        for (Map.Entry<Integer, Long> entry : analysis.stats.entrySet()) {
            entries.add(new BarEntry(entry.getKey(), entry.getValue() / 60));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setValueTextColor(CHART_TEXT_COLOR);
        set.setColors(getColorFor(ActivityKind.TYPE_ACTIVITY));
        //set.setDrawValues(false);
        //data.setBarWidth(0.1f);
        data.addDataSet(set);

        return new MySpeedZonesData(data);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
        MyChartsData mcd = (MyChartsData) chartsData;
        mStatsChart.setData(mcd.getChartsData().getBarData());
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

        XAxis x = mStatsChart.getXAxis();
        x.setTextColor(CHART_TEXT_COLOR);

        YAxis yr = mStatsChart.getAxisRight();
        yr.setTextColor(CHART_TEXT_COLOR);
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
        return super.getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void renderCharts() {
        mStatsChart.invalidate();
    }

    private static class MySpeedZonesData extends ChartsData {
        private final BarData barData;

        MySpeedZonesData(BarData barData) {
            this.barData = barData;
        }

        BarData getBarData() {
            return barData;
        }
    }

    private static class MyChartsData extends ChartsData {
        private final MySpeedZonesData chartsData;

        MyChartsData(MySpeedZonesData chartsData) {
            this.chartsData = chartsData;
        }

        MySpeedZonesData getChartsData() {
            return chartsData;
        }
    }
}