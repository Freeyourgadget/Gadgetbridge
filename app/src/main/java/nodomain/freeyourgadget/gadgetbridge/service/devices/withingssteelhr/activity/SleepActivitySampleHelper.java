/*  Copyright (C) 2021 Frank Ertl

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr.WithingsSteelHRSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.WithingsSteelHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/**
 * This class is needed for sleep tracking as the withings steel HR sends heartrate while sleeping in an extra activity.
 * This leads to breaking the sleep session in the sleep calculation of GB.
 */
public class SleepActivitySampleHelper {

    private static Logger logger = LoggerFactory.getLogger(SleepActivitySampleHelper.class);
    private static int mergeCount;

    public static WithingsSteelHRActivitySample mergeIfNecessary(WithingsSteelHRSampleProvider provider, WithingsSteelHRActivitySample sample) {
        if (!shouldMerge(sample)) {
            return sample;
        }

        WithingsSteelHRActivitySample overlappingSample = getOverlappingSample(provider, (int)sample.getTimestamp());
        if (overlappingSample != null) {
            sample = doMerge(overlappingSample, sample);
        }

        return sample;
    }

    private static WithingsSteelHRActivitySample getOverlappingSample(WithingsSteelHRSampleProvider provider, long timestamp) {
        List<WithingsSteelHRActivitySample> samples = provider.getActivitySamples((int)timestamp - 500, (int)timestamp);
        if (samples.isEmpty()) {
            return null;
        }

        for (int i = samples.size()-1; i >= 0; i--) {
            WithingsSteelHRActivitySample lastSample = samples.get(i);
            if (isNotHeartRateOnly(lastSample, (int) timestamp)) {
                return lastSample;
            }
        }

        return null;
    }

    private static boolean isNotHeartRateOnly(WithingsSteelHRActivitySample lastSample, int timestamp) {
        return lastSample.getRawKind() != ActivityKind.TYPE_NOT_MEASURED; // && lastSample.getTimestamp() <= timestamp && (lastSample.getTimestamp() + lastSample.getDuration()) >= timestamp);
    }

    private static boolean shouldMerge(WithingsSteelHRActivitySample sample) {
        return sample.getSteps() == 0
                && sample.getDistance() == 0
                && sample.getRawKind() == -1
                && sample.getCalories() == 0
                && sample.getHeartRate() > 1
                && sample.getRawIntensity() == 0;
    }

    private static WithingsSteelHRActivitySample doMerge(WithingsSteelHRActivitySample origin, WithingsSteelHRActivitySample update) {
        WithingsSteelHRActivitySample mergeResult = new WithingsSteelHRActivitySample();
        mergeResult.setTimestamp(update.getTimestamp());
        mergeResult.setRawKind(origin.getRawKind());
        mergeResult.setRawIntensity(origin.getRawIntensity());
        mergeResult.setDuration(origin.getDuration() - (update.getTimestamp() - origin.getTimestamp()));
        mergeResult.setDevice(origin.getDevice());
        mergeResult.setDeviceId(origin.getDeviceId());
        mergeResult.setUser(origin.getUser());
        mergeResult.setUserId(origin.getUserId());
        mergeResult.setProvider(origin.getProvider());
        mergeResult.setHeartRate(update.getHeartRate());
        return mergeResult;
    }
}
