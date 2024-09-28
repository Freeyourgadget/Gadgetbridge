package nodomain.freeyourgadget.gadgetbridge.activities.dashboard.widgets;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardBodyEnergyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardGoalsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardHrvWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressBreakdownWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSegmentedWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStressSimpleWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxAnyWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxCyclingWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardVO2MaxRunningWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.data.DashboardData;

public class DashboardWidgetFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardWidgetFactory.class);

    @Nullable
    public static AbstractDashboardWidget createWidget(final String widgetName,
                                                       final DashboardData dashboardData) {
        switch (widgetName) {
            case "today":
                return DashboardTodayWidget.newInstance(dashboardData);
            case "goals":
                return DashboardGoalsWidget.newInstance(dashboardData);
            case "steps":
                return DashboardStepsWidget.newInstance(dashboardData);
            case "distance":
                return DashboardDistanceWidget.newInstance(dashboardData);
            case "activetime":
                return DashboardActiveTimeWidget.newInstance(dashboardData);
            case "sleep":
                return DashboardSleepWidget.newInstance(dashboardData);
            case "stress_simple":
                return DashboardStressSimpleWidget.newInstance(dashboardData);
            case "stress_segmented":
                return DashboardStressSegmentedWidget.newInstance(dashboardData);
            case "stress_breakdown":
                return DashboardStressBreakdownWidget.newInstance(dashboardData);
            case "bodyenergy":
                return DashboardBodyEnergyWidget.newInstance(dashboardData);
            case "hrv":
                return DashboardHrvWidget.newInstance(dashboardData);
            case "vo2max_running":
                return DashboardVO2MaxRunningWidget.newInstance(dashboardData);
            case "vo2max_cycling":
                return DashboardVO2MaxCyclingWidget.newInstance(dashboardData);
            case "vo2max":
                return DashboardVO2MaxAnyWidget.newInstance(dashboardData);
            default:
                LOG.error("Unknown dashboard widget: '{}'", widgetName);
        }

        return null;
    }
}
