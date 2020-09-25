/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, Pavel Elagin

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


public class ActivitySummariesChartFragment extends AbstractChartFragment {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesChartFragment.class);

    private LineChart mChart;
    private int startTime;
    private int endTime;
    private GBDevice gbDevice;

    public void setDateAndGetData(GBDevice gbDevice, long startTime, long endTime) {
        this.startTime = (int) startTime;
        this.endTime = (int) endTime;
        this.gbDevice = gbDevice;
        try {
            populate_charts_data();
        } catch (Exception e) {
            LOG.debug("Unable to fill charts data right now:", e);
        }
    }

    private void populate_charts_data() {
        int LEGEND_TEXT_COLOR = 0;

        try (DBHandler handler = GBApplication.acquireDB()) {
            try {
                LEGEND_TEXT_COLOR = GBApplication.getTextColor(getContext());
            } catch (Exception e) {
                LOG.debug("Unable to get color right now:", e);
            }

            List<? extends ActivitySample> samples = getSamples(handler, gbDevice, startTime, endTime);
            DefaultChartsData dcd=null;
            try {
                dcd = refresh(gbDevice, samples);
            }catch(Exception e){
                LOG.debug("Unable to get charts data right now:", e);
            }
            if (dcd != null) {
                mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
                mChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
                mChart.getXAxis().setValueFormatter(dcd.getXValueFormatter());
                mChart.setData((LineData) dcd.getData());
                mChart.invalidate();
            }
        } catch (Exception e) {
            LOG.error("Unable to get charts data:", e);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init();
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);
        mChart = rootView.findViewById(R.id.activitysleepchart);
        if (this.gbDevice != null) {
            setupChart();
            populate_charts_data();
        }
        return rootView;
    }

    @Override
    public String getTitle() {
        return "";
    }

    private void setupChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mChart);

        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(supportsHeartrate(gbDevice));
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaximum(HeartRateUtils.getInstance().getMaxHeartRate());
        yAxisRight.setAxisMinimum(HeartRateUtils.getInstance().getMinHeartRate());

    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void setupLegend(Chart chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(5);

        LegendEntry activityEntry = new LegendEntry();
        activityEntry.label = akActivity.label;
        activityEntry.formColor = akActivity.color;
        legendEntries.add(activityEntry);

        if (supportsHeartrate(gbDevice)) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    @Override
    protected ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        return null;
    }

    @Override
    protected void renderCharts() {
    }

    protected Entry createLineEntry(float value, int xValue) {
        return new Entry(xValue, value);
    }

    @Override
    protected void updateChartsnUIThread(ChartsData chartsData) {
    }

}
