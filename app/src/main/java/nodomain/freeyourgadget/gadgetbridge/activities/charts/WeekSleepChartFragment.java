package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

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
    int getTotalForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSeconds = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if ((amount.getActivityKind() & ActivityKind.TYPE_SLEEP) != 0) {
                totalSeconds += amount.getTotalSeconds();
            }
        }
        return (int) (totalSeconds / 60);
    }

    @Override
    protected String formatPieValue(int value) {
        return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.MINUTES);
    }

    @Override
    IValueFormatter getFormatter() {
        return new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return formatPieValue((int) value);
            }
        };
    }

    @Override
    Integer getMainColor() {
        return akLightSleep.color;
    }
}
