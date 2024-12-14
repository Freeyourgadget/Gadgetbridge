/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.card.MaterialCardView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCaloriesActiveGoalWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardBodyEnergyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCalendarActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCaloriesTotalSegmentedWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardGoalsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardHrvWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepScoreWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressBreakdownWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSegmentedWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSimpleWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxCyclingWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxAnyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxRunningWidget;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DashboardFragment extends Fragment implements MenuProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardFragment.class);

    private final Calendar day = GregorianCalendar.getInstance();
    private TextView textViewDate;
    private TextView arrowRight;
    private GridLayout gridLayout;
    private final Map<String, AbstractDashboardWidget> widgetMap = new HashMap<>();
    private DashboardData dashboardData = new DashboardData();
    private boolean isConfigChanged = false;

    private ActivityResultLauncher<Intent> calendarLauncher;
    private final ActivityResultCallback<ActivityResult> calendarCallback = result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            long timeMillis = result.getData().getLongExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, 0);
            if (timeMillis != 0) {
                day.setTimeInMillis(timeMillis);
                fullRefresh();
            }
        }
    };

    public static final String ACTION_CONFIG_CHANGE = "nodomain.freeyourgadget.gadgetbridge.activities.dashboardfragment.action.config_change";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case GBApplication.ACTION_NEW_DATA:
                    final GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        if (dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) {
                            refresh();
                        }
                    }
                    break;
                case ACTION_CONFIG_CHANGE:
                    isConfigChanged = true;
                    break;
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View dashboardView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        textViewDate = dashboardView.findViewById(R.id.dashboard_date);
        gridLayout = dashboardView.findViewById(R.id.dashboard_gridlayout);

        calendarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                calendarCallback
        );

        // Increase column count on landscape, tablets and open foldables
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.widthPixels / displayMetrics.density >= 600) {
            gridLayout.setColumnCount(4);
        }

        final TextView arrowLeft = dashboardView.findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            day.add(Calendar.DAY_OF_MONTH, -1);
            refresh();
        });
        arrowRight = dashboardView.findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameDay(today, day)) {
                day.add(Calendar.DAY_OF_MONTH, 1);
                refresh();
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("dashboard_data") && dashboardData.isEmpty()) {
            dashboardData = (DashboardData) savedInstanceState.getSerializable("dashboard_data");
        } else if (dashboardData.isEmpty()) {
            reloadPreferences();
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(ACTION_CONFIG_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        return dashboardView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isConfigChanged) {
            isConfigChanged = false;
            fullRefresh();
        } else if (dashboardData.isEmpty() || !widgetMap.containsKey("today")) {
            refresh();
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dashboard_data", dashboardData);
    }

    @Override
    public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.dashboard_show_calendar) {
            final Intent intent = new Intent(requireActivity(), DashboardCalendarActivity.class);
            intent.putExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, day.getTimeInMillis());
            calendarLauncher.launch(intent);
            return true;
        } else if (itemId == R.id.dashboard_settings) {
            final Intent intent = new Intent(requireActivity(), DashboardPreferencesActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void fullRefresh() {
        gridLayout.removeAllViews();
        widgetMap.clear();
        refresh();
    }

    private void refresh() {
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        dashboardData.clear();
        reloadPreferences();
        draw();
    }

    private void reloadPreferences() {
        Prefs prefs = GBApplication.getPrefs();
        dashboardData.showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        dashboardData.showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());
        dashboardData.hrIntervalSecs = prefs.getInt("dashboard_widget_today_hr_interval", 1) * 60;
        dashboardData.timeTo = (int) (day.getTimeInMillis() / 1000);
        dashboardData.timeFrom = DateTimeUtils.shiftDays(dashboardData.timeTo, -1);
    }

    private void draw() {
        Prefs prefs = GBApplication.getPrefs();
        String defaultWidgetsOrder = String.join(",", getResources().getStringArray(R.array.pref_dashboard_widgets_order_default));
        String widgetsOrderPref = prefs.getString("pref_dashboard_widgets_order", defaultWidgetsOrder);
        String[] widgetsOrder = widgetsOrderPref.split(",");

        Calendar today = GregorianCalendar.getInstance();
        if (DateTimeUtils.isSameDay(today, day)) {
            textViewDate.setText(requireContext().getString(R.string.activity_summary_today));
            arrowRight.setAlpha(0.5f);
        } else {
            textViewDate.setText(DateTimeUtils.formatDate(day.getTime()));
            arrowRight.setAlpha(1);
        }

        boolean cardsEnabled = prefs.getBoolean("dashboard_cards_enabled", true);

        for (String widgetName : widgetsOrder) {
            AbstractDashboardWidget widget = widgetMap.get(widgetName);
            if (widget == null) {
                int columnSpan = 1;
                switch (widgetName) {
                    case "today":
                        widget = DashboardTodayWidget.newInstance(dashboardData);
                        columnSpan = prefs.getBoolean("dashboard_widget_today_2columns", true) ? 2 : 1;
                        break;
                    case "goals":
                        widget = DashboardGoalsWidget.newInstance(dashboardData);
                        columnSpan = prefs.getBoolean("dashboard_widget_goals_2columns", true) ? 2 : 1;
                        break;
                    case "steps":
                        widget = DashboardStepsWidget.newInstance(dashboardData);
                        break;
                    case "distance":
                        widget = DashboardDistanceWidget.newInstance(dashboardData);
                        break;
                    case "activetime":
                        widget = DashboardActiveTimeWidget.newInstance(dashboardData);
                        break;
                    case "sleep":
                        widget = DashboardSleepWidget.newInstance(dashboardData);
                        break;
                    case "stress_simple":
                        widget = DashboardStressSimpleWidget.newInstance(dashboardData);
                        break;
                    case "stress_segmented":
                        widget = DashboardStressSegmentedWidget.newInstance(dashboardData);
                        break;
                    case "stress_breakdown":
                        widget = DashboardStressBreakdownWidget.newInstance(dashboardData);
                        break;
                    case "bodyenergy":
                        widget = DashboardBodyEnergyWidget.newInstance(dashboardData);
                        break;
                    case "hrv":
                        widget = DashboardHrvWidget.newInstance(dashboardData);
                        break;
                    case "vo2max_running":
                        widget = DashboardVO2MaxRunningWidget.newInstance(dashboardData);
                        break;
                    case "vo2max_cycling":
                        widget = DashboardVO2MaxCyclingWidget.newInstance(dashboardData);
                        break;
                    case "vo2max":
                        widget = DashboardVO2MaxAnyWidget.newInstance(dashboardData);
                        break;
                    case "calories_active":
                        widget = DashboardCaloriesActiveGoalWidget.newInstance(dashboardData);
                        break;
                    case "calories_segmented":
                        widget = DashboardCaloriesTotalSegmentedWidget.newInstance(dashboardData);
                        break;
                    case "sleepscore":
                        widget = DashboardSleepScoreWidget.newInstance(dashboardData);
                        break;
                    default:
                        LOG.error("Unknown dashboard widget {}", widgetName);
                        continue;
                }

                createWidget(widget, cardsEnabled, columnSpan);

                widgetMap.put(widgetName, widget);
            } else {
                widget.update();
            }
        }
    }

    private void createWidget(AbstractDashboardWidget widgetObj, boolean cardsEnabled, int columnSpan) {
        final float scale = requireContext().getResources().getDisplayMetrics().density;
        FragmentContainerView fragment = new FragmentContainerView(requireActivity());
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);
        getChildFragmentManager()
                .beginTransaction()
                .replace(fragmentId, widgetObj)
                .commitAllowingStateLoss(); // FIXME: #4007

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL, 1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        if (cardsEnabled) {
            MaterialCardView card = new MaterialCardView(requireActivity());
            int pixels_4dp = (int) (4 * scale + 0.5f);
            card.setRadius(pixels_4dp);
            card.setCardElevation(pixels_4dp);
            card.setContentPadding(pixels_4dp, pixels_4dp, pixels_4dp, pixels_4dp);
            card.setLayoutParams(layoutParams);
            card.addView(fragment);
            gridLayout.addView(card);
        } else {
            fragment.setLayoutParams(layoutParams);
            gridLayout.addView(fragment);
        }
    }

    /**
     * This class serves as a data collection object for all data points used by the various
     * dashboard widgets. Since retrieving this data can be costly, this class makes sure it will
     * only be done once. It will be passed to every widget, making sure they have the necessary
     * data available.
     */
    public static class DashboardData implements Serializable {
        public boolean showAllDevices;
        public Set<String> showDeviceList;
        public int hrIntervalSecs;
        public int timeFrom;
        public int timeTo;
        public final List<GeneralizedActivity> generalizedActivities = Collections.synchronizedList(new ArrayList<>());
        private int stepsTotal;
        private float stepsGoalFactor;
        private int restingCaloriesTotal;
        private int activeCaloriesTotal;
        private float activeCaloriesGoalFactor;
        private int caloriesTotal;
        private long sleepTotalMinutes;
        private float sleepGoalFactor;
        private float distanceTotalMeters;
        private float distanceGoalFactor;
        private long activeMinutesTotal;
        private float activeMinutesGoalFactor;
        private final Map<String, Serializable> genericData = new ConcurrentHashMap<>();

        public void clear() {
            restingCaloriesTotal = 0;
            activeCaloriesTotal = 0;
            activeCaloriesGoalFactor = 0;
            caloriesTotal = 0;
            stepsTotal = 0;
            stepsGoalFactor = 0;
            sleepTotalMinutes = 0;
            sleepGoalFactor = 0;
            distanceTotalMeters = 0;
            distanceGoalFactor = 0;
            activeMinutesTotal = 0;
            activeMinutesGoalFactor = 0;
            generalizedActivities.clear();
            genericData.clear();
        }

        public boolean isEmpty() {
            return (stepsTotal == 0 &&
                    stepsGoalFactor == 0 &&
                    restingCaloriesTotal == 0 &&
                    activeCaloriesTotal == 0 &&
                    activeCaloriesGoalFactor == 0 &&
                    caloriesTotal == 0 &&
                    sleepTotalMinutes == 0 &&
                    sleepGoalFactor == 0 &&
                    distanceTotalMeters == 0 &&
                    distanceGoalFactor == 0 &&
                    activeMinutesTotal == 0 &&
                    activeMinutesGoalFactor == 0 &&
                    genericData.isEmpty() &&
                    generalizedActivities.isEmpty());
        }

        public synchronized int getStepsTotal() {
            if (stepsTotal == 0)
                stepsTotal = DashboardUtils.getStepsTotal(this);
            return stepsTotal;
        }

        public synchronized float getStepsGoalFactor() {
            if (stepsGoalFactor == 0)
                stepsGoalFactor = DashboardUtils.getStepsGoalFactor(this);
            return stepsGoalFactor;
        }

        public synchronized float getDistanceTotal() {
            if (distanceTotalMeters == 0)
                distanceTotalMeters = DashboardUtils.getDistanceTotal(this);
            return distanceTotalMeters;
        }

        public synchronized float getDistanceGoalFactor() {
            if (distanceGoalFactor == 0)
                distanceGoalFactor = DashboardUtils.getDistanceGoalFactor(this);
            return distanceGoalFactor;
        }

        public synchronized long getActiveMinutesTotal() {
            if (activeMinutesTotal == 0)
                activeMinutesTotal = DashboardUtils.getActiveMinutesTotal(this);
            return activeMinutesTotal;
        }

        public synchronized float getActiveMinutesGoalFactor() {
            if (activeMinutesGoalFactor == 0)
                activeMinutesGoalFactor = DashboardUtils.getActiveMinutesGoalFactor(this);
            return activeMinutesGoalFactor;
        }

        public synchronized long getSleepMinutesTotal() {
            if (sleepTotalMinutes == 0)
                sleepTotalMinutes = DashboardUtils.getSleepMinutesTotal(this);
            return sleepTotalMinutes;
        }

        public synchronized float getSleepMinutesGoalFactor() {
            if (sleepGoalFactor == 0)
                sleepGoalFactor = DashboardUtils.getSleepMinutesGoalFactor(this);
            return sleepGoalFactor;
        }

        public synchronized int getActiveCaloriesTotal() {
            if (activeCaloriesTotal == 0)
                activeCaloriesTotal = DashboardUtils.getActiveCaloriesTotal(this);
            return activeCaloriesTotal;
        }

        public synchronized int getRestingCaloriesTotal() {
            if (restingCaloriesTotal == 0)
                restingCaloriesTotal = DashboardUtils.getRestingCaloriesTotal(this);
            return restingCaloriesTotal;
        }

        public synchronized float getActiveCaloriesGoalFactor() {
            if (activeCaloriesGoalFactor == 0)
                activeCaloriesGoalFactor = DashboardUtils.getActiveCaloriesGoalFactor(this);
            return activeCaloriesGoalFactor;
        }

        public void put(final String key, final Serializable value) {
            genericData.put(key, value);
        }

        public Serializable get(final String key) {
            return genericData.get(key);
        }

        /**
         * @noinspection UnusedReturnValue
         */
        public Serializable computeIfAbsent(final String key, final Supplier<Serializable> supplier) {
            return genericData.computeIfAbsent(key, absent -> supplier.get());
        }

        public static class GeneralizedActivity implements Serializable {
            public ActivityKind activityKind;
            public long timeFrom;
            public long timeTo;

            public GeneralizedActivity(ActivityKind activityKind, long timeFrom, long timeTo) {
                this.activityKind = activityKind;
                this.timeFrom = timeFrom;
                this.timeTo = timeTo;
            }

            @NonNull
            @Override
            public String toString() {
                return "Generalized activity: timeFrom=" + timeFrom + ", timeTo=" + timeTo + ", activityKind=" + activityKind + ", calculated duration: " + (timeTo - timeFrom) + " seconds";
            }
        }
    }
}