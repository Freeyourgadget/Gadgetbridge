/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import android.util.SparseIntArray;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveHrSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.VivomoveHrActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class FitImporter {
    private static final int ACTIVITY_TYPE_ALL = -1;
    private final SortedMap<Integer, List<FitEvent>> eventsPerTimestamp = new TreeMap<>();

    public void importFitData(List<FitMessage> messages) {
        boolean ohrEnabled = false;
        int softwareVersion = -1;

        int lastTimestamp = 0;
        final SparseIntArray lastCycles = new SparseIntArray();

        for (FitMessage message : messages) {
            switch (message.definition.globalMessageID) {
                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_EVENT:
                    //message.getField();
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_SOFTWARE:
                    final Integer versionField = message.getIntegerField("version");
                    if (versionField != null) softwareVersion = versionField;
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_MONITORING_INFO:
                    lastTimestamp = message.getIntegerField("timestamp");
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_MONITORING:
                    lastTimestamp = processMonitoringMessage(message, ohrEnabled, lastTimestamp, lastCycles);
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_OHR_SETTINGS:
                    final Boolean isOhrEnabled = message.getBooleanField("enabled");
                    if (isOhrEnabled != null) ohrEnabled = isOhrEnabled;
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_SLEEP_LEVEL:
                    processSleepLevelMessage(message);
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_MONITORING_HR_DATA:
                    processHrDataMessage(message);
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_STRESS_LEVEL:
                    processStressLevelMessage(message);
                    break;

                case FitMessageDefinitions.FIT_MESSAGE_NUMBER_MAX_MET_DATA:
                    processMaxMetDataMessage(message);
                    break;
            }
        }
    }

    public void processImportedData(FitImportProcessor processor) {
        for (final Map.Entry<Integer, List<FitEvent>> eventsForTimestamp : eventsPerTimestamp.entrySet()) {
            final VivomoveHrActivitySample sample = new VivomoveHrActivitySample();
            sample.setTimestamp(eventsForTimestamp.getKey());

            sample.setRawKind(ActivitySample.NOT_MEASURED);
            sample.setCaloriesBurnt(ActivitySample.NOT_MEASURED);
            sample.setSteps(ActivitySample.NOT_MEASURED);
            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setFloorsClimbed(ActivitySample.NOT_MEASURED);
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);

            FitEvent.EventKind bestKind = FitEvent.EventKind.UNKNOWN;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (final FitEvent event : eventsForTimestamp.getValue()) {
                if (event.getHeartRate() > sample.getHeartRate()) {
                    sample.setHeartRate(event.getHeartRate());
                }
                if (event.getFloorsClimbed() > sample.getFloorsClimbed()) {
                    sample.setFloorsClimbed(event.getFloorsClimbed());
                }

                float score = 0;
                if (event.getRawKind() > 0) score += 1;
                if (event.getCaloriesBurnt() > 0) score += event.getCaloriesBurnt() * 10.0f;
                if (event.getSteps() > 0) score += event.getSteps();
                //if (event.getRawIntensity() > 0) score += 10.0f * event.getRawIntensity();
                if (event.getKind().isBetterThan(bestKind) || (event.getKind() == bestKind && score > bestScore)) {
//                    if (bestScore > Float.NEGATIVE_INFINITY && event.getKind() != FitEvent.EventKind.NOT_WORN) {
//                        System.out.println(String.format(Locale.ROOT, "Replacing %s %d (%d cal, %d steps) with %s %d (%d cal, %d steps)", sample.getRawKind(), sample.getRawIntensity(), sample.getCaloriesBurnt(), sample.getSteps(), event.getRawKind(), event.getRawIntensity(), event.getCaloriesBurnt(), event.getSteps()));
//                    }
                    bestScore = score;
                    bestKind = event.getKind();
                    sample.setRawKind(event.getRawKind());
                    sample.setCaloriesBurnt(event.getCaloriesBurnt());
                    sample.setSteps(event.getSteps());
                    sample.setRawIntensity(event.getRawIntensity());
                }
            }

            if (sample.getHeartRate() == ActivitySample.NOT_MEASURED && ((sample.getRawKind() & VivomoveHrSampleProvider.RAW_TYPE_KIND_SLEEP) != 0)) {
                sample.setRawKind(VivomoveHrSampleProvider.RAW_NOT_WORN);
                sample.setRawIntensity(0);
            }

            processor.onSample(sample);
        }
    }

    private void processSleepLevelMessage(FitMessage message) {
        final Integer timestampFull = message.getIntegerField("timestamp");
        final Integer sleepLevel = message.getIntegerField("sleep_level");

        final int timestamp = GarminTimeUtils.garminTimestampToUnixTime(timestampFull);
        final int rawIntensity = (4 - sleepLevel) * 40;
        final int rawKind = VivomoveHrSampleProvider.RAW_TYPE_KIND_SLEEP | sleepLevel;

        addEvent(new FitEvent(timestamp, FitEvent.EventKind.SLEEP, rawKind, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, rawIntensity));
    }

    private int processMonitoringMessage(FitMessage message, boolean ohrEnabled, int lastTimestamp, SparseIntArray lastCycles) {
        final Integer activityType = message.getIntegerField("activity_type");
        final Double activeCalories = message.getNumericField("active_calories");
        final Integer intensity = message.getIntegerField("current_activity_type_intensity");
        final Integer cycles = message.getIntegerField("cycles");
        final Double heartRateMeasured = message.getNumericField("heart_rate");
        final Integer timestampFull = message.getIntegerField("timestamp");
        final Integer timestamp16 = message.getIntegerField("timestamp_16");
        final Double activeTime = message.getNumericField("active_time");

        final int activityTypeOrAll = activityType == null ? ACTIVITY_TYPE_ALL : activityType;
        final int activityTypeOrDefault = activityType == null ? 0 : activityType;

        final int lastDefaultCycleCount = lastCycles.get(ACTIVITY_TYPE_ALL);
        final int lastCycleCount = Math.max(lastCycles.get(activityTypeOrAll), lastDefaultCycleCount);
        final Integer currentCycles = cycles == null ? null : cycles < lastCycleCount ? cycles : cycles - lastCycleCount;
        if (currentCycles != null) {
            lastCycles.put(activityTypeOrDefault, cycles);
            final int newAllCycles = Math.max(lastDefaultCycleCount, cycles);
            if (newAllCycles != lastDefaultCycleCount) {
                assert newAllCycles > lastDefaultCycleCount;
                lastCycles.put(ACTIVITY_TYPE_ALL, newAllCycles);
            }
        }

        if (timestampFull != null) {
            lastTimestamp = timestampFull;
        } else if (timestamp16 != null) {
            lastTimestamp += (timestamp16 - (lastTimestamp & 0xFFFF)) & 0xFFFF;
        } else {
            // TODO: timestamp_min_8
            throw new IllegalArgumentException("Unsupported timestamp");
        }

        final int timestamp = GarminTimeUtils.garminTimestampToUnixTime(lastTimestamp);
        final int rawKind, caloriesBurnt, floorsClimbed, heartRate, steps, rawIntensity;
        final FitEvent.EventKind eventKind;

        caloriesBurnt = activeCalories == null ? ActivitySample.NOT_MEASURED : (int) Math.round(activeCalories);
        floorsClimbed = ActivitySample.NOT_MEASURED;
        heartRate = ohrEnabled && heartRateMeasured != null && heartRateMeasured > 0 ? (int) Math.round(heartRateMeasured) : ActivitySample.NOT_MEASURED;
        steps = currentCycles == null ? ActivitySample.NOT_MEASURED : currentCycles;
        rawIntensity = intensity == null ? 0 : intensity;
        rawKind = VivomoveHrSampleProvider.RAW_TYPE_KIND_ACTIVITY | activityTypeOrDefault;
        eventKind = steps != ActivitySample.NOT_MEASURED || rawIntensity > 0 || activityTypeOrDefault > 0 ? FitEvent.EventKind.ACTIVITY : FitEvent.EventKind.WORN;

        if (rawKind != ActivitySample.NOT_MEASURED
                || caloriesBurnt != ActivitySample.NOT_MEASURED
                || floorsClimbed != ActivitySample.NOT_MEASURED
                || heartRate != ActivitySample.NOT_MEASURED
                || steps != ActivitySample.NOT_MEASURED
                || rawIntensity != ActivitySample.NOT_MEASURED) {

            addEvent(new FitEvent(timestamp, eventKind, rawKind, caloriesBurnt, floorsClimbed, heartRate, steps, rawIntensity));
        } else {
            addEvent(new FitEvent(timestamp, FitEvent.EventKind.NOT_WORN, VivomoveHrSampleProvider.RAW_NOT_WORN, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED, ActivitySample.NOT_MEASURED));
        }
        return lastTimestamp;
    }

    private void processHrDataMessage(FitMessage message) {
    }

    private void processStressLevelMessage(FitMessage message) {
    }

    private void processMaxMetDataMessage(FitMessage message) {
    }

    private void addEvent(FitEvent event) {
        List<FitEvent> eventsForTimestamp = eventsPerTimestamp.get(event.getTimestamp());
        if (eventsForTimestamp == null) {
            eventsForTimestamp = new ArrayList<>();
            eventsPerTimestamp.put(event.getTimestamp(), eventsForTimestamp);
        }
        eventsForTimestamp.add(event);
    }

    private static class FitEvent {
        private final int timestamp;
        private final EventKind kind;
        private final int rawKind;
        private final int caloriesBurnt;
        private final int floorsClimbed;
        private final int heartRate;
        private final int steps;
        private final int rawIntensity;

        private FitEvent(int timestamp, EventKind kind, int rawKind, int caloriesBurnt, int floorsClimbed, int heartRate, int steps, int rawIntensity) {
            this.timestamp = timestamp;
            this.kind = kind;
            this.rawKind = rawKind;
            this.caloriesBurnt = caloriesBurnt;
            this.floorsClimbed = floorsClimbed;
            this.heartRate = heartRate;
            this.steps = steps;
            this.rawIntensity = rawIntensity;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public EventKind getKind() {
            return kind;
        }

        public int getRawKind() {
            return rawKind;
        }

        public int getCaloriesBurnt() {
            return caloriesBurnt;
        }

        public int getFloorsClimbed() {
            return floorsClimbed;
        }

        public int getHeartRate() {
            return heartRate;
        }

        public int getSteps() {
            return steps;
        }

        public int getRawIntensity() {
            return rawIntensity;
        }

        public enum EventKind {
            UNKNOWN,
            NOT_WORN,
            WORN,
            SLEEP,
            ACTIVITY;

            public boolean isBetterThan(EventKind other) {
                return ordinal() > other.ordinal();
            }
        }
    }
}
