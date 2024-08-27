/*  Copyright (C) 2023-2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DefaultChartsData;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SampleXLabelFormatter;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.TimestampTranslation;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardTodayWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardTodayWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardTodayWidget.class);

    private View todayView;
    private LineChart mActivityChart;

    private boolean mode_24h;

    public DashboardTodayWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardTodayWidget.
     */
    public static DashboardTodayWidget newInstance(DashboardFragment.DashboardData dashboardData) {
        DashboardTodayWidget fragment = new DashboardTodayWidget();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        todayView = inflater.inflate(R.layout.dashboard_widget_today, container, false);
        mActivityChart = todayView.findViewById(R.id.dashboard_today_horizontal);

        if (dashboardData.generalizedActivities.isEmpty()) {
            fillData();
        } else {
            draw();
        }

        return todayView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActivityChart != null) fillData();
    }

    private void draw() {
        mActivityChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        //mActivityChart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
        mActivityChart.getAxisLeft().setDrawLabels(false);
        final Map<ActivityKind, List<Entry>> entries = new HashMap<ActivityKind, List<Entry>>() {{
            put(ActivityKind.NOT_MEASURED, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.NOT_WORN, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.LIGHT_SLEEP, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.SLEEP_ANY, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.REM_SLEEP, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.DEEP_SLEEP, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.AWAKE_SLEEP, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.EXERCISE, new ArrayList<>(dashboardData.generalizedActivities.size()));
            put(ActivityKind.ACTIVITY, new ArrayList<>(dashboardData.generalizedActivities.size()));
        }};
        final Map<ActivityKind, Integer> colors = new HashMap<ActivityKind, Integer>() {{
            put(ActivityKind.NOT_MEASURED, color_worn);
            put(ActivityKind.NOT_WORN, color_not_worn);
            put(ActivityKind.LIGHT_SLEEP, color_light_sleep);
            put(ActivityKind.SLEEP_ANY, color_light_sleep);
            put(ActivityKind.REM_SLEEP, color_rem_sleep);
            put(ActivityKind.DEEP_SLEEP, color_deep_sleep);
            put(ActivityKind.AWAKE_SLEEP, color_awake_sleep);
            put(ActivityKind.EXERCISE, color_exercise);
            put(ActivityKind.ACTIVITY, color_activity);
        }};
        final Map<ActivityKind, String> labels = new HashMap<ActivityKind, String>() {{
            put(ActivityKind.NOT_MEASURED, "Worn");
            put(ActivityKind.NOT_WORN, "Not Worn");
            put(ActivityKind.LIGHT_SLEEP, "Light Sleep");
            put(ActivityKind.SLEEP_ANY, "Sleep");
            put(ActivityKind.REM_SLEEP, "REM Sleep");
            put(ActivityKind.DEEP_SLEEP, "Deep Sleep");
            put(ActivityKind.AWAKE_SLEEP, "Awake Sleep");
            put(ActivityKind.EXERCISE, "Exercise");
            put(ActivityKind.ACTIVITY, "Activity");
        }};
        final Map<ActivityKind, Integer> height = new HashMap<ActivityKind, Integer>() {{
            put(ActivityKind.NOT_MEASURED, 10);
            put(ActivityKind.NOT_WORN, 5);
            put(ActivityKind.LIGHT_SLEEP, 30);
            put(ActivityKind.SLEEP_ANY, 30);
            put(ActivityKind.REM_SLEEP, 35);
            put(ActivityKind.DEEP_SLEEP, 25);
            put(ActivityKind.AWAKE_SLEEP, 40);
            put(ActivityKind.EXERCISE, 35);
            put(ActivityKind.ACTIVITY, 30);
        }};
        synchronized (dashboardData.generalizedActivities) {
            TimestampTranslation tsTranslation = new TimestampTranslation();

            for (DashboardFragment.DashboardData.GeneralizedActivity activity : dashboardData.generalizedActivities) {
                if (!entries.containsKey(activity.activityKind)) {
                    LOG.error("???? {}", activity.activityKind);
                    continue;
                }
                int tsFrom = tsTranslation.shorten((int) activity.timeFrom);
                int tsTo = tsTranslation.shorten((int) activity.timeTo);

                entries.get(activity.activityKind).add(new Entry(tsFrom, height.get(activity.activityKind)));
                entries.get(activity.activityKind).add(new Entry(tsTo, height.get(activity.activityKind)));

                for (final Map.Entry<ActivityKind, List<Entry>> e : entries.entrySet()) {
                    if (e.getKey() != activity.activityKind) {
                        entries.get(e.getKey()).add(new Entry(tsFrom, 0));
                        entries.get(e.getKey()).add(new Entry(tsTo, 0));
                    }
                }
            }

            for (final Map.Entry<ActivityKind, List<Entry>> e : entries.entrySet()) {
                List<Entry> e2 = entries.get(e.getKey());
                e2.add(new Entry(e2.get(e2.size() - 1).getX(), 0));
                e2.add(new Entry(tsTranslation.shorten(dashboardData.timeTo), 0));
            }

            List<ILineDataSet> lineDataSets = new ArrayList<>();
            for (final Map.Entry<ActivityKind, List<Entry>> e : entries.entrySet()) {
                lineDataSets.add(createDataSet(e.getValue(), colors.get(e.getKey()), labels.get(e.getKey())));
            }

            LineData lineData = new LineData(lineDataSets);
            mActivityChart.setData(lineData);
            mActivityChart.setDrawGridBackground(false);
            mActivityChart.setDrawBorders(false);
            mActivityChart.getAxisRight().setEnabled(false);
            mActivityChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            mActivityChart.getXAxis().setDrawGridLines(false);
            mActivityChart.getXAxis().setValueFormatter(new SampleXLabelFormatter(tsTranslation));
            mActivityChart.getAxisLeft().setDrawGridLines(false);
            mActivityChart.getAxisRight().setDrawGridLines(false);
            mActivityChart.getLegend().setEnabled(false);
            mActivityChart.animateX(250, Easing.EaseInOutQuart);
        }
    }

    protected LineDataSet createDataSet(List<Entry> values, Integer color, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setColor(color);
        set1.setDrawFilled(true);
        set1.setDrawCircles(false);
        set1.setFillColor(color);
        set1.setFillAlpha(255);
        set1.setDrawValues(false);
        //set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set1;
    }


    protected void fillData() {
        if (todayView == null) return;
        todayView.post(new Runnable() {
            @Override
            public void run() {
                FillDataAsyncTask myAsyncTask = new FillDataAsyncTask();
                myAsyncTask.execute();
            }
        });
    }

    private class FillDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private final TreeMap<Long, ActivityKind> activityTimestamps = new TreeMap<>();

        private void addActivity(long timeFrom, long timeTo, ActivityKind activityKind) {
            for (long i = timeFrom; i<=timeTo; i++) {
                // If the current timestamp isn't saved yet, do so immediately
                if (activityTimestamps.get(i) == null) {
                    activityTimestamps.put(i, activityKind);
                    continue;
                }
                // If the current timestamp is already saved, compare the activity kinds and
                // keep the most 'important' one
                switch (activityTimestamps.get(i)) {
                    case EXERCISE:
                        break;
                    case ACTIVITY:
                        if (activityKind == ActivityKind.EXERCISE)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case DEEP_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case LIGHT_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case REM_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case SLEEP_ANY:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP ||
                                activityKind == ActivityKind.REM_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    default:
                        activityTimestamps.put(i, activityKind);
                        break;
                }
            }
        }

        private void calculateWornSessions(List<ActivitySample> samples) {
            int firstTimestamp = 0;
            int lastTimestamp = 0;

            for (ActivitySample sample : samples) {
                if (sample.getHeartRate() < 10 && firstTimestamp == 0) continue;
                if (firstTimestamp == 0) firstTimestamp = sample.getTimestamp();
                if (lastTimestamp == 0) lastTimestamp = sample.getTimestamp();
                if ((sample.getHeartRate() < 10 || sample.getTimestamp() > lastTimestamp + dashboardData.hrIntervalSecs) && firstTimestamp != lastTimestamp) {
                    LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                    addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
                    if (sample.getHeartRate() < 10) {
                        firstTimestamp = 0;
                        lastTimestamp = 0;
                    } else {
                        firstTimestamp = sample.getTimestamp();
                        lastTimestamp = sample.getTimestamp();
                    }
                    continue;
                }
                lastTimestamp = sample.getTimestamp();
            }
            if (firstTimestamp != lastTimestamp) {
                LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
            }
        }

        private void createGeneralizedActivities() {
            DashboardFragment.DashboardData.GeneralizedActivity previous = null;
            long midDaySecond = dashboardData.timeFrom + (12 * 60 * 60);
            for (Map.Entry<Long, ActivityKind> activity : activityTimestamps.entrySet()) {
                long timestamp = activity.getKey();
                ActivityKind activityKind = activity.getValue();
                if (previous == null || previous.activityKind != activityKind || (!mode_24h && timestamp == midDaySecond) || previous.timeTo < timestamp - 60) {
                    previous = new DashboardFragment.DashboardData.GeneralizedActivity(activityKind, timestamp, timestamp);
                    dashboardData.generalizedActivities.add(previous);
                } else {
                    previous.timeTo = timestamp;
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            final long nanoStart = System.nanoTime();

            // Retrieve activity data
            dashboardData.generalizedActivities.clear();
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            List<ActivitySample> allActivitySamples = new ArrayList<>();
            List<ActivitySession> stepSessions = new ArrayList<>();
            List<BaseActivitySummary> activitySummaries = null;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = DashboardUtils.getAllSamples(dbHandler, dev, dashboardData);
                        allActivitySamples.addAll(activitySamples);
                        StepAnalysis stepAnalysis = new StepAnalysis();
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                    }
                }
                activitySummaries = DashboardUtils.getWorkoutSamples(dbHandler, dashboardData);
            } catch (Exception e) {
                LOG.warn("Could not retrieve activity amounts: ", e);
            }
            Collections.sort(allActivitySamples, (lhs, rhs) -> Integer.valueOf(lhs.getTimestamp()).compareTo(rhs.getTimestamp()));

            // Determine worn sessions from heart rate samples
            calculateWornSessions(allActivitySamples);

            // Integrate various data from multiple devices
            for (ActivitySample sample : allActivitySamples) {
                // Handle only TYPE_NOT_WORN and TYPE_SLEEP (including variants) here
                if (sample.getKind() != ActivityKind.NOT_WORN && (sample.getKind() == ActivityKind.NOT_MEASURED || !ActivityKind.isSleep(sample.getKind())))
                    continue;
                // Add to day results
                addActivity(sample.getTimestamp(), sample.getTimestamp() + 60, sample.getKind());
            }
            if (activitySummaries != null) {
                for (BaseActivitySummary baseActivitySummary : activitySummaries) {
                    addActivity(baseActivitySummary.getStartTime().getTime() / 1000, baseActivitySummary.getEndTime().getTime() / 1000, ActivityKind.EXERCISE);
                }
            }
            for (ActivitySession session : stepSessions) {
                addActivity(session.getStartTime().getTime() / 1000, session.getEndTime().getTime() / 1000, ActivityKind.ACTIVITY);
            }
            createGeneralizedActivities();

            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", DashboardTodayWidget.this.getClass().getSimpleName(), executionTime);

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {
            super.onPostExecute(unused);
            try {
                draw();
            } catch (final Exception e) {
                LOG.error("calling draw() failed", e);
            }
        }
    }
}