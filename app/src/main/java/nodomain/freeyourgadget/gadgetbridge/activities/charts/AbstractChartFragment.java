/*  Copyright (C) 2015-2018 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, walkjivefly

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.View;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

import static nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepUtils.isSleep;

/**
 * A base class fragment to be used with ChartsActivity. The fragment can supply
 * a title to be displayed in the activity by returning non-null in #getTitle()
 * Broadcast events can be received by overriding #onReceive(Context,Intent).
 * The chart can be refreshed by calling #refresh()
 * Implement refreshInBackground(DBHandler, GBDevice) to fetch the samples from the DB,
 * and add the samples to the chart. The actual rendering, which must be performed in the UI
 * thread, must be done in #renderCharts().
 * Access functionality of the hosting activity with #getHost()
 * <p/>
 * The hosting ChartsHost activity provides a section for displaying a date or date range
 * being the basis for the chart, as well as two buttons for moving backwards and forward
 * in time. The date is held by the activity, so that it can be shared by multiple chart
 * fragments. It is still the responsibility of the (currently visible) chart fragment
 * to set the desired date in the ChartsActivity via #setDateRange(Date,Date).
 * The default implementations #handleDatePrev(Date,Date) and #handleDateNext(Date,Date)
 * shift the date by one day.
 */
public abstract class AbstractChartFragment extends AbstractGBFragment {
    protected final int ANIM_TIME = 250;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChartFragment.class);


    private final Set<String> mIntentFilterActions;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractChartFragment.this.onReceive(context, intent);
        }
    };
    private boolean mChartDirty = true;
    private AsyncTask refreshTask;

    public boolean isChartDirty() {
        return mChartDirty;
    }

    @Override
    public abstract String getTitle();

    public boolean supportsHeartrate(GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator != null && coordinator.supportsHeartRateMeasurement(device);
    }

    protected static final class ActivityConfig {
        public final int type;
        public final String label;
        public final Integer color;

        public ActivityConfig(int kind, String label, Integer color) {
            this.type = kind;
            this.label = label;
            this.color = color;
        }
    }

    protected ActivityConfig akActivity;
    protected ActivityConfig akLightSleep;
    protected ActivityConfig akDeepSleep;
    protected ActivityConfig akNotWorn;


    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int HEARTRATE_COLOR;
    protected int HEARTRATE_FILL_COLOR;
    protected int AK_ACTIVITY_COLOR;
    protected int AK_DEEP_SLEEP_COLOR;
    protected int AK_LIGHT_SLEEP_COLOR;
    protected int AK_NOT_WORN_COLOR;

    protected String HEARTRATE_LABEL;

    protected AbstractChartFragment(String... intentFilterActions) {
        mIntentFilterActions = new HashSet<>();
        if (intentFilterActions != null) {
            mIntentFilterActions.addAll(Arrays.asList(intentFilterActions));
        }
        mIntentFilterActions.add(ChartsHost.DATE_NEXT);
        mIntentFilterActions.add(ChartsHost.DATE_PREV);
        mIntentFilterActions.add(ChartsHost.REFRESH);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        IntentFilter filter = new IntentFilter();
        for (String action : mIntentFilterActions) {
            filter.addAction(action);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    protected void init() {
        TypedValue runningColor = new TypedValue();
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = ContextCompat.getColor(getContext(), R.color.secondarytext);
        HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate);
        HEARTRATE_FILL_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_fill);
        getContext().getTheme().resolveAttribute(R.attr.chart_activity, runningColor, true);
        AK_ACTIVITY_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_deep_sleep, runningColor, true);
        AK_DEEP_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_light_sleep, runningColor, true);
        AK_LIGHT_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_not_worn, runningColor, true);
        AK_NOT_WORN_COLOR = runningColor.data;

        HEARTRATE_LABEL = getContext().getString(R.string.charts_legend_heartrate);

        akActivity = new ActivityConfig(ActivityKind.TYPE_ACTIVITY, getString(R.string.abstract_chart_fragment_kind_activity), AK_ACTIVITY_COLOR);
        akLightSleep = new ActivityConfig(ActivityKind.TYPE_LIGHT_SLEEP, getString(R.string.abstract_chart_fragment_kind_light_sleep), AK_LIGHT_SLEEP_COLOR);
        akDeepSleep = new ActivityConfig(ActivityKind.TYPE_DEEP_SLEEP, getString(R.string.abstract_chart_fragment_kind_deep_sleep), AK_DEEP_SLEEP_COLOR);
        akNotWorn = new ActivityConfig(ActivityKind.TYPE_NOT_WORN, getString(R.string.abstract_chart_fragment_kind_not_worn), AK_NOT_WORN_COLOR);
    }

    private void setStartDate(Date date) {
        getChartsHost().setStartDate(date);
    }

    @Nullable
    protected ChartsHost getChartsHost() {
        return (ChartsHost) getActivity();
    }

    private void setEndDate(Date date) {
        getChartsHost().setEndDate(date);
    }

    public Date getStartDate() {
        return getChartsHost().getStartDate();
    }

    public Date getEndDate() {
        return getChartsHost().getEndDate();
    }

    /**
     * Called when this fragment has been fully scrolled into the activity.
     *
     * @see #isVisibleInActivity()
     * @see #onMadeInvisibleInActivity()
     */
    @Override
    protected void onMadeVisibleInActivity() {
        super.onMadeVisibleInActivity();
        showDateBar(true);
        if (isChartDirty()) {
            refresh();
        }
    }

    protected void showDateBar(boolean show) {
        getChartsHost().getDateBar().setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    protected void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ChartsHost.REFRESH.equals(action)) {
            refresh();
        } else if (ChartsHost.DATE_NEXT.equals(action)) {
            handleDateNext(getStartDate(), getEndDate());
        } else if (ChartsHost.DATE_PREV.equals(action)) {
            handleDatePrev(getStartDate(), getEndDate());
        }
    }

    /**
     * Default implementation shifts the dates by one day, if visible
     * and calls #refreshIfVisible().
     *
     * @param startDate
     * @param endDate
     */
    protected void handleDatePrev(Date startDate, Date endDate) {
        if (isVisibleInActivity()) {
            if (!shiftDates(startDate, endDate, -1)) {
                return;
            }
        }
        refreshIfVisible();
    }

    /**
     * Default implementation shifts the dates by one day, if visible
     * and calls #refreshIfVisible().
     *
     * @param startDate
     * @param endDate
     */
    protected void handleDateNext(Date startDate, Date endDate) {
        if (isVisibleInActivity()) {
            if (!shiftDates(startDate, endDate, +1)) {
                return;
            }
        }
        refreshIfVisible();
    }

    protected void refreshIfVisible() {
        if (isVisibleInActivity()) {
            refresh();
        } else {
            mChartDirty = true;
        }
    }

    /**
     * Shifts the given dates by offset days. offset may be positive or negative.
     *
     * @param startDate
     * @param endDate
     * @param offset    a positive or negative number of days to shift the dates
     * @return true if the shift was successful and false otherwise
     */
    protected boolean shiftDates(Date startDate, Date endDate, int offset) {
        Date newStart = DateTimeUtils.shiftByDays(startDate, offset);
        Date newEnd = DateTimeUtils.shiftByDays(endDate, offset);

        return setDateRange(newStart, newEnd);
    }

    protected Integer getColorFor(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_DEEP_SLEEP:
                return akDeepSleep.color;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return akLightSleep.color;
            case ActivityKind.TYPE_ACTIVITY:
                return akActivity.color;
        }
        return akActivity.color;
    }

    protected SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }

    /**
     * Returns all kinds of samples for the given device.
     * To be called from a background thread.
     *
     * @param device
     * @param tsFrom
     * @param tsTo
     */
    protected List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getAllActivitySamples(tsFrom, tsTo);
    }

    protected List<? extends AbstractActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends AbstractActivitySample> provider = getProvider(db, device);
        return provider.getActivitySamples(tsFrom, tsTo);
    }


    protected List<? extends ActivitySample> getSleepSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getSleepSamples(tsFrom, tsTo);
    }

    protected void configureChartDefaults(Chart<?> chart) {
        chart.getXAxis().setValueFormatter(new TimestampValueFormatter());
        chart.getDescription().setText("");

        // if enabled, the chart will always start at zero on the y-axis
        chart.setNoDataText(getString(R.string.chart_no_data_synchronize));

        // disable value highlighting
        chart.setHighlightPerTapEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);

// commented out: this has weird bugs/sideeffects at least on WeekStepsCharts
// where only the first Day-label is drawn, because AxisRenderer.computeAxisValues(float,float)
// appears to have an overflow when calculating 'n' (number of entries)
//        chart.getXAxis().setGranularity(60*5);

        setupLegend(chart);
    }

    protected void configureBarLineChartDefaults(BarLineChartBase<?> chart) {
        configureChartDefaults(chart);
        if (chart instanceof BarChart) {
            ((BarChart) chart).setFitBars(true);
        }

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
//        chart.setPinchZoom(true);

        chart.setDrawGridBackground(false);
    }

    /**
     * This method will invoke a background task to read the data from the
     * database, analyze it, prepare it for the charts and eventually call
     * #renderCharts
     */
    protected void refresh() {
        ChartsHost chartsHost = getChartsHost();
        if (chartsHost != null) {
            if (chartsHost.getDevice() != null) {
                mChartDirty = false;
                updateDateInfo(getStartDate(), getEndDate());
                if (refreshTask != null && refreshTask.getStatus() != AsyncTask.Status.FINISHED) {
                    refreshTask.cancel(true);
                }
                refreshTask = createRefreshTask("Visualizing data", getActivity()).execute();
            }
        }
    }

    /**
     * This method reads the data from the database, analyzes and prepares it for
     * the charts. This will be called from a background task, so there must not be
     * any UI access. #updateChartsInUIThread and #renderCharts will be automatically called after this method.
     */
    protected abstract ChartsData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device);

    /**
     * Triggers the actual (re-) rendering of the chart.
     * Always called from the UI thread.
     */
    protected abstract void renderCharts();

    protected DefaultChartsData<LineData> refresh(GBDevice gbDevice, List<? extends ActivitySample> samples) {
//        Calendar cal = GregorianCalendar.getInstance();
//        cal.clear();
        TimestampTranslation tsTranslation = new TimestampTranslation();
//        Date date;
//        String dateStringFrom = "";
//        String dateStringTo = "";
//        ArrayList<String> xLabels = null;

        LOG.info("" + getTitle() + ": number of samples:" + samples.size());
        LineData lineData;
        if (samples.size() > 1) {
            boolean annotate = true;
            boolean use_steps_as_movement;

            int last_type = ActivityKind.TYPE_UNKNOWN;

            int numEntries = samples.size();
            List<Entry> activityEntries = new ArrayList<>(numEntries);
            List<Entry> deepSleepEntries = new ArrayList<>(numEntries);
            List<Entry> lightSleepEntries = new ArrayList<>(numEntries);
            List<Entry> notWornEntries = new ArrayList<>(numEntries);
            boolean hr = supportsHeartrate(gbDevice);
            List<Entry> heartrateEntries = hr ? new ArrayList<Entry>(numEntries) : null;
            List<Integer> colors = new ArrayList<>(numEntries); // this is kinda inefficient...
            int lastHrSampleIndex = -1;
            HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

            for (int i = 0; i < numEntries; i++) {
                ActivitySample sample = samples.get(i);
                int type = sample.getKind();
                int ts = tsTranslation.shorten(sample.getTimestamp());

//                System.out.println(ts);
//                ts = i;
                // determine start and end dates
//                if (i == 0) {
//                    cal.setTimeInMillis(ts * 1000L); // make sure it's converted to long
//                    date = cal.getTime();
//                    dateStringFrom = dateFormat.format(date);
//                } else if (i == samples.size() - 1) {
//                    cal.setTimeInMillis(ts * 1000L); // same here
//                    date = cal.getTime();
//                    dateStringTo = dateFormat.format(date);
//                }

                float movement = sample.getIntensity();

                float value = movement;
                switch (type) {
                    case ActivityKind.TYPE_DEEP_SLEEP:
                        if (last_type != type) { //FIXME: this is ugly but it works (repeated in each case)
                            deepSleepEntries.add(createLineEntry(0, ts - 1));

                            lightSleepEntries.add(createLineEntry(0, ts));
                            notWornEntries.add(createLineEntry(0, ts));
                            activityEntries.add(createLineEntry(0, ts));
                        }
                        deepSleepEntries.add(createLineEntry(value + SleepUtils.Y_VALUE_DEEP_SLEEP, ts));
                        break;
                    case ActivityKind.TYPE_LIGHT_SLEEP:
                        if (last_type != type) {
                            lightSleepEntries.add(createLineEntry(0, ts - 1));

                            deepSleepEntries.add(createLineEntry(0, ts));
                            notWornEntries.add(createLineEntry(0, ts));
                            activityEntries.add(createLineEntry(0, ts));
                        }
                        lightSleepEntries.add(createLineEntry(value, ts));
                        break;
                    case ActivityKind.TYPE_NOT_WORN:
                        if (last_type != type) {
                            notWornEntries.add(createLineEntry(0, ts - 1));

                            lightSleepEntries.add(createLineEntry(0, ts));
                            deepSleepEntries.add(createLineEntry(0, ts));
                            activityEntries.add(createLineEntry(0, ts));
                        }
                        notWornEntries.add(createLineEntry(SleepUtils.Y_VALUE_DEEP_SLEEP, ts)); //a small value, just to show something on the graphs
                        break;
                    default:
//                        short steps = sample.getSteps();
//                        if (use_steps_as_movement && steps != 0) {
//                            // I'm not sure using steps for this is actually a good idea
//                            movement = steps;
//                        }
//                        value = ((float) movement) / movement_divisor;
                        if (last_type != type) {
                            activityEntries.add(createLineEntry(0, ts - 1));

                            lightSleepEntries.add(createLineEntry(0, ts));
                            notWornEntries.add(createLineEntry(0, ts));
                            deepSleepEntries.add(createLineEntry(0, ts));
                        }
                        activityEntries.add(createLineEntry(value, ts));
                }
                if (hr && sample.getKind() != ActivityKind.TYPE_NOT_WORN && heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                    if (lastHrSampleIndex > -1 && ts - lastHrSampleIndex > 1800*HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                        heartrateEntries.add(createLineEntry(0, lastHrSampleIndex + 1));
                        heartrateEntries.add(createLineEntry(0, ts - 1));
                    }

                    heartrateEntries.add(createLineEntry(sample.getHeartRate(), ts));
                    lastHrSampleIndex = ts;
                }

                String xLabel = "";
                if (annotate) {
                    if (last_type != type) {
                        String filter = "test";
                        Intent intent = new Intent(filter);
                        if (isSleep(last_type) && !isSleep(type)) {
                            // woken up
                            intent.putExtra("woken_up", ts);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        } else if (!isSleep(last_type) && isSleep(type)) {
                            // fallen asleep
                            intent.putExtra("fallen_asleep", ts);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        }
                    }
                }
                last_type = type;
            }


            List<ILineDataSet> lineDataSets = new ArrayList<>();
            LineDataSet activitySet = createDataSet(activityEntries, akActivity.color, "Activity");
            lineDataSets.add(activitySet);
            LineDataSet deepSleepSet = createDataSet(deepSleepEntries, akDeepSleep.color, "Deep Sleep");
            lineDataSets.add(deepSleepSet);
            LineDataSet lightSleepSet = createDataSet(lightSleepEntries, akLightSleep.color, "Light Sleep");
            lineDataSets.add(lightSleepSet);
            LineDataSet notWornSet = createDataSet(notWornEntries, akNotWorn.color, "Not worn");
            lineDataSets.add(notWornSet);

            if (hr && heartrateEntries.size() > 0) {
                LineDataSet heartrateSet = createHeartrateSet(heartrateEntries, "Heart Rate");

                lineDataSets.add(heartrateSet);
            }
            lineData = new LineData(lineDataSets);

//            chart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            chart.setDescriptionPosition(?, ?);
        } else {
            lineData = new LineData();
        }

        IAxisValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation);
        return new DefaultChartsData(lineData, xValueFormatter);
    }

    /**
     * Implement this to supply the samples to be displayed.
     *
     * @param db
     * @param device
     * @param tsFrom
     * @param tsTo
     * @return
     */
    protected abstract List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo);

    protected abstract void setupLegend(Chart chart);

    protected Entry createLineEntry(float value, int xValue) {
        return new Entry(xValue, value);
    }

    protected LineDataSet createDataSet(List<Entry> values, Integer color, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setColor(color);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
        set1.setDrawFilled(true);
        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
        set1.setFillColor(color);
        set1.setFillAlpha(255);
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set1;
    }

    protected LineDataSet createHeartrateSet(List<Entry> values, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setLineWidth(2.2f);
        set1.setColor(HEARTRATE_COLOR);
//        set1.setDrawCubic(true);
        set1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        set1.setCubicIntensity(0.1f);
        set1.setDrawCircles(false);
//        set1.setCircleRadius(2f);
//        set1.setDrawFilled(true);
//        set1.setColor(getResources().getColor(android.R.color.background_light));
//        set1.setCircleColor(HEARTRATE_COLOR);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setDrawValues(true);
        set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return set1;
    }

    protected RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    public class RefreshTask extends DBAccess {
        private ChartsData chartsData;

        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler db) {
            ChartsHost chartsHost = getChartsHost();
            if (chartsHost != null) {
                chartsData = refreshInBackground(chartsHost, db, chartsHost.getDevice());
            } else {
                cancel(true);
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            FragmentActivity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                updateChartsnUIThread(chartsData);
                renderCharts();
            } else {
                LOG.info("Not rendering charts because activity is not available anymore");
            }
        }
    }

    protected abstract void updateChartsnUIThread(ChartsData chartsData);

    /**
     * Returns true if the date was successfully shifted, and false if the shift
     * was ignored, e.g. when the to-value is in the future.
     *
     * @param from
     * @param to
     */
    public boolean setDateRange(Date from, Date to) {
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException("Bad date range: " + from + ".." + to);
        }
        Date now = new Date();
        if (to.after(now)) {
            return false;
        }
        setStartDate(from);
        setEndDate(to);
        return true;
    }

    protected void updateDateInfo(Date from, Date to) {
        if (from.equals(to)) {
            getChartsHost().setDateInfo(DateTimeUtils.formatDate(from));
        } else {
            getChartsHost().setDateInfo(DateTimeUtils.formatDateRange(from, to));
        }
    }

    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device) {
        int tsStart = getTSStart();
        int tsEnd = getTSEnd();
        List<ActivitySample> samples = (List<ActivitySample>) getSamples(db, device, tsStart, tsEnd);
        ensureStartAndEndSamples(samples, tsStart, tsEnd);
//        List<ActivitySample> samples2 = new ArrayList<>();
//        int min = Math.min(samples.size(), 10);
//        int min = Math.min(samples.size(), 10);
//        for (int i = 0; i < min; i++) {
//            samples2.add(samples.get(i));
//        }
//        return samples2;
        return samples;
    }

    protected void ensureStartAndEndSamples(List<ActivitySample> samples, int tsStart, int tsEnd) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        ActivitySample lastSample = samples.get(samples.size() - 1);
        if (lastSample.getTimestamp() < tsEnd) {
            samples.add(createTrailingActivitySample(lastSample, tsEnd));
        }

        ActivitySample firstSample = samples.get(0);
        if (firstSample.getTimestamp() > tsStart) {
            samples.add(createTrailingActivitySample(firstSample, tsStart));
        }
    }

    private ActivitySample createTrailingActivitySample(ActivitySample referenceSample, int timestamp) {
        TrailingActivitySample sample = new TrailingActivitySample();
        if (referenceSample instanceof AbstractActivitySample) {
            AbstractActivitySample reference = (AbstractActivitySample) referenceSample;
            sample.setUserId(reference.getUserId());
            sample.setDeviceId(reference.getDeviceId());
            sample.setProvider(reference.getProvider());
        }
        sample.setTimestamp(timestamp);
        return sample;
    }

    private int getTSEnd() {
        return toTimestamp(getEndDate());
    }

    private int getTSStart() {
        return toTimestamp(getStartDate());
    }

    private int toTimestamp(Date date) {
        return (int) ((date.getTime() / 1000));
    }

    public static class DefaultChartsData<T extends ChartData<?>> extends ChartsData {
        private final T data;
        private IAxisValueFormatter xValueFormatter;

        public DefaultChartsData(T data, IAxisValueFormatter xValueFormatter) {
            this.xValueFormatter = xValueFormatter;
            this.data = data;
        }

        public IAxisValueFormatter getXValueFormatter() {
            return xValueFormatter;
        }

        public T getData() {
            return data;
        }
    }

    protected static class SampleXLabelFormatter implements IAxisValueFormatter {
        private final TimestampTranslation tsTranslation;
        SimpleDateFormat annotationDateFormat = new SimpleDateFormat("HH:mm");
//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Calendar cal = GregorianCalendar.getInstance();

        public SampleXLabelFormatter(TimestampTranslation tsTranslation) {
            this.tsTranslation = tsTranslation;

        }
        // TODO: this does not work. Cannot use precomputed labels
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            cal.clear();
            int ts = (int) value;
            cal.setTimeInMillis(tsTranslation.toOriginalValue(ts) * 1000L);
            Date date = cal.getTime();
            String dateString = annotationDateFormat.format(date);
            return dateString;
        }
    }

    protected static class PreformattedXIndexLabelFormatter implements IAxisValueFormatter {
        private ArrayList<String> xLabels;

        public PreformattedXIndexLabelFormatter(ArrayList<String> xLabels) {
            this.xLabels = xLabels;

        }
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            int index = (int) value;
            if (xLabels == null || index >= xLabels.size()) {
                return String.valueOf(value);
            }
            return xLabels.get(index);
        }
    }

    /**
     * Awkward class that helps in translating long timestamp
     * values to float (sic!) values. It basically rebases all
     * timestamps to a base (the very first) timestamp value.
     *
     * It does this so that the large timestamp values can be used
     * floating point values, where the mantissa is just 24 bits.
     */
    protected static class TimestampTranslation {
        private int tsOffset = -1;

        public int shorten(int timestamp) {
            if (tsOffset == -1) {
                tsOffset = timestamp;
                return 0;
            }
            return timestamp - tsOffset;
        }

        public int toOriginalValue(int timestamp) {
            if (tsOffset == -1) {
                return timestamp;
            }
            return timestamp + tsOffset;
        }
    }
}
