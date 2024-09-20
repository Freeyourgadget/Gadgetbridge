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

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;

public class HuaweiSampleProvider extends AbstractSampleProvider<HuaweiActivitySample> {
    /*
     * We save all data by saving a marker at the begin and end.
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

    private static class SampleLoopState {
        public long deviceId = 0;
        public long userId = 0;

        public int sleepModifier = 0;
    }

    /*
     * Note that this does a lot more than the normal implementation, as it takes care of everything
     * that is necessary for proper displaying of data.
     *
     * This essentially boils down to four things:
     *  - It adds in the workout heart rate data without activity data in between
     *  - It adds a sample with intensity zero before start markers (start of block)
     *  - It adds a sample with intensity zero after end markers (end of block)
     *  - It modifies some blocks so the sleep data gets handled correctly
     * The second and fourth are necessary for proper stats calculation, the third is mostly for
     * nicer graphs.
     *
     * Note that the data in the database isn't changed, as the samples are detached.
     */
    @Override
    protected List<HuaweiActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to) {
        // Note that the result of this function has to be sorted by timestamp!
        List<HuaweiActivitySample> rawSamples = getRawOrderedActivitySamples(timestamp_from, timestamp_to);
        List<HuaweiWorkoutDataSample> workoutSamples = getRawOrderedWorkoutSamplesWithHeartRate(timestamp_from, timestamp_to);

        List<int[]> workoutSpans = getWorkoutSpans(rawSamples, workoutSamples, 5);
        List<HuaweiActivitySample> processedSamples = new ArrayList<>();

        Iterator<HuaweiActivitySample> itRawSamples = rawSamples.iterator();
        Iterator<HuaweiWorkoutDataSample> itWorkoutSamples = workoutSamples.iterator();

        HuaweiActivitySample nextRawSample = null;
        if (itRawSamples.hasNext())
            nextRawSample = itRawSamples.next();
        HuaweiWorkoutDataSample nextWorkoutSample = null;
        if (itWorkoutSamples.hasNext())
            nextWorkoutSample = itWorkoutSamples.next();

        SampleLoopState state = new SampleLoopState();
        if (nextRawSample != null) {
            state.deviceId = nextRawSample.getDeviceId();
            state.userId = nextRawSample.getUserId();
        }

        while (nextRawSample != null || nextWorkoutSample != null) {
            if (nextRawSample == null || (nextWorkoutSample != null && nextWorkoutSample.getTimestamp() < nextRawSample.getTimestamp())) {
                processWorkoutSample(processedSamples, state, nextWorkoutSample);
                nextWorkoutSample = itWorkoutSamples.hasNext() ? itWorkoutSamples.next() : null;
            } else {
                boolean sampleInWorkout = isInWorkout(workoutSpans, nextRawSample.getTimestamp());
                if (sampleInWorkout) {
                    nextRawSample.setHeartRate(ActivitySample.NOT_MEASURED);
                    nextRawSample.setRawIntensity(0);
                }
                processRawSample(processedSamples, state, nextRawSample);
                nextRawSample = itRawSamples.hasNext() ? itRawSamples.next() : null;
            }
        }
        processedSamples = interpolate(processedSamples);

        return processedSamples;
    }

    /*
    * Calculates the timespans: [start, end] of workouts
    * Normal activities should not be processed when in middle of workout
    **/
    private List<int[]> getWorkoutSpans(List<HuaweiActivitySample> activity, List<HuaweiWorkoutDataSample> workout, int threshold) {
        List<int[]> validActivitySpans = new ArrayList<>();

        Iterator<HuaweiActivitySample> activityIterator = activity.iterator();
        Iterator<HuaweiWorkoutDataSample> workoutIterator = workout.iterator();

        HuaweiActivitySample currentActivity = activityIterator.hasNext() ? activityIterator.next() : null;
        HuaweiWorkoutDataSample currentWorkout = workoutIterator.hasNext() ? workoutIterator.next() : null;

        int consecutiveActivityCount = 0;
        Integer spanStart = null;

        int workoutEnd = 0;
        while (currentActivity != null || currentWorkout != null) {
            if (currentWorkout == null || (currentActivity != null && currentActivity.getTimestamp() < currentWorkout.getTimestamp())) {
                // handle activity
                if (spanStart != null) {
                    // We're in workout, check for activity interruption
                    consecutiveActivityCount++;
                    if (consecutiveActivityCount > threshold) {
                        // Enough activity samples to interrupt the workout
                        validActivitySpans.add(new int[]{spanStart, workoutEnd});
                        spanStart = null;
                        consecutiveActivityCount = 0;
                    }
                }
                currentActivity = activityIterator.hasNext() ? activityIterator.next() : null;
            } else {
                // handle workout
                if (spanStart == null) {
                    spanStart = currentWorkout.getTimestamp();
                }
                workoutEnd = currentWorkout.getTimestamp();
                consecutiveActivityCount = 0;
                currentWorkout = workoutIterator.hasNext() ? workoutIterator.next() : null;
            }
        }

        // If there's an open valid span at the end, close it
        if (spanStart != null) {
            validActivitySpans.add(new int[]{spanStart, workoutEnd});
        }

        return validActivitySpans;
    }

    private boolean isInWorkout(List<int[]> validSpans, int timestamp) {
        for (int[] span : validSpans) {
            if (timestamp > span[0] && timestamp < span[1]) {
                return true;
            }
        }
        return false;
    }

    private List<HuaweiActivitySample> interpolate(List<HuaweiActivitySample> processedSamples) {
        List<HuaweiActivitySample> retv = new ArrayList<>();

        if (processedSamples.isEmpty())
            return retv;

        HuaweiActivitySample lastSample = processedSamples.get(0);
        retv.add(lastSample);
        for (int i = 1; i < processedSamples.size() - 1; i++) {
            HuaweiActivitySample sample = processedSamples.get(i);

            int timediff = sample.getTimestamp() - lastSample.getTimestamp();
            if (timediff > 60) {
                if (lastSample.getRawKind() != -1 && sample.getRawKind() != lastSample.getRawKind()) {
                    HuaweiActivitySample postSample = new HuaweiActivitySample(
                            lastSample.getTimestamp() + 1,
                            lastSample.getDeviceId(),
                            lastSample.getUserId(),
                            0,
                            (byte) 0x00,
                            ActivitySample.NOT_MEASURED,
                            0,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED
                    );
                    postSample.setProvider(this);
                    retv.add(postSample);
                }

                if (sample.getRawKind() != -1 && sample.getRawKind() != lastSample.getRawKind()) {
                    HuaweiActivitySample preSample = new HuaweiActivitySample(
                            sample.getTimestamp() - 1,
                            sample.getDeviceId(),
                            sample.getUserId(),
                            0,
                            (byte) 0x00,
                            ActivitySample.NOT_MEASURED,
                            0,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED,
                            ActivitySample.NOT_MEASURED
                    );
                    preSample.setProvider(this);
                    retv.add(preSample);
                }
            }

            retv.add(sample);
            lastSample = sample;
        }

        if (lastSample.getRawKind() != -1) {
            HuaweiActivitySample postSample = new HuaweiActivitySample(
                    lastSample.getTimestamp() + 1,
                    lastSample.getDeviceId(),
                    lastSample.getUserId(),
                    0,
                    (byte) 0x00,
                    ActivitySample.NOT_MEASURED,
                    0,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED,
                    ActivitySample.NOT_MEASURED
            );
            postSample.setProvider(this);
            retv.add(postSample);
        }

        return retv;
    }

    private void processRawSample(List<HuaweiActivitySample> processedSamples, SampleLoopState state, HuaweiActivitySample sample) {
        // Filter on Source 0x0d, Type 0x01, until we know what it is and how we should handle them.
        // Just showing them currently has some issues.
        if (sample.getSource() == FitnessData.MessageData.sleepId && sample.getRawKind() == RawTypes.UNKNOWN)
            return;

        HuaweiActivitySample lastSample = null;

        boolean isStartMarker = sample.getTimestamp() < sample.getOtherTimestamp();

        // Handle preferences for wakeup status ignore - can fix some quirks on some devices
        if (sample.getRawKind() == 0x08) {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
            if (isStartMarker && prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_IGNORE_WAKEUP_STATUS_START, false))
                return;
            if (!isStartMarker && prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_IGNORE_WAKEUP_STATUS_END, false))
                return;
        }

        // Backdate the end marker by one - otherwise the interpolation fails
        if (sample.getTimestamp() > sample.getOtherTimestamp())
            sample.setTimestamp(sample.getTimestamp() - 1);

        if (!processedSamples.isEmpty())
            lastSample = processedSamples.get(processedSamples.size() - 1);
        if (lastSample != null && lastSample.getTimestamp() == sample.getTimestamp()) {
            // Merge the samples - only if there isn't any data yet, except the kind

            if (lastSample.getRawKind() == -1)
                lastSample.setRawKind(sample.getRawKind());
            // Do overwrite the kind if the new sample is a starting sample
            if (isStartMarker && sample.getRawKind() != -1) {
                lastSample.setRawKind(sample.getRawKind());
                lastSample.setOtherTimestamp(sample.getOtherTimestamp()); // Necessary for interpolation
            }

            if (lastSample.getRawIntensity() == -1)
                lastSample.setRawIntensity(sample.getRawIntensity());
            if (lastSample.getSteps() == -1)
                lastSample.setSteps(sample.getSteps());
            if (lastSample.getCalories() == -1)
                lastSample.setCalories(sample.getCalories());
            if (lastSample.getDistance() == -1)
                lastSample.setDistance(sample.getDistance());
            if (lastSample.getSpo() == -1)
                lastSample.setSpo(sample.getSpo());
            if (lastSample.getHeartRate() == -1)
                lastSample.setHeartRate(sample.getHeartRate());
            if (lastSample.getSource() != sample.getSource())
                lastSample.setSource((byte) 0x00);
        } else {
            if (state.sleepModifier != 0)
                sample.setRawKind(state.sleepModifier);
            processedSamples.add(sample);
        }

        if (
                (sample.getSource() == FitnessData.MessageData.sleepId || sample.getSource() == 0x0a) // Sleep sources
                        && (sample.getRawKind() == RawTypes.LIGHT_SLEEP || sample.getRawKind() == RawTypes.DEEP_SLEEP) // Sleep types
        ) {
            if (isStartMarker)
                state.sleepModifier = sample.getRawKind();
            else
                state.sleepModifier = 0;
        }
    }

    private void processWorkoutSample(List<HuaweiActivitySample> processedSamples, SampleLoopState state, HuaweiWorkoutDataSample workoutSample) {
        processRawSample(processedSamples, state, convertWorkoutSampleToActivitySample(workoutSample, state));
    }

    private HuaweiActivitySample convertWorkoutSampleToActivitySample(HuaweiWorkoutDataSample workoutSample, SampleLoopState state) {
        int hr = workoutSample.getHeartRate() & 0xFF;
        HuaweiActivitySample newSample = new HuaweiActivitySample(
                workoutSample.getTimestamp(),
                state.deviceId,
                state.userId,
                0,
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
