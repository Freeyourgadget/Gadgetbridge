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
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public class StepAnalysis {
    protected static final Logger LOG = LoggerFactory.getLogger(StepAnalysis.class);
    private int totalDailySteps = 0;

    public List<ActivitySession> calculateStepSessions(List<? extends ActivitySample> samples) {
        LOG.debug("get all samples activitysessions: " + samples.toArray().length);
        List<ActivitySession> result = new ArrayList<>();
        ActivityUser activityUser = new ActivityUser();
        double STEP_LENGTH_M;
        final int MIN_SESSION_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_min_session_length", 5);
        final int MAX_IDLE_PHASE_LENGTH = 60 * GBApplication.getPrefs().getInt("chart_list_max_idle_phase_length", 5);
        final int MIN_STEPS_PER_MINUTE = GBApplication.getPrefs().getInt("chart_list_min_steps_per_minute", 40);
        int stepLengthCm = activityUser.getStepLengthCm();
        int heightCm = activityUser.getHeightCm();
        totalDailySteps = 0;

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

        List<Integer> heartRateSum = new ArrayList<>();
        List<Integer> heartRateBetweenActivePeriodsSum = new ArrayList<>();

        float activeIntensity = 0;
        float intensityBetweenActivePeriods = 0;
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        for (ActivitySample sample : samples) {
            int steps = sample.getSteps();
            if (steps > 0) {
                totalDailySteps += steps;
            }

            if (sample.getKind() != ActivityKind.TYPE_SLEEP //anything but sleep counts
                    && !(sample instanceof TrailingActivitySample)) { //trailing samples have wrong date and make trailing activity have 0 duration

                if (sessionStart == null) {
                    sessionStart = getDateFromSample(sample);
                    activeSteps = sample.getSteps();
                    activeIntensity = sample.getIntensity();
                    heartRateSum = new ArrayList<>();
                    if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                        heartRateSum.add(sample.getHeartRate());
                    }
                    durationSinceLastActiveStep = 0;
                    stepsBetweenActivePeriods = 0;
                    heartRateBetweenActivePeriodsSum = new ArrayList<>();
                    previousSample = null;
                }
                if (previousSample != null) {
                    int durationSinceLastSample = sample.getTimestamp() - previousSample.getTimestamp();

                    if (sample.getSteps() > MIN_STEPS_PER_MINUTE || //either some steps
                            (sample.getIntensity() > MIN_SESSION_INTENSITY && sample.getSteps() > 0)) { //or some intensity plus at least one step
                        activeSteps += sample.getSteps() + stepsBetweenActivePeriods;
                        activeIntensity += sample.getIntensity() + intensityBetweenActivePeriods;
                        if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                            heartRateSum.add(sample.getHeartRate());
                        }
                        heartRateSum.addAll(heartRateBetweenActivePeriodsSum);
                        heartRateBetweenActivePeriodsSum = new ArrayList<>();
                        stepsBetweenActivePeriods = 0;
                        intensityBetweenActivePeriods = 0;
                        durationSinceLastActiveStep = 0;

                    } else { //short break data to remember, we will add it to the rest later, if break not too long
                        stepsBetweenActivePeriods += sample.getSteps();
                        if (heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                            heartRateBetweenActivePeriodsSum.add(sample.getHeartRate());
                        }
                        durationSinceLastActiveStep += durationSinceLastSample;
                        intensityBetweenActivePeriods += sample.getIntensity();
                    }
                    if (durationSinceLastActiveStep >= MAX_IDLE_PHASE_LENGTH) { //break too long, we split here

                        int current = sample.getTimestamp();
                        int starting = (int) (sessionStart.getTime() / 1000);
                        int session_length = current - starting - durationSinceLastActiveStep;

                        if (session_length >= MIN_SESSION_LENGTH) { //valid activity session
                            int heartRateAverage = heartRateSum.toArray().length > 0 ? calculateSumOfInts(heartRateSum) / heartRateSum.toArray().length : 0;
                            float distance = (float) (activeSteps * STEP_LENGTH_M);
                            sessionEnd = new Date((sample.getTimestamp() - durationSinceLastActiveStep) * 1000L);
                            activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                            ActivitySession activitySession = new ActivitySession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind);
                            //activitySession.setSessionType(ActivitySession.SESSION_ONGOING);
                            result.add(activitySession);
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
                int heartRateAverage = heartRateSum.toArray().length > 0 ? calculateSumOfInts(heartRateSum) / heartRateSum.toArray().length : 0;
                float distance = (float) (activeSteps * STEP_LENGTH_M);
                sessionEnd = getDateFromSample(previousSample);
                activityKind = detect_activity_kind(session_length, activeSteps, heartRateAverage, activeIntensity);
                ActivitySession ongoingActivity = new ActivitySession(sessionStart, sessionEnd, activeSteps, heartRateAverage, activeIntensity, distance, activityKind);
                ongoingActivity.setSessionType(ActivitySession.SESSION_ONGOING);
                result.add(ongoingActivity);
            }
        }
        return result;
    }

    public ActivitySession calculateSummary(List<ActivitySession> sessions, boolean empty) {

        Date startTime = null;
        Date endTime = null;
        int stepsSum = 0;
        int heartRateAverage = 0;
        List<Integer> heartRateSum = new ArrayList<>();
        int distanceSum = 0;
        float intensitySum = 0;
        int sessionCount;
        long durationSum = 0;

        for (ActivitySession session : sessions) {
            startTime = session.getStartTime();
            endTime = session.getEndTime();
            durationSum += endTime.getTime() - startTime.getTime();
            stepsSum += session.getActiveSteps();
            distanceSum += session.getDistance();
            heartRateSum.add(session.getHeartRateAverage());
            intensitySum += session.getIntensity();
        }

        sessionCount = sessions.toArray().length;
        if (heartRateSum.toArray().length > 0) {
            heartRateAverage = calculateSumOfInts(heartRateSum) / heartRateSum.toArray().length;
        }
        startTime = new Date(0);
        endTime = new Date(durationSum);

        ActivitySession stepSessionSummary = new ActivitySession(startTime, endTime,
                stepsSum, heartRateAverage, intensitySum, distanceSum, 0);

        stepSessionSummary.setSessionCount(sessionCount);
        stepSessionSummary.setSessionType(ActivitySession.SESSION_SUMMARY);
        stepSessionSummary.setEmptySummary(empty);


        stepSessionSummary.setTotalDaySteps(totalDailySteps);
        return stepSessionSummary;
    }

    public ActivitySession getOngoingSessions(List<ActivitySession> sessions) {

        for (ActivitySession session : sessions) {
            if (session.getSessionType() == ActivitySession.SESSION_ONGOING) {
                return session;
            }
        }
        return null;
    }

    private int calculateSumOfInts(List<Integer> samples) {
        int result = 0;
        for (Integer sample : samples) {
            result += sample;
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

    private Date getDateFromSample(ActivitySample sample) {
        return new Date(sample.getTimestamp() * 1000L);
    }
}
