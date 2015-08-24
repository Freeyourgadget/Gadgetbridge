package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

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

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public abstract class AbstractChartFragment extends AbstractGBFragment {
    protected int ANIM_TIME = 350;

    private static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private final Set<String> mIntentFilterActions;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractChartFragment.this.onReceive(context, intent);
        }
    };
    private boolean mChartDirty = true;

    public boolean isChartDirty() {
        return mChartDirty;
    }

    public abstract String getTitle();

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

    protected ActivityConfig akActivity = new ActivityConfig(ActivityKind.TYPE_ACTIVITY, "Activity", Color.rgb(89, 178, 44));
    protected ActivityConfig akLightSleep = new ActivityConfig(ActivityKind.TYPE_LIGHT_SLEEP, "Light Sleep", Color.rgb(182, 191, 255));
    protected ActivityConfig akDeepSleep = new ActivityConfig(ActivityKind.TYPE_DEEP_SLEEP, "Deep Sleep", Color.rgb(76, 90, 255));

    protected static final int BACKGROUND_COLOR = Color.rgb(24, 22, 24);
    protected static final int DESCRIPTION_COLOR = Color.WHITE;
    protected static final int CHART_TEXT_COLOR = Color.WHITE;
    protected static final int LEGEND_TEXT_COLOR = Color.WHITE;

    protected AbstractChartFragment(String... intentFilterActions) {
        mIntentFilterActions = new HashSet<>();
        if (intentFilterActions != null) {
            mIntentFilterActions.addAll(Arrays.asList(intentFilterActions));
            mIntentFilterActions.add(ChartsHost.DATE_NEXT);
            mIntentFilterActions.add(ChartsHost.DATE_PREV);
            mIntentFilterActions.add(ChartsHost.REFRESH);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        for (String action : mIntentFilterActions) {
            filter.addAction(action);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = super.onCreateView(inflater, container, savedInstanceState);
//        updateDateInfo(mStartDate, mEndDate);
//        return view;
//    }

    private void setStartDate(Date date) {
        getHost().setStartDate(date);
    }

    private void setEndDate(Date date) {
        getHost().setEndDate(date);
    }

    public Date getStartDate() {
        return getHost().getStartDate();
    }

    public Date getEndDate() {
        return getHost().getEndDate();
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
        if (isChartDirty()) {
            refresh();
        }
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

    protected void handleDatePrev(Date startDate, Date endDate) {
        if (isVisibleInActivity()) {
            shiftDates(startDate, endDate, -1);
        }
        refreshIfVisible();
    }

    protected void handleDateNext(Date startDate, Date endDate) {
        if (isVisibleInActivity()) {
            shiftDates(startDate, endDate, +1);
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

    protected void shiftDates(Date startDate, Date endDate, int offset) {
        Date newStart = DateTimeUtils.shiftByDays(startDate, offset);
        Date newEnd = DateTimeUtils.shiftByDays(endDate, offset);

        setDateRange(newStart, newEnd);
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

    protected SampleProvider getProvider(GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider();
    }

    /**
     * Returns all kinds of samples for the given device.
     * To be called from a background thread.
     * @param device
     * @param tsFrom
     * @param tsTo
     */
    protected List<ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        SampleProvider provider = getProvider(device);
        return db.getAllActivitySamples(tsFrom, tsTo, provider);
    }

    private int getTSLast24Hours() {
        long now = System.currentTimeMillis();
        return (int) ((now / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
    }

    protected List<ActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        SampleProvider provider = getProvider(device);
        return db.getActivitySamples(tsFrom, tsTo, provider);
    }


    protected List<ActivitySample> getSleepSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            tsFrom = getTSLast24Hours();
        }
        SampleProvider provider = getProvider(device);
        return db.getSleepSamples(tsFrom, tsTo, provider);
    }

    protected List<ActivitySample> getTestSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JUNE, 10, 6, 40);
        // ignore provided date ranges
        tsTo = (int) ((cal.getTimeInMillis() / 1000) & 0xffffffff);
        tsFrom = tsTo - (24 * 60 * 60);

        SampleProvider provider = getProvider(device);
        return db.getAllActivitySamples(tsFrom, tsTo, provider);
    }

    protected void configureChartDefaults(Chart<?> chart) {
        // if enabled, the chart will always start at zero on the y-axis
        chart.setNoDataText(getString(R.string.chart_no_data_synchronize));

        // disable value highlighting
        chart.setHighlightEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);
    }

    protected void configureBarLineChartDefaults(BarLineChartBase<?> chart) {
        configureChartDefaults(chart);

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
        if (getHost().getDevice() != null) {
            mChartDirty = false;
            updateDateInfo(getStartDate(), getEndDate());
            createRefreshTask("Visualizing data", getActivity()).execute();
        }
    }

    /**
     * This method reads the data from the database, analyzes and prepares it for
     * the charts. This will be called from a background task, so there must not be
     * any UI access. #renderCharts will be automatically called after this method.
     */
    protected abstract void refreshInBackground(DBHandler db, GBDevice device);

    /**
     * Performs a re-rendering of the chart.
     * Always called from the UI thread.
     */
    protected abstract void renderCharts();

    protected void refresh(GBDevice gbDevice, BarLineChartBase chart, List<ActivitySample> samples) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.clear();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        LOG.info("number of samples:" + samples.size());
        if (samples.size() > 1) {
            float movement_divisor;
            boolean annotate = true;
            boolean use_steps_as_movement;
            SampleProvider provider = getProvider(gbDevice);

            int last_type = ActivityKind.TYPE_UNKNOWN;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            SimpleDateFormat annotationDateFormat = new SimpleDateFormat("HH:mm");

            int numEntries = samples.size();
            List<String> xLabels = new ArrayList<>(numEntries);
            List<BarEntry> activityEntries = new ArrayList<>(numEntries);
            List<Integer> colors = new ArrayList<>(numEntries); // this is kinda inefficient...

            for (int i = 0; i < numEntries; i++) {
                ActivitySample sample = samples.get(i);
                int type = sample.getKind();

                // determine start and end dates
                if (i == 0) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // make sure it's converted to long
                    date = cal.getTime();
                    dateStringFrom = dateFormat.format(date);
                } else if (i == samples.size() - 1) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // same here
                    date = cal.getTime();
                    dateStringTo = dateFormat.format(date);
                }

                float movement = sample.getIntensity();

                float value = movement;
                if (type == ActivityKind.TYPE_DEEP_SLEEP) {
                    value += SleepUtils.Y_VALUE_DEEP_SLEEP;
                    activityEntries.add(createBarEntry(value, i));
                    colors.add(akDeepSleep.color);
                } else {
                    if (type == ActivityKind.TYPE_LIGHT_SLEEP) {
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akLightSleep.color);
                    } else {
//                        short steps = sample.getSteps();
//                        if (use_steps_as_movement && steps != 0) {
//                            // I'm not sure using steps for this is actually a good idea
//                            movement = steps;
//                        }
//                        value = ((float) movement) / movement_divisor;
                        activityEntries.add(createBarEntry(value, i));
                        colors.add(akActivity.color);
                    }
                }

                String xLabel = "";
                if (annotate) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    String dateString = annotationDateFormat.format(date);
                    xLabel = dateString;
//                    if (last_type != type) {
//                        if (isSleep(last_type) && !isSleep(type)) {
//                            // woken up
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextColor(Color.WHITE);
//                            line.setTextSize(15);
//                            chart.getXAxis().addLimitLine(line);
//                        } else if (!isSleep(last_type) && isSleep(type)) {
//                            // fallen asleep
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextSize(15);
//                            line.setTextColor(Color.WHITE);
//                            chart.getXAxis().addLimitLine(line);
//                        }
//                    }
                    last_type = type;
                }
                xLabels.add(xLabel);
            }

            chart.getXAxis().setValues(xLabels);

            BarDataSet activitySet = createActivitySet(activityEntries, colors, "Activity");

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
            dataSets.add(activitySet);

            // create a data object with the datasets
            BarData data = new BarData(xLabels, dataSets);
            data.setGroupSpace(0);

            chart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            chart.setDescriptionPosition(?, ?);

            setupLegend(chart);

            chart.setData(data);
        }
    }

    protected abstract List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo);

    protected abstract void setupLegend(Chart chart);

    protected BarEntry createBarEntry(float value, int index) {
        return new BarEntry(value, index);
    }

    protected BarDataSet createActivitySet(List<BarEntry> values, List<Integer> colors, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
        set1.setColors(colors);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        return set1;
    }

    protected BarDataSet createDeepSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(76, 90, 255));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        return set1;
    }

    protected BarDataSet createLightSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);

//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(182, 191, 255));
        set1.setValueTextColor(CHART_TEXT_COLOR);
//        set1.setColor(Color.CYAN);
        return set1;
    }

    protected RefreshTask createRefreshTask(String task, Context context) {
        return new RefreshTask(task, context);
    }

    public class RefreshTask extends DBAccess {
        public RefreshTask(String task, Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(DBHandler db) {
            refreshInBackground(db, getHost().getDevice());
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            FragmentActivity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                renderCharts();
            } else {
                LOG.info("Not rendering charts because activity is not available anymore");
            }
        }
    }

    public void setDateRange(Date from, Date to) {
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException("Bad date range: " +from + ".." + to);
        }
        setStartDate(from);
        setEndDate(to);
    }

    protected void updateDateInfo(Date from, Date to) {
        if (from.equals(to)) {
            getHost().setDateInfo(DateTimeUtils.formatDate(from));
        } else {
            getHost().setDateInfo(DateTimeUtils.formatDateRange(from, to));
        }
    }

    protected List<ActivitySample> getSamples(DBHandler db, GBDevice device) {
        return getSamples(db, device, getTSStart(), getTSEnd());
    }

    private int getTSEnd() {
        return toTimestamp(getEndDate());
    }

    private int getTSStart() {
        return toTimestamp(getStartDate());
    }

    private int toTimestamp(Date date) {
        return (int) ((date.getTime() / 1000) & 0xffffffff);
    }

    protected ChartsHost getHost() {
        return (ChartsHost) getActivity();
    }
}
