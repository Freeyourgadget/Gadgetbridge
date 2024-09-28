/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepAnalysis;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardData;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

public class DashboardUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardUtils.class);

    public static long getSteps(GBDevice device, DBHandler db, int timeTo) {
        Calendar day = GregorianCalendar.getInstance();
        day.setTimeInMillis(timeTo * 1000L);
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db)[0];
    }

    public static int getStepsTotal(DashboardData dashboardData) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        int totalSteps = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSteps += getSteps(dev, dbHandler, dashboardData.timeTo);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of steps: ", e);
        }
        return totalSteps;
    }

    public static float getStepsGoalFactor(DashboardData dashboardData) {
        ActivityUser activityUser = new ActivityUser();
        float stepsGoal = activityUser.getStepsGoal();
        float goalFactor = getStepsTotal(dashboardData) / stepsGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    public static long getSleep(GBDevice device, DBHandler db, int timeTo) {
        Calendar day = GregorianCalendar.getInstance();
        day.setTimeInMillis(timeTo * 1000L);
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(device, day, db)[1];
    }

    public static long getSleepMinutesTotal(DashboardData dashboardData) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalSleepMinutes = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSleepMinutes += getSleep(dev, dbHandler, dashboardData.timeTo);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of sleep: ", e);
        }
        return totalSleepMinutes;
    }

    public static float getSleepMinutesGoalFactor(DashboardData dashboardData) {
        ActivityUser activityUser = new ActivityUser();
        int sleepMinutesGoal = activityUser.getSleepDurationGoal() * 60;
        float goalFactor = (float) getSleepMinutesTotal(dashboardData) / sleepMinutesGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    public static float getDistanceTotal(DashboardData dashboardData) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalSteps = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalSteps += getSteps(dev, dbHandler, dashboardData.timeTo);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total distance: ", e);
        }
        ActivityUser activityUser = new ActivityUser();
        int stepLength = activityUser.getStepLengthCm();
        return totalSteps * stepLength * 0.01f;
    }

    public static float getDistanceGoalFactor(DashboardData dashboardData) {
        ActivityUser activityUser = new ActivityUser();
        int distanceGoal = activityUser.getDistanceGoalMeters();
        float goalFactor = getDistanceTotal(dashboardData) / distanceGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    public static long getActiveMinutesTotal(DashboardData dashboardData) {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        long totalActiveMinutes = 0;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            for (GBDevice dev : devices) {
                if ((dashboardData.showAllDevices || dashboardData.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                    totalActiveMinutes += getActiveMinutes(dev, dbHandler, dashboardData);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total amount of activity: ", e);
        }
        return totalActiveMinutes;
    }

    public static float getActiveMinutesGoalFactor(DashboardData dashboardData) {
        ActivityUser activityUser = new ActivityUser();
        int activeTimeGoal = activityUser.getActiveTimeGoalMinutes();
        float goalFactor = (float) getActiveMinutesTotal(dashboardData) / activeTimeGoal;
        if (goalFactor > 1) goalFactor = 1;

        return goalFactor;
    }

    public static long getActiveMinutes(GBDevice gbDevice, DBHandler db, DashboardData dashboardData) {
        ActivitySession stepSessionsSummary = new ActivitySession();
        List<ActivitySession> stepSessions;
        List<? extends ActivitySample> activitySamples = getAllSamples(db, gbDevice, dashboardData);
        StepAnalysis stepAnalysis = new StepAnalysis();

        boolean isEmptySummary = false;
        if (activitySamples != null) {
            stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
            if (stepSessions.toArray().length == 0) {
                isEmptySummary = true;
            }
            stepSessionsSummary = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
        }
        long duration = stepSessionsSummary.getEndTime().getTime() - stepSessionsSummary.getStartTime().getTime();
        return duration / 1000 / 60;
    }

    public static List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device, DashboardData dashboardData) {
        SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
        return provider.getAllActivitySamples(dashboardData.timeFrom, dashboardData.timeTo);
    }

    protected static SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.getSampleProvider(device, db.getDaoSession());
    }

    public static List<BaseActivitySummary> getWorkoutSamples(DBHandler db, DashboardData dashboardData) {
        return db.getDaoSession().getBaseActivitySummaryDao().queryBuilder().where(
                BaseActivitySummaryDao.Properties.StartTime.gt(new Date(dashboardData.timeFrom * 1000L)),
                BaseActivitySummaryDao.Properties.EndTime.lt(new Date(dashboardData.timeTo * 1000L))
        ).build().list();
    }
}
