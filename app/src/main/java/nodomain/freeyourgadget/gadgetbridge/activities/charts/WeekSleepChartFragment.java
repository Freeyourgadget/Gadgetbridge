/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo,
    Pavel Elagin, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class WeekSleepChartFragment extends AbstractWeekChartFragment {
    @Override
    public String getTitle() {
        if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
            return getString(R.string.weeksleepchart_sleep_a_month);
        }
        else{
            return getString(R.string.weeksleepchart_sleep_a_week);
        }
    }

    @Override
    String getPieDescription(int targetValue) {
        return getString(R.string.weeksleepchart_today_sleep_description, DateTimeUtils.formatDurationHoursMinutes(targetValue, TimeUnit.MINUTES));
    }

    @Override
    int getGoal() {
        return GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_SLEEP_DURATION, 8) * 60;
    }

    @Override
    int getOffsetHours() {
        return -12;
    }


    @Override
    protected long calculateBalance(ActivityAmounts activityAmounts) {
        long balance = 0;

        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.TYPE_DEEP_SLEEP ||
                    amount.getActivityKind() == ActivityKind.TYPE_LIGHT_SLEEP ||
                    amount.getActivityKind() == ActivityKind.TYPE_REM_SLEEP) {
                balance += amount.getTotalSeconds();
            }
        }
        return (int) (balance / 60);
    }

    @Override
    protected String getBalanceMessage(long balance, int targetValue) {
        if (balance > 0) {
            final long totalBalance = balance - ((long)targetValue * TOTAL_DAYS_FOR_AVERAGE);
            if (totalBalance > 0)
                return getString(R.string.overslept, getHM(totalBalance));
            else
                return getString(R.string.lack_of_sleep, getHM(Math.abs(totalBalance)));
        } else
            return getString(R.string.no_data);
    }

    @Override
    float[] getTotalsForActivityAmounts(ActivityAmounts activityAmounts) {
        long totalSecondsDeepSleep = 0;
        long totalSecondsLightSleep = 0;
        long totalSecondsRemSleep = 0;
        for (ActivityAmount amount : activityAmounts.getAmounts()) {
            if (amount.getActivityKind() == ActivityKind.TYPE_DEEP_SLEEP) {
                totalSecondsDeepSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                totalSecondsLightSleep += amount.getTotalSeconds();
            } else if (amount.getActivityKind() == ActivityKind.TYPE_REM_SLEEP) {
                totalSecondsRemSleep += amount.getTotalSeconds();
            }
        }
        int totalMinutesDeepSleep = (int) (totalSecondsDeepSleep / 60);
        int totalMinutesLightSleep = (int) (totalSecondsLightSleep / 60);
        int totalMinutesRemSleep = (int) (totalSecondsRemSleep / 60);
        if (supportsRemSleep(getChartsHost().getDevice())) {
            return new float[]{totalMinutesDeepSleep, totalMinutesLightSleep, totalMinutesRemSleep};
        } else {
            return new float[]{totalMinutesDeepSleep, totalMinutesLightSleep};
        }
    }

    @Override
    protected String formatPieValue(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    @Override
    String[] getPieLabels() {
        String[] labels = {
                getString(R.string.abstract_chart_fragment_kind_deep_sleep),
                getString(R.string.abstract_chart_fragment_kind_light_sleep)
        };
        if (supportsRemSleep(getChartsHost().getDevice())) {
            labels = ArrayUtils.add(labels,  getString(R.string.abstract_chart_fragment_kind_rem_sleep));
        }
        return labels;
    }

    @Override
    ValueFormatter getPieValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatPieValue((long) value);
            }
        };
    }

    @Override
    ValueFormatter getBarValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    ValueFormatter getYAxisFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateTimeUtils.minutesToHHMM((int) value);
            }
        };
    }

    @Override
    int[] getColors() {
        int[] colors = {akDeepSleep.color, akLightSleep.color};
        if (supportsRemSleep(getChartsHost().getDevice())) {
            colors = ArrayUtils.add(colors, akRemSleep.color);
        }
        return colors;
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(2);

        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsRemSleep(getChartsHost().getDevice())) {
            LegendEntry remSleepEntry = new LegendEntry();
            remSleepEntry.label = akRemSleep.label;
            remSleepEntry.formColor = akRemSleep.color;
            legendEntries.add(remSleepEntry);
        }

        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    @Override
    String getAverage(float value) {
        return getHM((long)value);
    }

}
