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
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class StepAnalysis {
    protected static final Logger LOG = LoggerFactory.getLogger(StepAnalysis.class);

    public List<StepSession> calculateStepSessions(List<? extends ActivitySample> samples) {
        List<StepSession> result = new ArrayList<>();
        ActivityUser activityUser = new ActivityUser();
        double STEP_LENGTH_M;
        final int MIN_SESSION_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_min_session_length", 5);
        final int MAX_IDLE_PHASE_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_max_idle_phase_length", 5);
        final int MIN_STEPS_PER_MINUTE = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute", 40);
        int stepLengthCm = activityUser.getStepLengthCm();
        int heightCm = activityUser.getHeightCm();

        if (stepLengthCm == 0 && heightCm != 0) {
            STEP_LENGTH_M = heightCm * 0.43 * 0.01;
        } else {
            STEP_LENGTH_M = stepLengthCm * 0.01;
        }
        final double MIN_SESSION_INTENSITY = Math.max(0, Math.min(1, MIN_STEPS_PER_MINUTE * 0.01));

        ActivitySample previousSample = null;
        Date sessionStart = null;
        Date sessionEnd;
        int activeSteps = 0; //steps that we count
        int stepsBetweenActivePeriods = 0; //steps during time when we maybe take a rest but then restart
        int durationSinceLastActiveStep = 0;
        int activityKind;

        int heartRateForAverage = 0;
        int heartRateToAdd = 0;
        int heartRateBetweenActivePeriods = 0;
        int activeHrSamplesForAverage = 0;
        int activeHrSamplesToAdd = 0;

        float activeIntensity = 0;
        float intensityBetweenActivePeriods = 0;


        for (ActivitySample sample : samples) {
            if (sample.getKind() != ActivityKind.TYPE_SLEEP //anything but sleep counts
                    && !(sample instanceof TrailingActivitySample)) { //trailing samples have wrong date and make trailing activity have 0 duration
                if (sample.getHeartRate() != 255 && sample.getHeartRate() != -1) {
                    heartRateToAdd = sample.getHeartRate();
                    activeHrSamplesToAdd = 1;
                } else {
                    heartRateToAdd = 0;
                    activeHrSamplesToAdd = 0;
                }

                if (sessionStart == null) {
                    sessionStart = getDateFromSample(sample);
                    activeSteps = sample.getSteps();
                    activeIntensity = sample.getIntensity();
                    heartRateForAverage = heartRateToAdd;
                    activeHrSamplesForAverage = activeHrSamplesToAdd;
                    durationSinceLastActiveStep = 0;
                    stepsBetweenActivePeriods = 0;
                    heartRateBetweenActivePeriods = 0;
                    previousSample = null;
                }
                if (previousSample != null) {
                    int durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();
                    activeHrSamplesForAverage += activeHrSamplesToAdd;
                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE || //either some steps
                            (sample.getIntensity() > MIN_SESSION_INTENSITY && sample.getSteps() > 0)) { //or some intensity plus at least one step
                        activeSteps += sample.getSteps() + stepsBetweenActivePeriods;
                        activeIntensity += sample.getIntensity() + intensityBetweenActivePeriods;
                        heartRateForAverage += heartRateToAdd + heartRateBetweenActivePeriods;
                        stepsBetweenActivePeriods = 0;
                        heartRateBetweenActivePeriods = 0;
                        intensityBetweenActivePeriods = 0;
                        durationSinceLastActiveStep = 0;

                    } else { //short break data to remember, we will add it to the rest later, if break not too long
                        stepsBetweenActivePeriods += sample.getSteps();
                        heartRateBetweenActivePeriods += heartRateToAdd;
                        durationSinceLastActiveStep += durationSinceLastSample;
                        intensityBetweenActivePeriods += sample.getIntensity();
                    }
                    if (durationSinceLastActiveStep >= MAX_IDLE_PHASE_LENGTH) { //break too long, we split here

                        int current = sample.getTimestamp();
                        int starting = (int) (sessionStart.getTime() / 1000);
                        int session_length = current - starting - durationSinceLastActiveStep;

                        if (session_length >= MIN_SESSION_LENGTH) { //valid activity session
                            int heartRateAverage = activeHrSamplesForAverage > 0 ? heartRateForAverage / activeHrSamplesForAverage : 0;
                            float distance = (float) (activeSteps * STEP_LENGTH_M);
                            sessionEnd = new Date((sample.getTimestamp() - durationSinceLastActiveStep) * 1000L);
                            activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                            result.add(new StepSession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind));
                        }
                        sessionStart = null;
                    }
                }
                previousSample = sample;
            }
        }
        //trailing activity: make sure we show the last portion of the data as well in case no further activity is recorded yet

        if (sessionStart != null && previousSample != null) {
            int current = previousSample.getTimestamp();
            int starting = (int) (sessionStart.getTime() / 1000);
            int session_length = current - starting - durationSinceLastActiveStep;

            if (session_length >= MIN_SESSION_LENGTH) {
                int heartRateAverage = activeHrSamplesForAverage > 0 ? heartRateForAverage / activeHrSamplesForAverage : 0;
                float distance = (float) (activeSteps * STEP_LENGTH_M);
                sessionEnd = getDateFromSample(previousSample);
                activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                result.add(new StepSession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind));
            }
        }
        return result;
    }

    private int detect_activity_kind(int session_length, int activeSteps, int heartRateAverage, float intensity) {
        final int MIN_STEPS_PER_MINUTE_FOR_RUN = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute_for_run", 120);
        int spm = (int) (activeSteps / (session_length / 60));
        if (spm > MIN_STEPS_PER_MINUTE_FOR_RUN) {
            return ActivityKind.TYPE_RUNNING;
        }
        if (activeSteps > 200) {
            return ActivityKind.TYPE_WALKING;
        }
        if (heartRateAverage > 90 && intensity > 15) { //needs tuning
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
        private final Date startTime;
        private final Date endTime;
        private final int steps;
        private final int heartRateAverage;
        private final float intensity;
        private final float distance;
        private final int activityKind;

        StepSession(Date startTime,
                    Date endTime,
                    int steps, int heartRateAverage, float intensity, float distance, int activityKind) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.steps = steps;
            this.heartRateAverage = heartRateAverage;
            this.intensity = intensity;
            this.distance = distance;
            this.activityKind = activityKind;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getEndTime() {
            return endTime;
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

        public float getIntensity() {
            return intensity;
        }

        public float getDistance() {
            return distance;
        }
    }
}
