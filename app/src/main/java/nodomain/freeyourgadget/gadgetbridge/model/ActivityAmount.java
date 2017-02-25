package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ActivityAmount {
    private final int activityKind;
    private short percent;
    private long totalSeconds;
    private long totalSteps;

    public ActivityAmount(int activityKind) {
        this.activityKind = activityKind;
    }

    public void addSeconds(long seconds) {
        totalSeconds += seconds;
    }

    public void addSteps(long steps) {
        totalSteps += steps;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public long getTotalSteps() {
        return totalSteps;
    }

    public int getActivityKind() {
        return activityKind;
    }

    public short getPercent() {
        return percent;
    }

    public void setPercent(short percent) {
        this.percent = percent;
    }

    public String getName(Context context) {
        switch (activityKind) {
            case ActivityKind.TYPE_DEEP_SLEEP:
                return context.getString(R.string.abstract_chart_fragment_kind_deep_sleep);
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return context.getString(R.string.abstract_chart_fragment_kind_light_sleep);
        }
        return context.getString(R.string.abstract_chart_fragment_kind_activity);
    }
}
