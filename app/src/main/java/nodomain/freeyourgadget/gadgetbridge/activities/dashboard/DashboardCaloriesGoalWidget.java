package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.CaloriesDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardCaloriesGoalWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardCaloriesGoalWidget extends AbstractGaugeWidget {
    public DashboardCaloriesGoalWidget() {
        super(R.string.calories, "calories", CaloriesDailyFragment.GaugeViewMode.TOTAL_CALORIES_GOAL.toString());
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardCaloriesGoalWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardCaloriesGoalWidget fragment = new DashboardCaloriesGoalWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsActiveCalories();
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        dashboardData.getCaloriesTotal();
        dashboardData.getCaloriesGoalFactor();
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        setText(String.valueOf(dashboardData.getCaloriesTotal()));
        final int colorCalories = ContextCompat.getColor(GBApplication.getContext(), R.color.calories_color);
        drawSimpleGauge(
                colorCalories,
                dashboardData.getCaloriesGoalFactor()
        );
    }
}
