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
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.AbstractChartFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevel;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevelDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class BatteryInfoChartFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoChartFragment.class);
    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected String BATTERY_LABEL;

    private LineChart mChart;
    private int startTime;
    private int endTime;
    private GBDevice gbDevice;

    public void setDateAndGetData(GBDevice gbDevice, long startTime, long endTime) {
        this.startTime = (int) startTime;
        this.endTime = (int) endTime;
        this.gbDevice = gbDevice;
        try {
            createRefreshTask("Visualizing data", getActivity()).execute();
        } catch (Exception e) {
            LOG.debug("Unable to fill charts data right now:", e);
        }
    }


    protected RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    private DefaultBatteryChartsData fill_dcd(List<? extends BatteryLevel> samples) {
        AbstractChartFragment.TimestampTranslation tsTranslation = new AbstractChartFragment.TimestampTranslation();
        List<Entry> entries = new ArrayList<Entry>();
        int firstTs = 0;

        for (BatteryLevel sample : samples) {
            entries.add(new Entry(tsTranslation.shorten(sample.getTimestamp()), sample.getLevel()));
            if (firstTs == 0) {
                firstTs = sample.getTimestamp();
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, BATTERY_LABEL);
        dataSet.setLineWidth(2.2f);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(false);
        dataSet.setCircleRadius(2f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(CHART_TEXT_COLOR);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighlightEnabled(true);
        LineData lineData = new LineData(dataSet);

        return new DefaultBatteryChartsData(lineData, new customFormatter(tsTranslation), firstTs);

    }

    private void init() {
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = ContextCompat.getColor(getContext(), R.color.secondarytext);
        BATTERY_LABEL = getString(R.string.battery_level);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init();
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);
        mChart = rootView.findViewById(R.id.activitysleepchart);
        if (this.gbDevice != null) {
            setupChart();
            createRefreshTask("Visualizing data", getActivity()).execute();
        }
        return rootView;
    }

    @Override
    public String getTitle() {
        return "";
    }

    private void setupChart() {
        LEGEND_TEXT_COLOR = GBApplication.getTextColor(getContext());
        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        mChart.setTouchEnabled(true);
        mChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        mChart.getDescription().setEnabled(false);

        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        mChart.getXAxis().setSpaceMin(0.5f);
        x.setLabelCount(3);
        x.setTextColor(CHART_TEXT_COLOR);

        YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setAxisMaximum(100L);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setAxisMaximum(100L);
        yAxisRight.setAxisMinimum(0);
        yAxisRight.setEnabled(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
    }

    private List<? extends BatteryLevel> getBatteryLevels(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        BatteryLevelDao batteryLevelDao = db.getDaoSession().getBatteryLevelDao();
        Device dbDevice = DBHelper.findDevice(device, db.getDaoSession());
        QueryBuilder<BatteryLevel> qb = batteryLevelDao.queryBuilder();

        qb.where(BatteryLevelDao.Properties.DeviceId.eq(dbDevice.getId())).orderAsc(BatteryLevelDao.Properties.Timestamp);
        qb.where(BatteryLevelDao.Properties.Timestamp.gt(tsFrom));
        qb.where(BatteryLevelDao.Properties.Timestamp.lt(tsTo));

        List<BatteryLevel> levels = new ArrayList<>();
        levels.addAll(qb.build().list());
        return levels;
    }

    protected static class customFormatter extends ValueFormatter {
        private final AbstractChartFragment.TimestampTranslation tsTranslation;
        SimpleDateFormat annotationDateFormat = new SimpleDateFormat("dd.MM HH:mm");
        Calendar cal = GregorianCalendar.getInstance();

        public customFormatter(AbstractChartFragment.TimestampTranslation tsTranslation) {
            this.tsTranslation = tsTranslation;
        }

        @Override
        public String getFormattedValue(float value) {
            cal.clear();
            int ts = (int) value;
            cal.setTimeInMillis(tsTranslation.toOriginalValue(ts) * 1000L);
            Date date = cal.getTime();
            return annotationDateFormat.format(date);
        }
    }

    public class RefreshTask extends DBAccess {

        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler handler) {
            List<? extends BatteryLevel> samples = getBatteryLevels(handler, gbDevice, startTime, endTime);
            DefaultBatteryChartsData dcd = null;
            try {
                dcd = fill_dcd(samples);
            } catch (Exception e) {
                LOG.debug("Unable to get charts data right now:", e);
            }
            if (dcd != null && mChart != null) {
                mChart.setTouchEnabled(true);
                mChart.setMarker(new batteryValuesAndDateMarker(getContext(), R.layout.custom_chart_marker, dcd.firstTs));
                mChart.getXAxis().setValueFormatter(dcd.getXValueFormatter());
                mChart.setData((LineData) dcd.getData());
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            mChart.invalidate();
        }
    }

    private class DefaultBatteryChartsData extends AbstractChartFragment.DefaultChartsData {
        public int firstTs;

        public DefaultBatteryChartsData(ChartData data, ValueFormatter xValueFormatter, int ts) {
            super(data, xValueFormatter);
            firstTs = ts;
        }
    }


    public class batteryValuesAndDateMarker extends MarkerView {

        customFormatter formatter;
        private TextView top_text;
        private TextView bottom_text;
        private MPPointF mOffset;
        private int firstTs;

        public batteryValuesAndDateMarker(Context context, int layoutResource, int ts) {
            super(context, layoutResource);
            AbstractChartFragment.TimestampTranslation tsTranslation = new AbstractChartFragment.TimestampTranslation();
            formatter = new customFormatter(tsTranslation);
            top_text = (TextView) findViewById(R.id.chart_marker_item_top);
            bottom_text = (TextView) findViewById(R.id.chart_marker_item_bottom);
            firstTs = ts;
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {

            top_text.setText(String.format("%1s%%", (int) e.getY()));
            bottom_text.setText(formatter.getFormattedValue(e.getX() + firstTs));

            // this will perform necessary layouting
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {

            if (mOffset == null) {
                // center the marker horizontally and vertically
                mOffset = new MPPointF(-(getWidth() / 2) + 20, -getHeight() - 10);
            }
            return mOffset;
        }
    }

}
