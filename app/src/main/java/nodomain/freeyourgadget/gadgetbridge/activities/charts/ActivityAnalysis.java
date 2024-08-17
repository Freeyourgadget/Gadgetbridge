/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Pavel Elagin, Petr Vaněk, Vebryn

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmount;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityAmounts;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class ActivityAnalysis {
    public static final Logger LOG = LoggerFactory.getLogger(ActivityAnalysis.class);

    // store raw steps and duration
    protected HashMap<Integer, Long> stats = new HashMap<Integer, Long>();
    // max speed determined from samples
    private int maxSpeed = 0;

    public ActivityAmounts calculateActivityAmounts(List<? extends ActivitySample> samples) {
        ActivityAmount deepSleep = new ActivityAmount(ActivityKind.DEEP_SLEEP);
        ActivityAmount lightSleep = new ActivityAmount(ActivityKind.LIGHT_SLEEP);
        ActivityAmount remSleep = new ActivityAmount(ActivityKind.REM_SLEEP);
        ActivityAmount awakeSleep = new ActivityAmount(ActivityKind.AWAKE_SLEEP);
        ActivityAmount notWorn = new ActivityAmount(ActivityKind.NOT_WORN);
        ActivityAmount activity = new ActivityAmount(ActivityKind.ACTIVITY);

        ActivityAmount previousAmount = null;
        ActivitySample previousSample = null;
        for (ActivitySample sample : samples) {
            ActivityAmount amount;
            switch (sample.getKind()) {
                case DEEP_SLEEP:
                    amount = deepSleep;
                    break;
                case LIGHT_SLEEP:
                    amount = lightSleep;
                    break;
                case REM_SLEEP:
                    amount = remSleep;
                    break;
                case AWAKE_SLEEP:
                    amount = awakeSleep;
                    break;
                case NOT_WORN:
                    amount = notWorn;
                    break;
                case ACTIVITY:
                default:
                    amount = activity;
                    break;
            }

            int steps = sample.getSteps();
            if (steps > 0) {
                amount.addSteps(steps);
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

                // add time
                if (steps > 0 && sample.getKind() == ActivityKind.ACTIVITY) {
                    if (steps > maxSpeed) {
                        maxSpeed = steps;
                    }

                    if (!stats.containsKey(steps)) {
//                        LOG.debug("Adding: " + steps);
                        stats.put(steps, timeDifference);
                    } else {
                        long time = stats.get(steps);
//                        LOG.debug("Updating: " + steps + " " + timeDifference + time);
                        stats.put(steps, timeDifference + time);
                    }
                }
            }

            amount.setStartDate(sample.getTimestamp());
            amount.setEndDate(sample.getTimestamp());

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
        if (remSleep.getTotalSeconds() > 0) {
            result.addAmount(remSleep);
        }
        if (awakeSleep.getTotalSeconds() > 0) {
            result.addAmount(awakeSleep);
        }
        if (activity.getTotalSeconds() > 0) {
            result.addAmount(activity);
        }
        if (notWorn.getTotalSeconds() > 0) {
            result.addAmount(notWorn);
        }

        result.calculatePercentages();

        return result;
    }

    int calculateTotalSteps(List<? extends ActivitySample> samples) {
        int totalSteps = 0;
        for (ActivitySample sample : samples) {
            int steps = sample.getSteps();
            if (steps > 0) {
                totalSteps += steps;
            }
        }
        return totalSteps;
    }
}
