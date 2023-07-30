/*  Copyright (C) 2015-2020 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Dikay900, Pavel Elagin, vanous, walkjivefly

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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

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
public abstract class AbstractChartFragment<D extends ChartsData> extends AbstractGBFragment {
    protected final int ANIM_TIME = 250;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChartFragment.class);
    @SuppressLint("SimpleDateFormat")
    protected final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final Set<String> mIntentFilterActions;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractChartFragment.this.onReceive(context, intent);
        }
    };

    private boolean mChartDirty = true;
    private AsyncTask refreshTask;

    protected AbstractChartFragment(String... intentFilterActions) {
        mIntentFilterActions = new HashSet<>();
        if (intentFilterActions != null) {
            mIntentFilterActions.addAll(Arrays.asList(intentFilterActions));
        }
        mIntentFilterActions.add(ChartsHost.DATE_NEXT_DAY);
        mIntentFilterActions.add(ChartsHost.DATE_PREV_DAY);
        mIntentFilterActions.add(ChartsHost.DATE_NEXT_WEEK);
        mIntentFilterActions.add(ChartsHost.DATE_PREV_WEEK);
        mIntentFilterActions.add(ChartsHost.DATE_NEXT_MONTH);
        mIntentFilterActions.add(ChartsHost.DATE_PREV_MONTH);
        mIntentFilterActions.add(ChartsHost.REFRESH);
    }

    @Override
    public abstract String getTitle();

    /**
     * Called in the fragment's onCreate, initializes this fragment.
     */
    protected abstract void init();

    /**
     * This method reads the data from the database, analyzes and prepares it for
     * the charts. This will be called from a background task, so there must not be
     * any UI access. #updateChartsInUIThread and #renderCharts will be automatically called after this method.
     */
    protected abstract D refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device);

    /**
     * Triggers the actual (re-) rendering of the chart.
     * Always called from the UI thread.
     */
    protected abstract void renderCharts();

    protected abstract void setupLegend(Chart<?> chart);

    protected abstract void updateChartsnUIThread(D chartsData);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        final IntentFilter filter = new IntentFilter();
        for (String action : mIntentFilterActions) {
            filter.addAction(action);
        }
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mReceiver);
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
        if (mChartDirty) {
            refresh();
        }
    }

    protected ChartsHost getChartsHost() {
        return (ChartsHost) requireActivity();
    }

    private void setStartDate(Date date) {
        getChartsHost().setStartDate(date);
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

    protected int getTSEnd() {
        return toTimestamp(getEndDate());
    }

    protected int getTSStart() {
        return toTimestamp(getStartDate());
    }

    protected int toTimestamp(Date date) {
        return (int) ((date.getTime() / 1000));
    }

    protected void showDateBar(boolean show) {
        getChartsHost().getDateBar().setVisibility(show ? View.VISIBLE : View.GONE);
    }

    protected void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ChartsHost.REFRESH.equals(action)) {
            refresh();
        } else if (ChartsHost.DATE_NEXT_DAY.equals(action)) {
            handleDate(getStartDate(), getEndDate(), +1);
        } else if (ChartsHost.DATE_PREV_DAY.equals(action)) {
            handleDate(getStartDate(), getEndDate(), -1);
        } else if (ChartsHost.DATE_NEXT_WEEK.equals(action)) {
            handleDate(getStartDate(), getEndDate(), +7);
        } else if (ChartsHost.DATE_PREV_WEEK.equals(action)) {
            handleDate(getStartDate(), getEndDate(), -7);
        } else if (ChartsHost.DATE_NEXT_MONTH.equals(action)) {
            //calculate dates to jump by month but keep subsequent logic working
            int time1 = DateTimeUtils.shiftMonths((int) (getStartDate().getTime() / 1000), 1);
            int time2 = DateTimeUtils.shiftMonths((int) (getEndDate().getTime() / 1000), 1);
            Date date1 = DateTimeUtils.shiftByDays(new Date(time1 * 1000L), 30);
            Date date2 = DateTimeUtils.shiftByDays(new Date(time2 * 1000L), 30);
            handleDate(date1, date2, -30);
        } else if (ChartsHost.DATE_PREV_MONTH.equals(action)) {
            int time1 = DateTimeUtils.shiftMonths((int) (getStartDate().getTime() / 1000), -1);
            int time2 = DateTimeUtils.shiftMonths((int) (getEndDate().getTime() / 1000), -1);
            Date date1 = DateTimeUtils.shiftByDays(new Date(time1 * 1000L), -30);
            Date date2 = DateTimeUtils.shiftByDays(new Date(time2 * 1000L), -30);
            handleDate(date1, date2, 30);
        }
    }

    /**
     * Default implementation shifts the dates by one day, if visible
     * and calls #refreshIfVisible().
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param offset    the offset, in days
     */
    private void handleDate(Date startDate, Date endDate, Integer offset) {
        if (isVisibleInActivity()) {
            if (!shiftDates(startDate, endDate, offset)) {
                return;
            }
        }
        refreshIfVisible();
    }

    private void refreshIfVisible() {
        if (isVisibleInActivity()) {
            refresh();
        } else {
            mChartDirty = true;
        }
    }

    /**
     * Shifts the given dates by offset days. offset may be positive or negative.
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param offset    a positive or negative number of days to shift the dates
     * @return true if the shift was successful and false otherwise
     */
    private boolean shiftDates(Date startDate, Date endDate, int offset) {
        Date newStart = DateTimeUtils.shiftByDays(startDate, offset);
        Date newEnd = DateTimeUtils.shiftByDays(endDate, offset);
        Date now = new Date();
        if (newEnd.after(now)) { //allow to jump to the end (now) if week/month reach after now
            newEnd = now;
            newStart = DateTimeUtils.shiftByDays(now, -1);
        }
        return setDateRange(newStart, newEnd);
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

// commented out: this has weird bugs/side-effects at least on WeekStepsCharts
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
        LOG.info("Refreshing data for {} from {} to {}", getTitle(), sdf.format(getStartDate()), sdf.format(getEndDate()));

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

    private RefreshTask createRefreshTask(final String task, final Context context) {
        return new RefreshTask(task, context);
    }

    @SuppressLint("StaticFieldLeak")
    private final class RefreshTask extends DBAccess {
        private D chartsData;

        public RefreshTask(final String task, final Context context) {
            super(task, context);
        }

        @Override
        protected void doInBackground(final DBHandler db) {
            final ChartsHost chartsHost = getChartsHost();
            if (chartsHost != null) {
                chartsData = refreshInBackground(chartsHost, db, chartsHost.getDevice());
            } else {
                cancel(true);
            }
        }

        @Override
        protected void onPostExecute(final Object o) {
            super.onPostExecute(o);
            final FragmentActivity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                updateChartsnUIThread(chartsData);
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
     * @param from the start date
     * @param to   the end date
     */
    private boolean setDateRange(final Date from, final Date to) {
        if (from.compareTo(to) > 0) {
            throw new IllegalArgumentException("Bad date range: " + from + ".." + to);
        }
        final Date now = new Date();
        if (to.after(now) || //do not refresh chart if we reached now
                to.getTime() / 10000 == (getEndDate().getTime() / 10000)) {
            return false;
        }
        setStartDate(from);
        setEndDate(to);
        return true;
    }

    private void updateDateInfo(final Date from, final Date to) {
        if (from.equals(to)) {
            getChartsHost().setDateInfo(DateTimeUtils.formatDate(from));
        } else {
            getChartsHost().setDateInfo(DateTimeUtils.formatDateRange(from, to));
        }
    }
}
