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
    private final int MIN_SESSION_STEPS = 100;

    public List<StepSession> calculateStepSessions(List<? extends ActivitySample> samples) {
        List<StepSession> result = new ArrayList<>();
        final int MIN_SESSION_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_min_session_length", 5);
        final int MAX_IDLE_PHASE_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_max_idle_phase_length", 5);
        final int MIN_STEPS_PER_MINUTE = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute", 40);

        ActivitySample previousSample = null;
        Date stepStart = null;
        Date stepEnd = null;
        int activeSteps = 0;
        int heartRateForAverage = 0;
        int heartRateToAdd = 0;
        int activeSamplesForAverage = 0;
        int activeSamplesToAdd = 0;
        int stepsBetweenActivities = 0;
        int heartRateBetweenActivities = 0;
        int durationSinceLastActiveStep = 0;
        int activityKind;

        for (ActivitySample sample : samples) {
            if (isStep(sample)) { //TODO    we could improve/extend this to other activities as well, if in database

                if (sample.getHeartRate() != 255 && sample.getHeartRate() != -1) {
                    heartRateToAdd = sample.getHeartRate();
                    activeSamplesToAdd = 1;
                } else {
                    heartRateToAdd = 0;
                    activeSamplesToAdd = 0;
                }

                if (stepStart == null) {
                    stepStart = getDateFromSample(sample);
                    activeSteps = sample.getSteps();
                    heartRateForAverage = heartRateToAdd;
                    activeSamplesForAverage = activeSamplesToAdd;
                    durationSinceLastActiveStep = 0;
                    stepsBetweenActivities = 0;
                    heartRateBetweenActivities = 0;
                    previousSample = null;
                }
                if (previousSample != null) {
                    int durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();
                    activeSamplesForAverage += activeSamplesToAdd;
                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE) {
                        activeSteps += sample.getSteps() + stepsBetweenActivities;
                        heartRateForAverage += heartRateToAdd + heartRateBetweenActivities;
                        stepsBetweenActivities = 0;
                        heartRateBetweenActivities = 0;
                        durationSinceLastActiveStep = 0;
                    } else {
                        stepsBetweenActivities += sample.getSteps();
                        heartRateBetweenActivities += heartRateToAdd;
                        durationSinceLastActiveStep += durationSinceLastSample;
                    }
                    if (durationSinceLastActiveStep >= MAX_IDLE_PHASE_LENGTH) {

                        int current = sample.getTimestamp();
                        int starting = (int) (stepStart.getTime() / 1000);
                        int session_length = current - starting - durationSinceLastActiveStep;
                        int heartRateAverage = activeSamplesForAverage > 0 ? heartRateForAverage / activeSamplesForAverage : 0;

                        if (session_length >= MIN_SESSION_LENGTH) {
                            stepEnd = new Date((sample.getTimestamp() - durationSinceLastActiveStep) * 1000L);
                            activityKind = detect_activity(session_length, activeSteps, heartRateAverage);
                            result.add(new StepSession(stepStart, stepEnd, activeSteps, heartRateAverage, activityKind));
                        }
                        stepStart = null;
                    }
                }
                previousSample = sample;
            }
        }
        //make sure we show the last portion of the data as well in case no further activity is recorded yet
        if (stepStart != null && previousSample != null) {
            int current = previousSample.getTimestamp();
            int starting = (int) (stepStart.getTime() / 1000);
            int session_length = current - starting - durationSinceLastActiveStep;
            int heartRateAverage = activeSamplesForAverage > 0 ? heartRateForAverage / activeSamplesForAverage : 0;

            if (session_length > MIN_SESSION_LENGTH && activeSteps > MIN_SESSION_STEPS) {
                stepEnd = getDateFromSample(previousSample);
                activityKind = detect_activity(session_length, activeSteps, heartRateAverage);
                result.add(new StepSession(stepStart, stepEnd, activeSteps, heartRateAverage, activityKind));
            }
        }
        return result;
    }

    private int detect_activity(int session_length, int activeSteps, int heartRateAverage) {
        final int MIN_STEPS_PER_MINUTE_FOR_RUN = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute_for_run", 120);
        int spm = (int) (activeSteps / (session_length / 60));
        if (spm > MIN_STEPS_PER_MINUTE_FOR_RUN) {
            return ActivityKind.TYPE_RUNNING;
        }
        if (activeSteps > 200) {
            return ActivityKind.TYPE_WALKING;
        }
        if (heartRateAverage > 90) {
            return ActivityKind.TYPE_EXERCISE;
        }
        return ActivityKind.TYPE_ACTIVITY;
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
        private final int heartRateAverage;
        private final int activityKind;

        StepSession(Date stepStart,
                    Date stepEnd,
                    int steps, int heartRateAverage, int activityKind) {
            this.stepStart = stepStart;
            this.stepEnd = stepEnd;
            this.steps = steps;
            this.heartRateAverage = heartRateAverage;
            this.activityKind = activityKind;
        }

        public Date getStepStart() {
            return stepStart;
        }

        public Date getStepEnd() {
            return stepEnd;
        }

        public int getSteps() {
            return steps;
        }

        public int getHeartRateAverage() {
            return heartRateAverage;
        }

        public int getActivityKind() {
            return activityKind;
        }

    }
}
