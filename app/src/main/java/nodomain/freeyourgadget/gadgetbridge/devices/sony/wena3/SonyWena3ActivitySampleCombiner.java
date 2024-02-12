/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.BehaviorSample;

// The sole purpose of this part of code is to take the Behavior samples
// and drop them onto Steps samples (which are just your average activity data with the current
// architecture of Gadgetbridge)
public class SonyWena3ActivitySampleCombiner {
    private static final Logger LOG = LoggerFactory.getLogger(SonyWena3ActivitySampleCombiner.class);
    final SonyWena3ActivitySampleProvider activitySampleProvider;

    public SonyWena3ActivitySampleCombiner(SonyWena3ActivitySampleProvider activitySampleProvider) {
        this.activitySampleProvider = activitySampleProvider;
    }

    public void overlayBehaviorStartingAt(Date startDate, SonyWena3BehaviorSampleProvider behaviorSampleProvider) {
        List<Wena3BehaviorSample> behaviorSamples = behaviorSampleProvider.getAllSamples(startDate.getTime(), Long.MAX_VALUE);
        List<Wena3ActivitySample> alteredSamples = new ArrayList<>();
        for(Wena3BehaviorSample behaviorSample: behaviorSamples) {
            List<Wena3ActivitySample> activitySamplesForThisRange = activitySampleProvider.getAllActivitySamples((int)(behaviorSample.getTimestampFrom() / 1000L), (int)(behaviorSample.getTimestampTo() / 1000L));

            LOG.debug("Changing " + activitySamplesForThisRange.size() + " samples to behavior type: " + BehaviorSample.Type.LUT[behaviorSample.getRawKind()].name());
            for(Wena3ActivitySample activitySample: activitySamplesForThisRange) {
                activitySample.setRawKind(behaviorSample.getRawKind());
                alteredSamples.add(activitySample);
            }
        }
        activitySampleProvider.addGBActivitySamples(alteredSamples.toArray(new Wena3ActivitySample[alteredSamples.size()]));
    }

    public void overlayHeartRateStartingAt(Date startDate, SonyWena3HeartRateSampleProvider heartRateSampleProvider) {
        List<Wena3HeartRateSample> heartRateSamples = heartRateSampleProvider.getAllSamples(startDate.getTime(), Long.MAX_VALUE);
        List<Wena3ActivitySample> alteredSamples = new ArrayList<>();
        for(int i = 0; i < heartRateSamples.size(); i++) {
            HeartRateSample currentSample = heartRateSamples.get(i);
            HeartRateSample nextSample = (i == heartRateSamples.size() - 1) ? null : heartRateSamples.get(i + 1);

            List<Wena3ActivitySample> activitySamplesInRange = activitySampleProvider.getAllActivitySamples(
                    (int) (currentSample.getTimestamp() / 1000L),
                    (nextSample == null ? Integer.MAX_VALUE : ((int) (nextSample.getTimestamp() / 1000L)))
            );

            LOG.debug("Changing " + activitySamplesInRange.size() + " samples to heart rate: " + currentSample.getHeartRate());
            for(Wena3ActivitySample activitySample: activitySamplesInRange) {
                activitySample.setHeartRate(currentSample.getHeartRate());
                alteredSamples.add(activitySample);
            }
        }
        activitySampleProvider.addGBActivitySamples(alteredSamples.toArray(new Wena3ActivitySample[alteredSamples.size()]));
    }
}
