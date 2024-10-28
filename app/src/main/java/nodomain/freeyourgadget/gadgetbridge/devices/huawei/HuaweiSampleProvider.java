/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class HuaweiSampleProvider extends AbstractSampleProvider<HuaweiActivitySample> {
    /*
     * We save all data by saving a marker at the begin and end. We do not actively use these for
     * showing the data at the moment, but the samples are still saved as such, to keep the table
     * entries consistent.
     * Meaning of fields that are not self-explanatory:
     *  - `otherTimestamp`
     *    The timestamp of the other marker, if it's larger this is the begin, otherwise the end
     *  - `source`
     *    The source of the data, which Huawei Band message the data came from
     *    0x0d for sleep data from activity, 0x0a for TruSleep data
     */

    private static class RawTypes {
        public static final int NOT_MEASURED = -1;

        public static final int UNKNOWN = 1;

        public static final int DEEP_SLEEP = 0x07;
        public static final int LIGHT_SLEEP = 0x06;
    }

    public HuaweiSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        switch (rawType) {
            case RawTypes.DEEP_SLEEP:
                return ActivityKind.DEEP_SLEEP;
            case RawTypes.LIGHT_SLEEP:
                return ActivityKind.LIGHT_SLEEP;
            default:
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case DEEP_SLEEP:
                return RawTypes.DEEP_SLEEP;
            case LIGHT_SLEEP:
                return RawTypes.LIGHT_SLEEP;
            default:
                return RawTypes.NOT_MEASURED;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public AbstractDao<HuaweiActivitySample, ?> getSampleDao() {
        return getSession().getHuaweiActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return HuaweiActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuaweiActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuaweiActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public HuaweiActivitySample createActivitySample() {
        return new HuaweiActivitySample();
    }

    private int getLastFetchTimestamp(QueryBuilder<HuaweiActivitySample> qb) {
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return 0;
        Property deviceProperty = HuaweiActivitySampleDao.Properties.DeviceId;
        Property timestampProperty = HuaweiActivitySampleDao.Properties.Timestamp;

        qb.where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty)
                .limit(1);

        List<HuaweiActivitySample> samples = qb.build().list();
        if (samples.isEmpty())
            return 0;

        HuaweiActivitySample sample = samples.get(0);
        return sample.getTimestamp();
    }

    /**
     * Gets last timestamp where the sleep data has been fully synchronized
     * @return Last fully synchronized timestamp for sleep data
     */
    public int getLastSleepFetchTimestamp() {
        QueryBuilder<HuaweiActivitySample> qb = getSampleDao().queryBuilder();
        Property sourceProperty = HuaweiActivitySampleDao.Properties.Source;
        Property activityTypeProperty = HuaweiActivitySampleDao.Properties.RawKind;

        qb.where(
                qb.or(
                        sourceProperty.eq(0x0d),
                        sourceProperty.eq(0x0a)
                ),
                qb.or(
                        activityTypeProperty.eq(RawTypes.LIGHT_SLEEP),
                        activityTypeProperty.eq(RawTypes.DEEP_SLEEP)
                )
        );

        return getLastFetchTimestamp(qb);
    }

    /**
     * Gets last timestamp where the step data has been fully synchronized
     * @return Last fully synchronized timestamp for step data
     */
    public int getLastStepFetchTimestamp() {
        QueryBuilder<HuaweiActivitySample> qb = getSampleDao().queryBuilder();
        Property sourceProperty = HuaweiActivitySampleDao.Properties.Source;

        qb.where(sourceProperty.eq(0x0b));

        return getLastFetchTimestamp(qb);
    }

    /**
     * Makes a copy of a sample
     * @param sample The sample to copy
     * @return The copy of the sample
     */
    private HuaweiActivitySample copySample(HuaweiActivitySample sample) {
        HuaweiActivitySample sampleCopy = new HuaweiActivitySample(
                sample.getTimestamp(),
                sample.getDeviceId(),
                sample.getUserId(),
                sample.getOtherTimestamp(),
                sample.getSource(),
                sample.getRawKind(),
                sample.getRawIntensity(),
                sample.getSteps(),
                sample.getCalories(),
                sample.getDistance(),
                sample.getSpo(),
                sample.getHeartRate()
        );
        sampleCopy.setProvider(sample.getProvider());
        return sampleCopy;
    }

    @Override
    public void addGBActivitySample(HuaweiActivitySample activitySample) {
        HuaweiActivitySample start = copySample(activitySample);
        HuaweiActivitySample end = copySample(activitySample);
        end.setTimestamp(start.getOtherTimestamp());
        end.setSteps(ActivitySample.NOT_MEASURED);
        end.setCalories(ActivitySample.NOT_MEASURED);
        end.setDistance(ActivitySample.NOT_MEASURED);
        end.setSpo(ActivitySample.NOT_MEASURED);
        end.setHeartRate(ActivitySample.NOT_MEASURED);
        end.setOtherTimestamp(start.getTimestamp());

        getSampleDao().insertOrReplace(start);
        getSampleDao().insertOrReplace(end);
    }

    @Override
    public void addGBActivitySamples(HuaweiActivitySample[] activitySamples) {
        List<HuaweiActivitySample> newSamples = new ArrayList<>();
        for (HuaweiActivitySample sample : activitySamples) {
            HuaweiActivitySample start = copySample(sample);
            HuaweiActivitySample end = copySample(sample);
            end.setTimestamp(start.getOtherTimestamp());
            end.setSteps(ActivitySample.NOT_MEASURED);
            end.setCalories(ActivitySample.NOT_MEASURED);
            end.setDistance(ActivitySample.NOT_MEASURED);
            end.setSpo(ActivitySample.NOT_MEASURED);
            end.setHeartRate(ActivitySample.NOT_MEASURED);
            end.setOtherTimestamp(start.getTimestamp());

            newSamples.add(start);
            newSamples.add(end);
        }
        getSampleDao().insertOrReplaceInTx(newSamples);
    }

    /**
     * Gets the activity samples, ordered by timestamp
     * @param timestampFrom Start timestamp
     * @param timestampTo End timestamp
     * @return List of activities between the timestamps, ordered by timestamp
     */
    private List<HuaweiActivitySample> getRawOrderedActivitySamples(int timestampFrom, int timestampTo) {
        QueryBuilder<HuaweiActivitySample> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestampFrom))
                .where(timestampProperty.le(timestampTo))
                .orderAsc(timestampProperty);
        List<HuaweiActivitySample> samples = qb.build().list();
        for (HuaweiActivitySample sample : samples) {
            sample.setProvider(this);
        }
        detachFromSession();
        return samples;
    }

    private List<HuaweiWorkoutDataSample> getRawOrderedWorkoutSamplesWithHeartRate(int timestampFrom, int timestampTo) {
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return Collections.emptyList();

        QueryBuilder<HuaweiWorkoutDataSample> qb = getSession().getHuaweiWorkoutDataSampleDao().queryBuilder();
        Property timestampProperty = HuaweiWorkoutDataSampleDao.Properties.Timestamp;
        Property heartRateProperty = HuaweiWorkoutDataSampleDao.Properties.HeartRate;
        Property deviceProperty = HuaweiWorkoutSummarySampleDao.Properties.DeviceId;
        qb.join(HuaweiWorkoutDataSampleDao.Properties.WorkoutId, HuaweiWorkoutSummarySample.class, HuaweiWorkoutSummarySampleDao.Properties.WorkoutId)
                .where(deviceProperty.eq(dbDevice.getId()));
        qb.where(
                timestampProperty.ge(timestampFrom),
                timestampProperty.le(timestampTo),
                heartRateProperty.notEq(ActivitySample.NOT_MEASURED)
        ).orderAsc(timestampProperty);
        List<HuaweiWorkoutDataSample> samples = qb.build().list();
        getSession().getHuaweiWorkoutSummarySampleDao().detachAll();
        return samples;
    }

    /*
     * This takes the following three steps:
     *  - Generate a sample every minute
     *  - Add the activity sample data to the generated samples
     *  - Add the workout data to the generated samples
     */
    @Override
    protected List<HuaweiActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to) {
        List<HuaweiActivitySample> processedSamples = new ArrayList<>();

        for (int timestamp = timestamp_from; timestamp <= timestamp_to; timestamp += 60) {
            processedSamples.add(createDummySample(timestamp));
        }

        overlayActivitySamples(processedSamples, timestamp_from, timestamp_to);
        overlayWorkoutSamples(processedSamples, timestamp_from, timestamp_to);

        return processedSamples;
    }

    @Override
    protected List<HuaweiActivitySample> getGBActivitySamplesHighRes(int timestamp_from, int timestamp_to) {
        List<HuaweiActivitySample> processedSamples = getRawOrderedActivitySamples(timestamp_from, timestamp_to);
        addWorkoutSamples(processedSamples, timestamp_from, timestamp_to);
        // Filter out the end markers before returning
        return processedSamples.stream().filter(sample -> sample.getTimestamp() <= sample.getOtherTimestamp()).collect(Collectors.toList());
    }

    @Override
    public boolean hasHighResData() {
        return true;
    }

    private HuaweiActivitySample createDummySample(int timestamp) {
        HuaweiActivitySample activitySample = new HuaweiActivitySample(
                timestamp,
                -1,
                -1,
                timestamp + 60, // Make sure the duration is 60
                (byte) 0x00,
                ActivitySample.NOT_MEASURED,
                0,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED);
        activitySample.setProvider(this);
        return activitySample;
    }

    /*
     * For every activity sample, it adds the data into the following processed sample.
     * If there are multiple activity samples, the steps, calories, and distance is added together.
     * For the SpO and HR only the last value is used.
     */
    private void overlayActivitySamples(List<HuaweiActivitySample> processedSamples, int timestamp_from, int timestamp_to) {
        List<HuaweiActivitySample> activitySamples = getRawOrderedActivitySamples(timestamp_from, timestamp_to);

        int currentIndex = 0;

        boolean hasData = false;

        int stepCount = ActivitySample.NOT_MEASURED;
        int calorieCount = ActivitySample.NOT_MEASURED;
        int distanceCount = ActivitySample.NOT_MEASURED;

        int lastSpo = ActivitySample.NOT_MEASURED;
        int lastHr = ActivitySample.NOT_MEASURED;

        int stateModifier = ActivitySample.NOT_MEASURED;

        for (HuaweiActivitySample activitySample : activitySamples) {
            // Ignore the end markers
            if (activitySample.getTimestamp() > activitySample.getOtherTimestamp())
                continue;

            // Skip the processed samples that are before this activity sample
            while (activitySample.getTimestamp() > processedSamples.get(currentIndex).getTimestamp()) {
                // Add data to current index sample
                if (hasData || stateModifier != ActivitySample.NOT_MEASURED)
                    processedSamples.get(currentIndex).setRawIntensity(1);
                processedSamples.get(currentIndex).setSteps(stepCount);
                processedSamples.get(currentIndex).setCalories(calorieCount);
                processedSamples.get(currentIndex).setDistance(distanceCount);
                processedSamples.get(currentIndex).setSpo(lastSpo);
                processedSamples.get(currentIndex).setHeartRate(lastHr);
                processedSamples.get(currentIndex).setRawKind(stateModifier);

                // Reset counters
                hasData = false;
                stepCount = ActivitySample.NOT_MEASURED;
                calorieCount = ActivitySample.NOT_MEASURED;
                distanceCount = ActivitySample.NOT_MEASURED;
                lastSpo = ActivitySample.NOT_MEASURED;
                lastHr = ActivitySample.NOT_MEASURED;

                currentIndex += 1;
                if (currentIndex >= processedSamples.size())
                    return; // We cannot add the data to any samples, so we might as well return
            }

            // Update data
            if (activitySample.getSteps() != ActivitySample.NOT_MEASURED) {
                if (stepCount == ActivitySample.NOT_MEASURED)
                    stepCount = 0;
                stepCount += activitySample.getSteps();
                hasData = true;
            }
            if (activitySample.getCalories() != ActivitySample.NOT_MEASURED) {
                if (calorieCount == ActivitySample.NOT_MEASURED)
                    calorieCount = 0;
                calorieCount += activitySample.getCalories();
                hasData = true;
            }
            if (activitySample.getDistance() != ActivitySample.NOT_MEASURED) {
                if (distanceCount == ActivitySample.NOT_MEASURED)
                    distanceCount = 0;
                distanceCount += activitySample.getDistance();
                hasData = true;
            }
            if (activitySample.getSpo() != ActivitySample.NOT_MEASURED) {
                lastSpo = activitySample.getSpo();
                hasData = true;
            }
            if (activitySample.getHeartRate() != ActivitySample.NOT_MEASURED) {
                lastHr = activitySample.getHeartRate();
                hasData = true;
            }
            if (activitySample.getRawKind() != ActivitySample.NOT_MEASURED) {
                if (activitySample.getTimestamp() < activitySample.getOtherTimestamp()) {
                    // Starting of modifier
                    stateModifier = activitySample.getRawKind();
                } else {
                    // End of modifier, remove it if it was for the same state
                    if (activitySample.getRawKind() == stateModifier)
                        stateModifier = ActivitySample.NOT_MEASURED;
                }
            }
        }

        // If there is still data, it has to be part of the next index of processed samples
        currentIndex += 1;
        if (currentIndex >= processedSamples.size())
            return;
        if (hasData || stateModifier != ActivitySample.NOT_MEASURED)
            processedSamples.get(currentIndex).setRawIntensity(10);
        processedSamples.get(currentIndex).setSteps(stepCount);
        processedSamples.get(currentIndex).setCalories(calorieCount);
        processedSamples.get(currentIndex).setDistance(distanceCount);
        processedSamples.get(currentIndex).setSpo(lastSpo);
        processedSamples.get(currentIndex).setHeartRate(lastHr);
        processedSamples.get(currentIndex).setRawKind(stateModifier);
    }

    /*
     * For every workout sample, it adds the data into the following processed sample.
     * It also detects if it is still in the same workout, and resets the HR and intensity for the
     * samples in between, see #4126 for the reasoning.
     * NOTE: Huawei devices tend to generate a lot more data - mine up to every 5 seconds. Most of
     * this is lost in the conversion to data by the minute. It only shows the most recent value.
     */
    private void overlayWorkoutSamples(List<HuaweiActivitySample> processedSamples, int timestamp_from, int timestamp_to) {
        int currentIndex = 0;

        int lastHr = ActivitySample.NOT_MEASURED;

        List<HuaweiWorkoutDataSample> workoutSamples = getRawOrderedWorkoutSamplesWithHeartRate(timestamp_from, timestamp_to);

        for (int i = 0; i < workoutSamples.size(); i++) {
            // Look behind to see if this is still the same workout
            boolean inWorkout = i != 0 && workoutSamples.get(i).getWorkoutId() == workoutSamples.get(i - 1).getWorkoutId();

            // Skip the processed sample that are before this workout sample
            while (workoutSamples.get(i).getTimestamp() > processedSamples.get(currentIndex).getTimestamp()) {
                if (inWorkout) {
                    processedSamples.get(currentIndex).setHeartRate(lastHr);
                    processedSamples.get(currentIndex).setRawIntensity(0);
                }

                // Reset
                lastHr = ActivitySample.NOT_MEASURED;

                currentIndex += 1;
                if (currentIndex >= processedSamples.size())
                    return; // We cannot add the data to any samples, so we might as well return
            }

            if (workoutSamples.get(i).getHeartRate() != ActivitySample.NOT_MEASURED)
                lastHr = workoutSamples.get(i).getHeartRate() & 0xFF;
        }

        // If there is still data, it has to be part of the next index of processed samples
        // Data being present implies it's still in a workout
        currentIndex += 1;
        if (currentIndex >= processedSamples.size())
            return;
        if (lastHr != ActivitySample.NOT_MEASURED) {
            processedSamples.get(currentIndex).setHeartRate(lastHr);
            processedSamples.get(currentIndex).setRawIntensity(0);
        }
    }

    private void addWorkoutSamples(List<HuaweiActivitySample> processedSamples, int timestamp_from, int timestamp_to) {
        int currentIndex = 0;
        List<HuaweiWorkoutDataSample> workoutSamples = getRawOrderedWorkoutSamplesWithHeartRate(timestamp_from, timestamp_to);

        for (int i = 0; i < workoutSamples.size(); i++) {
            // Look behind to see if this is still the same workout
            boolean inWorkout = i != 0 && workoutSamples.get(i).getWorkoutId() == workoutSamples.get(i - 1).getWorkoutId();

            // Skip the samples that are before this workout sample, and potentially clear the HR
            // and intensity - see #4126 for the reasoning
            while (currentIndex < processedSamples.size() && workoutSamples.get(i).getTimestamp() > processedSamples.get(currentIndex).getTimestamp()) {
                if (inWorkout) {
                    processedSamples.get(currentIndex).setHeartRate(ActivitySample.NOT_MEASURED);
                    processedSamples.get(currentIndex).setRawIntensity(0);
                }

                currentIndex += 1;
            }

            if (i < workoutSamples.size() - 1) {
                processedSamples.add(currentIndex, convertWorkoutSampleToActivitySample(workoutSamples.get(i), workoutSamples.get(i + 1).getTimestamp()));
            } else {
                // For the last workout sample we assume it is over 5 seconds
                processedSamples.add(currentIndex, convertWorkoutSampleToActivitySample(workoutSamples.get(i), workoutSamples.get(i).getTimestamp() + 5));
            }
            currentIndex += 1; // Prevent clearing the sample in the next loop
        }
    }

    private HuaweiActivitySample convertWorkoutSampleToActivitySample(HuaweiWorkoutDataSample workoutSample, int nextTimestamp) {
        int hr = workoutSample.getHeartRate() & 0xFF;
        HuaweiActivitySample newSample = new HuaweiActivitySample(
                workoutSample.getTimestamp(),
                -1,
                -1,
                nextTimestamp - 1, // Just to prevent overlap causing issues
                (byte) 0x00,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                ActivitySample.NOT_MEASURED,
                hr
        );
        newSample.setProvider(this);
        return newSample;
    }
}
