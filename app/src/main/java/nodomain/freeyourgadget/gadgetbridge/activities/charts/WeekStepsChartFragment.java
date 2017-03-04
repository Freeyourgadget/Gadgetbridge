package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;

public class WeekStepsChartFragment extends AbstractWeekChartFragment {
    @Override
    public String getTitle() {
        return getString(R.string.weekstepschart_steps_a_week);
    }

    @Override
    String getPieDescription(int targetValue) {
        return getString(R.string.weeksteps_today_steps_description, String.valueOf(targetValue));
    }

    @Override
    int getGoal() {
        GBDevice device = getChartsHost().getDevice();
        if (device != null) {
            return MiBandCoordinator.getFitnessGoal(device.getAddress());
        }
        return -1;
    }

    @Override
    int getOffsetHours() {
        return 0;
    }

    @Override
    float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        int totalSteps = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            totalSteps += amount.getTotalSteps();
            amount.getTotalSteps();
        }
        return new float[]{totalSteps};
    }

    @Override
    protected String formatPieValue(int value) {
        return String.valueOf(value);
    }

    @Override
    IValueFormatter getPieValueFormatter() {
        return null;
    }

    @Override
    IValueFormatter getBarValueFormatter() {
        return null;
    }

    @Override
    IAxisValueFormatter getYAxisFormatter() {
        return null;
    }

    @Override
    int[] getColors() {
        return new int[]{akActivity.color};
    }
}
