package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

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
    protected final int ANIM_TIME = 350;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChartFragment.class);

    private final Set<String> mIntentFilterActions;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractChartFragment.this.onReceive(context, intent);
        }
    };
    private boolean mChartDirty = true;
    private boolean supportsHeartrateChart = false;

    public boolean isChartDirty() {
        return mChartDirty;
    }

    public abstract String getTitle();

    public boolean supportsHeartrate() {
        return supportsHeartrateChart;
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
    protected int AK_ACTIVITY_COLOR;
    protected int AK_DEEP_SLEEP_COLOR;
    protected int AK_LIGHT_SLEEP_COLOR;
    protected int AK_NOT_WORN_COLOR;

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

        init();

        IntentFilter filter = new IntentFilter();
        for (String action : mIntentFilterActions) {
            filter.addAction(action);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    protected void init() {
        BACKGROUND_COLOR = getResources().getColor(R.color.background_material_light);
        DESCRIPTION_COLOR = getResources().getColor(R.color.primarytext);
        CHART_TEXT_COLOR = getResources().getColor(R.color.secondarytext);
        LEGEND_TEXT_COLOR = getResources().getColor(R.color.primarytext);
        HEARTRATE_COLOR = getResources().getColor(R.color.chart_heartrate);
        AK_ACTIVITY_COLOR = getResources().getColor(R.color.chart_activity_light);
        AK_DEEP_SLEEP_COLOR = getResources().getColor(R.color.chart_light_sleep_light);
        AK_LIGHT_SLEEP_COLOR = getResources().getColor(R.color.chart_deep_sleep_light);
        AK_NOT_WORN_COLOR = getResources().getColor(R.color.chart_not_worn_light);

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
        getChartsHost().getDateBar().setVisibility(show ? View.VISIBLE : View.GONE);
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

    protected SampleProvider getProvider(GBDevice device) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.getSampleProvider();
    }

    /**
     * Returns all kinds of samples for the given device.
     * To be called from a background thread.
     *
     * @param device
     * @param tsFrom
     * @param tsTo
     */
    protected List<ActivitySample> getAllSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider provider = getProvider(device);
        return db.getAllActivitySamples(tsFrom, tsTo, provider);
    }

    private int getTSLast24Hours(int tsTo) {
        return (tsTo) - (24 * 60 * 60); // -24 hours
    }

    protected List<ActivitySample> getActivitySamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider provider = getProvider(device);
        return db.getActivitySamples(tsFrom, tsTo, provider);
    }


    protected List<ActivitySample> getSleepSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        SampleProvider provider = getProvider(device);
        return db.getSleepSamples(tsFrom, tsTo, provider);
    }

    protected List<ActivitySample> getTestSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JUNE, 10, 6, 40);
        // ignore provided date ranges
        tsTo = (int) ((cal.getTimeInMillis() / 1000));
        tsFrom = tsTo - (24 * 60 * 60);

        SampleProvider provider = getProvider(device);
        return db.getAllActivitySamples(tsFrom, tsTo, provider);
    }

    protected void configureChartDefaults(Chart<?> chart) {
        // if enabled, the chart will always start at zero on the y-axis
        chart.setNoDataText(getString(R.string.chart_no_data_synchronize));

        // disable value highlighting
        chart.setHighlightPerTapEnabled(false);

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
        ChartsHost chartsHost = getChartsHost();
        if (chartsHost != null) {
            if (chartsHost.getDevice() != null) {
                mChartDirty = false;
                updateDateInfo(getStartDate(), getEndDate());
                createRefreshTask("Visualizing data", getActivity()).execute();
            }
        }
    }

    /**
     * This method reads the data from the database, analyzes and prepares it for
     * the charts. This will be called from a background task, so there must not be
     * any UI access. #renderCharts will be automatically called after this method.
     */
    protected abstract void refreshInBackground(DBHandler db, GBDevice device);

    /**
     * Triggers the actual (re-) rendering of the chart.
     * Always called from the UI thread.
     */
    protected abstract void renderCharts();

    protected void refresh(GBDevice gbDevice, BarLineChartBase chart, List<ActivitySample> samples) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.clear();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        LOG.info("" + getTitle() + ": number of samples:" + samples.size());
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
            boolean hr = supportsHeartrate();
            List<Entry> heartrateEntries = hr ? new ArrayList<Entry>(numEntries) : null;
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
                switch (type) {
                    case ActivityKind.TYPE_DEEP_SLEEP:
                        value += SleepUtils.Y_VALUE_DEEP_SLEEP;
                        colors.add(akDeepSleep.color);
                        break;
                    case ActivityKind.TYPE_LIGHT_SLEEP:
                        colors.add(akLightSleep.color);
                        break;
                    case ActivityKind.TYPE_NOT_WORN:
                        value = SleepUtils.Y_VALUE_DEEP_SLEEP; //a small value, just to show something on the graphs
                        colors.add(akNotWorn.color);
                        break;
                    default:
//                        short steps = sample.getSteps();
//                        if (use_steps_as_movement && steps != 0) {
//                            // I'm not sure using steps for this is actually a good idea
//                            movement = steps;
//                        }
//                        value = ((float) movement) / movement_divisor;
                        colors.add(akActivity.color);
                }
                activityEntries.add(createBarEntry(value, i));
                if (hr) {
                    heartrateEntries.add(createLineEntry(sample.getCustomValue(), i));
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
            // create a data object with the datasets
            CombinedData combinedData = new CombinedData(xLabels);
            List<BarDataSet> list = new ArrayList<>();
            list.add(activitySet);
            BarData barData = new BarData(xLabels, list);
            barData.setGroupSpace(0);
            combinedData.setData(barData);

            if (hr) {
                LineDataSet heartrateSet = createHeartrateSet(heartrateEntries, "Heart Rate");
                LineData lineData = new LineData(xLabels, heartrateSet);
                combinedData.setData(lineData);
            }

            chart.setDescription("");
//            chart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            chart.setDescriptionPosition(?, ?);

            setupLegend(chart);

            chart.setData(combinedData);
        }
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
    protected abstract List<ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo);

    protected abstract void setupLegend(Chart chart);

    protected BarEntry createBarEntry(float value, int index) {
        return new BarEntry(value, index);
    }

    protected Entry createLineEntry(float value, int index) {
        return new Entry(value, index);
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
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set1;
    }

    protected LineDataSet createHeartrateSet(List<Entry> values, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setColor(HEARTRATE_COLOR);
//        set1.setColors(colors);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.RIGHT);
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
            ChartsHost chartsHost = getChartsHost();
            if (chartsHost != null) {
                refreshInBackground(db, chartsHost.getDevice());
            }
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
        return (int) ((date.getTime() / 1000));
    }
}
