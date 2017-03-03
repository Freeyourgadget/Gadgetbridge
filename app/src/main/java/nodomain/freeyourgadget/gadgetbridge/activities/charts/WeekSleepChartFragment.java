package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
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
    String getPieDescription(int targetValue) {
        return getString(R.string.weeksleepchart_today_sleep_description, DateTimeUtils.minutesToHHMM(targetValue));
    }

    @Override
    int getGoal() {
        return 8 * 60; // FIXME
    }

    @Override
    float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSecondsDeepSleep = 0;
        long totalSecondsLightSleep = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.TYPE_DEEP_SLEEP) {
                totalSecondsDeepSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                totalSecondsLightSleep += amount.getTotalSeconds();
            }
        }
        return new float[]{(int) (totalSecondsDeepSleep / 60), (int) (totalSecondsLightSleep / 60)};
    }

    @Override
    protected String formatPieValue(int value) {
        return DateTimeUtils.formatDurationHoursMinutes((long) value, TimeUnit.MINUTES);
    }

    @Override
    IValueFormatter getPieValueFormatter() {
        return new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return formatPieValue((int) value);
            }
        };
    }

    @Override
    IValueFormatter getBarValueFormatter() {
        return new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    IAxisValueFormatter getYAxisFormatter() {
        return new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    int[] getColors() {
        return new int[]{akDeepSleep.color, akLightSleep.color};
    }
}
