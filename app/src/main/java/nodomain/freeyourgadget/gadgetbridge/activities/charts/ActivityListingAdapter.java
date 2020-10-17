package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractActivityListingAdapter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class ActivityListingAdapter extends AbstractActivityListingAdapter<StepAnalysis.StepSession> {
    public ActivityListingAdapter(Context context) {
        super(context);
    }

    @Override
    protected String getDateLabel(StepAnalysis.StepSession item) {
        return "";
    }

    @Override
    protected boolean hasGPS(StepAnalysis.StepSession item) {
        return false;
    }

    @Override
    protected boolean hasDate(StepAnalysis.StepSession item) {
        return false;
    }

    @Override
    protected String getTimeFrom(StepAnalysis.StepSession item) {
        Date time = item.getStartTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getTimeTo(StepAnalysis.StepSession item) {
        Date time = item.getEndTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getActivityName(StepAnalysis.StepSession item) {
        return ActivityKind.asString(item.getActivityKind(), getContext());
    }

    @Override
    protected String getStepLabel(StepAnalysis.StepSession item) {
        return String.valueOf(item.getSteps());
    }

    @Override
    protected String getDistanceLabel(StepAnalysis.StepSession item) {
        float distance = item.getDistance();
        String unit = "###m";
        if (distance > 2000) {
            distance = distance / 1000;
            unit = "###.#km";
        }
        DecimalFormat df = new DecimalFormat(unit);
        //DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
        //symbols.setGroupingSeparator(' ');
        return df.format(distance);
    }

    @Override
    protected String getHrLabel(StepAnalysis.StepSession item) {
        return String.valueOf(item.getHeartRateAverage());
    }

    @Override
    protected String getIntensityLabel(StepAnalysis.StepSession item) {
        DecimalFormat df = new DecimalFormat("###.#");
        return df.format(item.getIntensity());
    }

    @Override
    protected String getDurationLabel(StepAnalysis.StepSession item) {
        long duration = item.getEndTime().getTime() - item.getStartTime().getTime();
        return DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean hasHR(StepAnalysis.StepSession item) {
        if (item.getHeartRateAverage() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasIntensity(StepAnalysis.StepSession item) {
        if (item.getIntensity() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean hasDistance(StepAnalysis.StepSession item) {
        if (item.getDistance() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean hasSteps(StepAnalysis.StepSession item) {
        if (item.getSteps() > 0) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    protected int getIcon(StepAnalysis.StepSession item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }


}
