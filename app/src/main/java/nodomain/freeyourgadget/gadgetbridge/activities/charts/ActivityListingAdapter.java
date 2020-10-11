package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractItemAdapter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class ActivityListingAdapter extends AbstractItemAdapter<StepAnalysis.StepSession> {
    public ActivityListingAdapter(Context context) {
        super(context);
    }

    @Override
    protected String getName(StepAnalysis.StepSession item) {
        int activityKind = item.getActivityKind();
        String activityKindLabel = ActivityKind.asString(activityKind, getContext());
        Date startTime = item.getStepStart();
        Date endTime = item.getStepEnd();

        String fromTime = DateTimeUtils.formatTime(startTime.getHours(), startTime.getMinutes());
        String toTime = DateTimeUtils.formatTime(endTime.getHours(), endTime.getMinutes());
        String duration = DateTimeUtils.formatDurationHoursMinutes(endTime.getTime() - startTime.getTime(), TimeUnit.MILLISECONDS);

        if (activityKind == ActivityKind.TYPE_UNKNOWN) {
            return getContext().getString(R.string.chart_no_active_data);
        }
        return activityKindLabel + " " + duration + " (" + fromTime + " - " + toTime + ")";
    }

    @Override
    protected String getDetails(StepAnalysis.StepSession item) {
        String heartRate = "";
        if (item.getActivityKind() == ActivityKind.TYPE_UNKNOWN) {
            return getContext().getString(R.string.chart_get_active_and_synchronize);
        }
        if (item.getHeartRateAverage() > 50) {
            heartRate = "   ❤️ " + item.getHeartRateAverage();
        }

        return "👣 " + item.getSteps() + heartRate;
    }

    @Override
    protected int getIcon(StepAnalysis.StepSession item) {
        int activityKind = item.getActivityKind();
        return ActivityKind.getIconId(activityKind);
    }


}
