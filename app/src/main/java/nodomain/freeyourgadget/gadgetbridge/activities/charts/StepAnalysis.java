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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class StepAnalysis {
    protected static final Logger LOG = LoggerFactory.getLogger(StepAnalysis.class);
    private static final long MIN_SESSION_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_min_session_length", 10);
    private static final long MAX_IDLE_PHASE_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_max_idle_phase_length", 5);
    private static final long MIN_STEPS_PER_MINUTE = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute", 80);
    private static final long MIN_STEPS_PER_MINUTE_FOR_RUN = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute_for_run", 120);

    public List<StepSession> calculateStepSessions(List<? extends ActivitySample> samples) {
        List<StepSession> result = new ArrayList<>();

        ActivitySample previousSample = null;
        Date stepStart = null;
        Date stepEnd = null;
        int activeSteps = 0;
        int stepsBetwenActivities = 0;
        long durationSinceLastActiveStep = 0;
        int activityKind;

        for (ActivitySample sample : samples) {
            if (isStep(sample)) { //TODO    we could improve/extend this to other activities as well, if in database
                if (stepStart == null) {
                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE) { //active step
                        stepStart = getDateFromSample(sample);
                        activeSteps = sample.getSteps();
                        durationSinceLastActiveStep = 0;
                        stepsBetwenActivities = 0;
                    }
                }
                if (previousSample != null) {
                    long durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();

                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE) {
                        activeSteps += sample.getSteps() + stepsBetwenActivities;
                        stepsBetwenActivities = 0;
                        durationSinceLastActiveStep = 0;
                    } else {
                        stepsBetwenActivities += sample.getSteps();
                        durationSinceLastActiveStep += durationSinceLastSample;
                    }
                    if (stepStart != null && durationSinceLastActiveStep >= MAX_IDLE_PHASE_LENGTH) {
                        long current = getDateFromSample(sample).getTime() / 1000;
                        long ending = stepStart.getTime() / 1000;
                        long session_length = current - ending;

                        if (session_length > MIN_SESSION_LENGTH) {
                            stepEnd = getDateFromSample(sample);
                            activityKind = detect_activity_from_steps_per_minute(session_length, activeSteps);
                            result.add(new StepSession(stepStart, stepEnd, activeSteps, activityKind));
                            stepStart = null;
                        }
                    }
                }
                previousSample = sample;
            }
        }
        //make sure we save the last portion of the data as well
        if (stepStart != null && previousSample != null) {
            long current = getDateFromSample(previousSample).getTime() / 1000;
            long ending = stepStart.getTime() / 1000;
            long session_length = current - ending;

            if (session_length > MIN_SESSION_LENGTH) {
                stepEnd = getDateFromSample(previousSample);
                activityKind = detect_activity_from_steps_per_minute(session_length, activeSteps);
                result.add(new StepSession(stepStart, stepEnd, activeSteps, activityKind));
            }
        }
        return result;
    }

    private int detect_activity_from_steps_per_minute(long session_length, int activeSteps) {
        long spm = activeSteps / (session_length / 60);
        if (spm > MIN_STEPS_PER_MINUTE_FOR_RUN) {
            return ActivityKind.TYPE_RUNNING;
        }
        return ActivityKind.TYPE_WALKING;
    }

    private boolean isStep(ActivitySample sample) {
        return sample.getKind() == ActivityKind.TYPE_WALKING || sample.getKind() == ActivityKind.TYPE_RUNNING || sample.getKind() == ActivityKind.TYPE_ACTIVITY;
    }

    private Date getDateFromSample(ActivitySample sample) {
        return new Date(sample.getTimestamp() * 1000L);
    }

    public static class StepSession {
        private final Date stepStart;
        private final Date stepEnd;
        private final int steps;
        private final int activityKind;

        StepSession(Date stepStart,
                    Date stepEnd,
                    int steps, int activityKind) {
            this.stepStart = stepStart;
            this.stepEnd = stepEnd;
            this.steps = steps;
            this.activityKind = activityKind;
        }

        public Date getStepStart() {
            return stepStart;
        }

        public Date getStepEnd() {
            return stepEnd;
        }

        public long getSteps() {
            return steps;
        }

        public int getActivityKind() {
            return activityKind;
        }

    }
}
