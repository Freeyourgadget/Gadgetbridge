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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractActivityChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DefaultChartsData;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;


public class ActivitySummariesChartFragment extends AbstractActivityChartFragment<ChartsData> {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesChartFragment.class);

    private LineChart mChart;
    private int startTime;
    private int endTime;
    private GBDevice gbDevice;
    private View view;
    private File gpxFile;

    public void setDateAndGetData(GBDevice gbDevice, long startTime, long endTime) {
        this.startTime = (int) startTime;
        this.endTime = (int) endTime;
        this.gbDevice = gbDevice;
        this.gpxFile = null;
        if (this.view != null) {
            createLocalRefreshTask("Visualizing data", getActivity()).execute();
        }
    }

    public void setDateAndGetData(GBDevice gbDevice, File gpxFile) {
        this.gbDevice = gbDevice;
        this.gpxFile = gpxFile;
        this.startTime = 0;
        this.endTime = 0;
        if (this.view != null) {
            createLocalRefreshTask("Visualizing data", getActivity()).execute();
        }
    }

    protected RefreshTask createLocalRefreshTask(String task, Context context) {
        if (gpxFile != null) {
            return new RefreshTask(task, context, (dbHandler) -> {
                final GpxFile gpx;

                try (FileInputStream inputStream = new FileInputStream(gpxFile)) {
                    final GpxParser gpxParser = new GpxParser(inputStream);
                    gpx = gpxParser.getGpxFile();
                } catch (final IOException e) {
                    LOG.error("Failed to open {}", gpxFile, e);
                    // fallback to activity samples
                    return getAllSamples(dbHandler, gbDevice, startTime, endTime);
                } catch (final GpxParseException e) {
                    LOG.error("Failed to parse gpx file", e);
                    // fallback to activity samples
                    return getAllSamples(dbHandler, gbDevice, startTime, endTime);
                }

                final List<GpxActivitySample> ret = new ArrayList<>(gpx.getPoints().size());

                for (final GpxTrackPoint point : gpx.getPoints()) {
                    ret.add(new GpxActivitySample(
                            (int) (point.getTime().getTime() / 1000L),
                            point.getHeartRate()
                    ));
                }

                return ret;
            });
        } else {
            return new RefreshTask(task, context, (dbHandler) -> getAllSamples(dbHandler, gbDevice, startTime, endTime));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);
        mChart = rootView.findViewById(R.id.activitysleepchart);
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        this.view = view;
        if (this.gbDevice != null) {
            setupChart();
            createLocalRefreshTask("Visualizing data", getActivity()).execute();
        }
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
        LOG.warn("This should not need to be called...");
        // FIXME: This fragment should be refactored, this is not even used
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
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
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
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

    public class RefreshTask extends DBAccess {
        private final ActivitySampleGetter getter;

        public RefreshTask(final String task, final Context context, final ActivitySampleGetter getter) {
            super(task, context);
            this.getter = getter;
        }

        @Override
        protected void doInBackground(DBHandler handler) {
            List<? extends ActivitySample> samples = getter.getSamples(handler);

            DefaultChartsData<LineData> dcd = null;
            try {
                dcd = refresh(gbDevice, samples);
            } catch (Exception e) {
                LOG.debug("Unable to get charts data right now:", e);
            }
            if (dcd != null) {
                mChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
                mChart.getXAxis().setValueFormatter(dcd.getXValueFormatter());
                mChart.setData((LineData) dcd.getData());
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            mChart.invalidate();
        }
    }

    public interface ActivitySampleGetter {
        List<? extends ActivitySample> getSamples(DBHandler handler);
    }

    private static class GpxActivitySample implements ActivitySample {

        private final int timestamp;
        private final int hr;

        public GpxActivitySample(final int timestamp, final int hr) {
            this.timestamp = timestamp;
            this.hr = hr;
        }

        @Override
        public SampleProvider getProvider() {
            return null;
        }

        @Override
        public int getRawKind() {
            return ActivityKind.TYPE_ACTIVITY;
        }

        @Override
        public int getKind() {
            return ActivityKind.TYPE_ACTIVITY;
        }

        @Override
        public int getRawIntensity() {
            return 0;
        }

        @Override
        public float getIntensity() {
            return 0;
        }

        @Override
        public int getSteps() {
            return 0;
        }

        @Override
        public int getHeartRate() {
            return hr;
        }

        @Override
        public void setHeartRate(final int value) {

        }

        @Override
        public int getTimestamp() {
            return timestamp;
        }
    }
}
