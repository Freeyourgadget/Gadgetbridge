package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.formatter.IValueFormatter;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class WeekStepsChartFragment extends AbstractWeekChartFragment {
    @Override
    public String getTitle() {
        return getString(R.string.weekstepschart_steps_a_week);
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
    int getTotalForSamples(List<? extends ActivitySample> activitySamples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        return analysis.calculateTotalSteps(activitySamples);
    }

    @Override
    protected String formatPieValue(int value) {
        return String.valueOf(value);
    }

    @Override
    IValueFormatter getFormatter() {
        return null;
    }

    @Override
    Integer getMainColor() {
        return akActivity.color;
    }
}
