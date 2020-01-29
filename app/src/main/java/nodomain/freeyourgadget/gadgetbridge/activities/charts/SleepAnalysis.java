/*  Copyright (C) 2019-2020 Q-er

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class SleepAnalysis {

    public static final long MIN_SESSION_LENGTH = 5 * 60;
    public static final long MAX_WAKE_PHASE_LENGTH = 2 * 60 * 60;

    public List<SleepSession> calculateSleepSessions(List<? extends ActivitySample> samples) {
        List<SleepSession> result = new ArrayList<>();

        ActivitySample previousSample = null;
        Date sleepStart = null;
        Date sleepEnd = null;
        long lightSleepDuration = 0;
        long deepSleepDuration = 0;
        long durationSinceLastSleep = 0;

        for (ActivitySample sample : samples) {
            if (isSleep(sample)) {
                if (sleepStart == null)
                    sleepStart = getDateFromSample(sample);
                sleepEnd = getDateFromSample(sample);

                durationSinceLastSleep = 0;
            }

            if (previousSample != null) {
                long durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();
                if (sample.getKind() == ActivityKind.TYPE_LIGHT_SLEEP) {
                    lightSleepDuration += durationSinceLastSample;
                } else if (sample.getKind() == ActivityKind.TYPE_DEEP_SLEEP) {
                    deepSleepDuration += durationSinceLastSample;
                } else {
                    durationSinceLastSleep += durationSinceLastSample;
                    if (sleepStart != null && durationSinceLastSleep > MAX_WAKE_PHASE_LENGTH) {
                        if (lightSleepDuration + deepSleepDuration > MIN_SESSION_LENGTH)
                            result.add(new SleepSession(sleepStart, sleepEnd, lightSleepDuration, deepSleepDuration));
                        sleepStart = null;
                        sleepEnd = null;
                        lightSleepDuration = 0;
                        deepSleepDuration = 0;
                    }
                }
            }

            previousSample = sample;
        }
        if (lightSleepDuration + deepSleepDuration > MIN_SESSION_LENGTH) {
            result.add(new SleepSession(sleepStart, sleepEnd, lightSleepDuration, deepSleepDuration));
        }
        return result;
    }

    private boolean isSleep(ActivitySample sample) {
        return sample.getKind() == ActivityKind.TYPE_DEEP_SLEEP || sample.getKind() == ActivityKind.TYPE_LIGHT_SLEEP;
    }

    private Date getDateFromSample(ActivitySample sample) {
        return new Date(sample.getTimestamp() * 1000L);
    }


    public static class SleepSession {
        private final Date sleepStart;
        private final Date sleepEnd;
        private final long lightSleepDuration;
        private final long deepSleepDuration;

        private SleepSession(Date sleepStart,
                             Date sleepEnd,
                             long lightSleepDuration,
                             long deepSleepDuration) {
            this.sleepStart = sleepStart;
            this.sleepEnd = sleepEnd;
            this.lightSleepDuration = lightSleepDuration;
            this.deepSleepDuration = deepSleepDuration;
        }

        public Date getSleepStart() {
            return sleepStart;
        }

        public Date getSleepEnd() {
            return sleepEnd;
        }

        public long getLightSleepDuration() {
            return lightSleepDuration;
        }

        public long getDeepSleepDuration() {
            return deepSleepDuration;
        }
    }
}
