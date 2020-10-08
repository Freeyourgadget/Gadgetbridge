package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;

import java.util.Date;

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
        Date start = item.getStepStart();
        Date end = item.getStepEnd();
        if (activityKind == ActivityKind.TYPE_UNKNOWN) {
            return getContext().getString(R.string.chart_no_active_data);
        }
        return activityKindLabel + " " + DateTimeUtils.formatTime(start.getHours(), start.getMinutes()) + " - " + DateTimeUtils.formatTime(end.getHours(), end.getMinutes());
    }

    @Override
    protected String getDetails(StepAnalysis.StepSession item) {
        if (item.getActivityKind() == ActivityKind.TYPE_UNKNOWN) {
            return getContext().getString(R.string.chart_get_active_and_synchronize);
        }
        return getContext().getString(R.string.steps) +": " + item.getSteps();
    }

    @Override
    protected int getIcon(StepAnalysis.StepSession item) {
        int activityKind = item.getActivityKind();
        return ActivityKind.getIconId(activityKind);
    }


}
