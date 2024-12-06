/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk

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
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractActivityChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsHost;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DefaultChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SampleXLabelFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.TimestampTranslation;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;


public class ActivitySummariesChartFragment extends AbstractActivityChartFragment<ChartsData> {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesChartFragment.class);

    private LineChart mChart;
    private View view;

    // If a track file is being used (takes precedence over activity data)
    private File trackFile;

    // If activity data is being used
    private GBDevice gbDevice;
    private int startTime;
    private int endTime;

    public void setDateAndGetData(@Nullable File trackFile, GBDevice gbDevice, long startTime, long endTime) {
        this.trackFile = trackFile;
        this.startTime = (int) startTime;
        this.endTime = (int) endTime;
        this.gbDevice = gbDevice;
        if (this.view != null) {
            createLocalRefreshTask("getting hr and activity", getActivity()).execute();
        }
    }

    protected RefreshTask createLocalRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
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
        if (this.trackFile != null || this.gbDevice != null) {
            setupChart();
            createLocalRefreshTask("getting hr and activity", getActivity()).execute();
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
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected List<? extends ActivitySample> getSamplesHighRes(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return getAllSamplesHighRes(db, device, tsFrom, tsTo);
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

        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler handler) {
            final DefaultChartsData<?> dcd;
            final DefaultChartsData<LineData> activitySamplesData = buildChartFromSamples(handler);

            if (trackFile != null) {
                final List<ActivityPoint> activityPoints = ActivitySummariesGpsFragment.getActivityPoints(trackFile)
                        .stream()
                        .filter(ap -> ap.getHeartRate() > 0)
                        .collect(Collectors.toList());

                if (!activityPoints.isEmpty()) {
                    dcd = buildHeartRateChart(activityPoints, activitySamplesData);
                } else {
                    dcd = activitySamplesData;
                }
            } else {
                dcd = activitySamplesData;
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

        private DefaultChartsData<LineData> buildChartFromSamples(DBHandler handler) {
            final List<? extends ActivitySample> samples = getAllSamples(handler, gbDevice, startTime, endTime);
            final List<? extends ActivitySample> highResSamples = getAllSamplesHighRes(handler, gbDevice, startTime, endTime);

            try {
                if (highResSamples == null)
                    return refresh(gbDevice, samples);
                return refresh(gbDevice, samples, highResSamples);
            } catch (Exception e) {
                LOG.error("Unable to get charts data right now", e);
            }

            return null;
        }

        private DefaultChartsData<LineData> buildHeartRateChart(final List<ActivityPoint> activityPoints,
                                                                final DefaultChartsData<LineData> activitySamplesData) {
            // If we have data from activity samples, we need to use the same TimestampTranslation so
            // that the HR chart is aligned
            // This is not ideal...
            final TimestampTranslation tsTranslation;
            if (activitySamplesData != null) {
                final ValueFormatter xValueFormatter = activitySamplesData.getXValueFormatter();
                if (xValueFormatter instanceof SampleXLabelFormatter) {
                    tsTranslation = ((SampleXLabelFormatter) xValueFormatter).getTsTranslation();
                } else {
                    LOG.error("Unable to get TimestampTranslation from x value formatter - class changed?");
                    tsTranslation = new TimestampTranslation();
                }
            } else {
                tsTranslation = new TimestampTranslation();
            }

            final List<Entry> heartRateEntries = new ArrayList<>(activityPoints.size());
            final List<ILineDataSet> heartRateDataSets = new ArrayList<>();
            int lastTsShorten = 0;
            for (final ActivityPoint activityPoint : activityPoints) {
                int tsShorten = tsTranslation.shorten((int) (activityPoint.getTime().getTime() / 1000));
                if (lastTsShorten == 0 || (tsShorten - lastTsShorten) <= 60 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                    heartRateEntries.add(new Entry(tsShorten, activityPoint.getHeartRate()));
                } else {
                    if (!heartRateEntries.isEmpty()) {
                        List<Entry> clone = new ArrayList<>(heartRateEntries.size());
                        clone.addAll(heartRateEntries);
                        heartRateDataSets.add(createHeartrateSet(clone, "Heart Rate"));
                        heartRateEntries.clear();
                    }
                }
                lastTsShorten = tsShorten;
                heartRateEntries.add(new Entry(tsShorten, activityPoint.getHeartRate()));
            }
            if (!heartRateEntries.isEmpty()) {
                heartRateDataSets.add(createHeartrateSet(heartRateEntries, "Heart Rate"));
            }

            if (activitySamplesData != null) {
                // if we have activity samples, replace the heart rate dataset
                LineData data = activitySamplesData.getData();
                List<ILineDataSet> dataSets = data.getDataSets();
                for (final ILineDataSet dataSet : dataSets) {
                    if ("Heart Rate".equals(dataSet.getLabel())) {
                        dataSets.remove(dataSet);
                        dataSets.addAll(heartRateDataSets);
                        return activitySamplesData;
                    }
                }
                // We failed to find a heart rate dataset. We can't append ours, or it will crash
                //dataSets.add(heartRateSet);
                return activitySamplesData;
            } else {
                final LineData lineData = new LineData(heartRateDataSets);
                final ValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation, "HH:mm");
                return new DefaultChartsData<>(lineData, xValueFormatter);
            }
        }
    }
}
