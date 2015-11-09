package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.R;

public class ActivityAmount {
    private int activityKind;
    private short percent;
    private long totalSeconds;

    public ActivityAmount(int activityKind) {
        this.activityKind = activityKind;
    }

    public void addSeconds(long seconds) {
        totalSeconds += seconds;
    }

    public long getTotalSeconds() {
        return totalSeconds;
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
