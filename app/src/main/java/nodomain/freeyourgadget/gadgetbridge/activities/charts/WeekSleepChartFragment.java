package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class WeekSleepChartFragment extends AbstractWeekChartFragment {
    @Override
    public String getTitle() {
        return getString(R.string.weeksleepchart_sleep_a_week);
    }

    @Override
    int getGoal() {
        return 8 * 60; // FIXME
    }

    @Override
    int getTotalForSamples(List<? extends ActivitySample> activitySamples) {
        ActivityAnalysis analysis = new ActivityAnalysis();
        ActivityAmounts amounts = analysis.calculateActivityAmounts(activitySamples);
        long totalSeconds = 0;
        for (ActivityAmount amount : amounts.getAmounts()) {
            if ((amount.getActivityKind() & ActivityKind.TYPE_SLEEP) != 0) {
                totalSeconds += amount.getTotalSeconds();
            }
        }
        return (int) (totalSeconds / 60);
    }
}
