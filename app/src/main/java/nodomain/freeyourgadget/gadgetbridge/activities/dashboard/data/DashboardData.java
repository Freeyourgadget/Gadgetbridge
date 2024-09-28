package nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * This class serves as a data collection object for all data points used by the various
 * dashboard widgets. Since retrieving this data can be costly, this class makes sure it will
 * only be done once. It will be passed to every widget, making sure they have the necessary
 * data available.
 */
public class DashboardData implements Serializable {
    public boolean showAllDevices;
    public Set<String> showDeviceList;
    public int hrIntervalSecs;
    public int timeFrom;
    public int timeTo;
    public final List<GeneralizedActivity> generalizedActivities = Collections.synchronizedList(new ArrayList<>());
    private int stepsTotal;
    private float stepsGoalFactor;
    private long sleepTotalMinutes;
    private float sleepGoalFactor;
    private float distanceTotalMeters;
    private float distanceGoalFactor;
    private long activeMinutesTotal;
    private float activeMinutesGoalFactor;
    private final Map<String, Serializable> genericData = new ConcurrentHashMap<>();

    public void clear() {
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

    public void reloadPreferences(final Calendar day) {
        Prefs prefs = GBApplication.getPrefs();
        showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());
        hrIntervalSecs = prefs.getInt("dashboard_widget_today_hr_interval", 1) * 60;
        timeTo = (int) (day.getTimeInMillis() / 1000);
        timeFrom = DateTimeUtils.shiftDays(timeTo, -1);
    }

    public boolean isEmpty() {
        return (stepsTotal == 0 &&
                stepsGoalFactor == 0 &&
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
