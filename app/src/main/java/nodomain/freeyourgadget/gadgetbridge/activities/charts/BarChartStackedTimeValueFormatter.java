package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.StackedValueFormatter;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class BarChartStackedTimeValueFormatter extends StackedValueFormatter {
    private float[] processedValues;
    private BarEntry lastEntry;
    private int lastNonZeroIndex;
    private int index = 0;

    public BarChartStackedTimeValueFormatter(boolean drawWholeStack, String suffix, int decimals) {
        super(drawWholeStack, suffix, decimals);
    }

    private int getLastNonZeroIndex(float[] array) {
        int last = 0;
        int i = 0;
        for(float v: array) {
            last = v == 0 ? last : i;
            i++;
        }
        return last;
    }

    @Override
    public String getBarStackedLabel(float value, BarEntry entry) {
        if (lastEntry != entry) {
            processedValues = entry.getYVals();
            lastEntry = entry;
            lastNonZeroIndex = getLastNonZeroIndex(processedValues);
            index = 0;
        }

        if (index == lastNonZeroIndex) {
            return getFormattedValue(processedValues);
        }

        index++;
        return "";
    }

    String getFormattedValue(float[] values) {
        float sum = 0;
        for (int i = 0; i < values.length - 1; i++) {
            sum += values[i];
        }
        return DateTimeUtils.minutesToHHMM((int) sum);
    }

}
