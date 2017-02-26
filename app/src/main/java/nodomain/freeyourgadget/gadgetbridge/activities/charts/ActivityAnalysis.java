package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

class ActivityAnalysis {
    ActivityAmounts calculateActivityAmounts(List<? extends ActivitySample> samples) {
        ActivityAmount deepSleep = new ActivityAmount(ActivityKind.TYPE_DEEP_SLEEP);
        ActivityAmount lightSleep = new ActivityAmount(ActivityKind.TYPE_LIGHT_SLEEP);
        ActivityAmount notWorn = new ActivityAmount(ActivityKind.TYPE_NOT_WORN);
        ActivityAmount activity = new ActivityAmount(ActivityKind.TYPE_ACTIVITY);

        ActivityAmount previousAmount = null;
        ActivitySample previousSample = null;
        for (ActivitySample sample : samples) {
            ActivityAmount amount;
            switch (sample.getKind()) {
                case ActivityKind.TYPE_DEEP_SLEEP:
                    amount = deepSleep;
                    break;
                case ActivityKind.TYPE_LIGHT_SLEEP:
                    amount = lightSleep;
                    break;
                case ActivityKind.TYPE_NOT_WORN:
                    amount = notWorn;
                    break;
                case ActivityKind.TYPE_ACTIVITY:
                default:
                    amount = activity;
                    break;
            }

            int steps = sample.getSteps();
            if (steps > 0) {
                amount.addSteps(sample.getSteps());
            }

            if (previousSample != null) {
                long timeDifference = sample.getTimestamp() - previousSample.getTimestamp();
                if (previousSample.getRawKind() == sample.getRawKind()) {
                    amount.addSeconds(timeDifference);
                } else {
                    long sharedTimeDifference = (long) (timeDifference / 2.0f);
                    previousAmount.addSeconds(sharedTimeDifference);
                    amount.addSeconds(sharedTimeDifference);
                }
            }

            previousAmount = amount;
            previousSample = sample;
        }

        ActivityAmounts result = new ActivityAmounts();
        if (deepSleep.getTotalSeconds() > 0) {
            result.addAmount(deepSleep);
        }
        if (lightSleep.getTotalSeconds() > 0) {
            result.addAmount(lightSleep);
        }
        if (activity.getTotalSeconds() > 0) {
            result.addAmount(activity);
        }
        result.calculatePercentages();

        return result;
    }

    int calculateTotalSteps(List<? extends ActivitySample> samples) {
        int totalSteps = 0;
        for (ActivitySample sample : samples) {
            int steps = sample.getSteps();
            if (steps > 0) {
                totalSteps += sample.getSteps();
            }
        }
        return totalSteps;
    }
}
