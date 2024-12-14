/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

/**
 * Base class for all sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 * @param <T> the sample type
 */
public abstract class AbstractSampleProvider<T extends AbstractActivitySample> implements SampleProvider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSampleProvider.class);

    private static final WhereCondition[] NO_CONDITIONS = new WhereCondition[0];
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractSampleProvider(GBDevice device, DaoSession session) {
        mDevice = device;
        mSession = session;
    }

    public GBDevice getDevice() {
        return mDevice;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @NonNull
    @Override
    public List<T> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to);
    }

    @NonNull
    @Override
    public List<T> getAllActivitySamplesHighRes(int timestamp_from, int timestamp_to) {
        return getGBActivitySamplesHighRes(timestamp_from, timestamp_to);
    }

    @Override
    public boolean hasHighResData() {
        return false;
    }

    @NonNull
    @Override
    @Deprecated // use getAllActivitySamples
    public List<T> getActivitySamples(int timestamp_from, int timestamp_to) {
        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, Collections.singleton(ActivityKind.ACTIVITY));
        }
    }

    @Override
    public void addGBActivitySample(T activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addGBActivitySamples(T[] activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    @Nullable
    @Override
    public T getLatestActivitySample() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderDesc(getTimestampSampleProperty()).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    @Nullable
    @Override
    public T getLatestActivitySample(final int until) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        Property timestampProperty = getTimestampSampleProperty();
        qb.where(timestampProperty.le(until))
                .where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(timestampProperty).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    @Nullable
    @Override
    public T getFirstActivitySample() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderAsc(getTimestampSampleProperty()).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        T sample = samples.get(0);
        sample.setProvider(this);
        return sample;
    }

    /**
     * Get the activity samples between two timestamps (inclusive). Exactly one every minute.
     * @param timestamp_from Start timestamp
     * @param timestamp_to End timestamp
     * @return Exactly one sample for every minute
     */
    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestamp_from))
            .where(timestampProperty.le(timestamp_to));
        List<T> samples = qb.build().list();
        for (T sample : samples) {
            sample.setProvider(this);
        }
        detachFromSession();
        return samples;
    }

    /**
     * Get the activity samples between two timestamps (inclusive).
     * Differs from {@link #getGBActivitySamples(int, int)} in that it supplies as many samples as
     * available.
     * It assumes {@link #getGBActivitySamples(int, int)} returns the highest resolution data unless
     * this is overwritten.
     * @param timestamp_from Start timestamp
     * @param timestamp_to End timestamp
     * @return All the samples between start and end timestamp (inclusive)
     */
    protected List<T> getGBActivitySamplesHighRes(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to);
    }

    /**
     * Detaches all samples of this type from the session. Changes to them may not be
     * written back to the database.
     * <p>
     * Subclasses should call this method after performing custom queries.
     */
    protected void detachFromSession() {
        getSampleDao().detachAll();
    }

    private List<T> getActivitySamplesByActivityFilter(int timestamp_from, int timestamp_to, Set<ActivityKind> activityFilter) {
        List<T> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<T> filteredSamples = new ArrayList<>();

        for (T sample : samples) {
            if (activityFilter.contains(sample.getKind())) {
                filteredSamples.add(sample);
            }
        }
        return filteredSamples;
    }

    public abstract AbstractDao<T,?> getSampleDao();

    @Nullable
    protected abstract Property getRawKindSampleProperty();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();

    public void convertCumulativeSteps(final List<T> samples, final Property stepsSampleProperty) {
        // Fix over-counting at the turn of day
        final T lastSample = getLastSampleWithStepsBefore(samples.get(0).getTimestamp(), stepsSampleProperty);
        if (lastSample != null && sameDay(lastSample, samples.get(0))) {
            if (samples.get(0).getSteps() > 0) {
                samples.get(0).setSteps(samples.get(0).getSteps() - lastSample.getSteps());
            }

            if (samples.get(0).getDistanceCm() > 0) {
                samples.get(0).setDistanceCm(samples.get(0).getDistanceCm() - lastSample.getDistanceCm());
            }

            if (samples.get(0).getActiveCalories() > 0) {
                samples.get(0).setActiveCalories(samples.get(0).getActiveCalories() - lastSample.getActiveCalories());
            }
        }

        // This slightly breaks activity recognition, because we don't have per-minute granularity...
        int prevSteps = samples.get(0).getSteps();
        int prevDistance = samples.get(0).getDistanceCm();
        int prevActiveCalories = samples.get(0).getActiveCalories();
        samples.get(0).setTimestamp((samples.get(0).getTimestamp() / 60) * 60);
        int bak;

        for (int i = 1; i < samples.size(); i++) {
            final T s1 = samples.get(i - 1);
            final T s2 = samples.get(i);
            s2.setTimestamp((s2.getTimestamp() / 60) * 60);

            if (!sameDay(s1, s2)) {
                // went past midnight - reset steps
                prevSteps = s2.getSteps() > 0 ? s2.getSteps() : 0;
                prevDistance = s2.getDistanceCm() > 0 ? s2.getDistanceCm() : 0;
                prevActiveCalories = s2.getActiveCalories() > 0 ? s2.getActiveCalories() : 0;
            } else {
                // New value for the current day - subtract the previous seen sample

                if (s2.getSteps() > 0) {
                    bak = s2.getSteps();
                    s2.setSteps(s2.getSteps() - prevSteps);
                    prevSteps = bak;
                }
                if (s2.getDistanceCm() > 0) {
                    bak = s2.getDistanceCm();
                    s2.setDistanceCm(s2.getDistanceCm() - prevDistance);
                    prevDistance = bak;
                }
                if (s2.getActiveCalories() > 0) {
                    bak = s2.getActiveCalories();
                    s2.setActiveCalories(s2.getActiveCalories() - prevActiveCalories);
                    prevActiveCalories = bak;
                }
            }
        }
    }

    @Nullable
    public T getLastSampleWithStepsBefore(final int timestampTo, final Property stepsSampleProperty) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final List<T> samples = getSampleDao().queryBuilder()
                .where(
                        getDeviceIdentifierSampleProperty().eq(dbDevice.getId()),
                        getTimestampSampleProperty().le(timestampTo),
                        stepsSampleProperty.gt(-1)
                ).orderDesc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    public boolean sameDay(final T s1, final T s2) {
        final Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(s1.getTimestamp() * 1000L - 1000L);
        final LocalDate d1 = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        cal.setTimeInMillis(s2.getTimestamp() * 1000L - 1000L);
        final LocalDate d2 = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        return d1.equals(d2);
    }

    protected List<T> fillGaps(final List<T> samples, final int timestamp_from, final int timestamp_to) {
        if (samples.isEmpty()) {
            return samples;
        }

        final long nanoStart = System.nanoTime();

        final List<T> ret = new LinkedList<>(samples);

        //ret.sort(Comparator.comparingLong(T::getTimestamp));

        final int firstTimestamp = ret.get(0).getTimestamp();
        if (firstTimestamp - timestamp_from > 60) {
            // Gap at the start
            for (int ts = timestamp_from; ts <= firstTimestamp + 60; ts += 60) {
                ret.add(0, createDummySample(ts));
            }
        }

        final int lastTimestamp = ret.get(ret.size() - 1).getTimestamp();
        // Do not generate fake samples into the future
        final long minTo = Math.min(timestamp_to, System.currentTimeMillis() / 1000L);
        if (minTo - lastTimestamp > 60) {
            // Gap at the end
            for (int ts = lastTimestamp + 60; ts <= minTo; ts += 60) {
                ret.add(createDummySample(ts));
            }
        }

        final ListIterator<T> it = ret.listIterator();
        T previousSample = it.next();

        while (it.hasNext()) {
            final T sample = it.next();
            if (sample.getTimestamp() - previousSample.getTimestamp() > 60) {
                LOG.trace("Filling gap between {} and {}", Instant.ofEpochSecond(previousSample.getTimestamp() + 60), Instant.ofEpochSecond(sample.getTimestamp()));
                for (int ts = previousSample.getTimestamp() + 60; ts < sample.getTimestamp(); ts += 60) {
                    it.add(createDummySample(ts));
                }
            }
            previousSample = sample;
        }

        final long nanoEnd = System.nanoTime();

        final long executionTime = (nanoEnd - nanoStart) / 1000000;

        final int dummyCount = ret.size() - samples.size();
        LOG.trace("Filled gaps with {} samples in {}ms", dummyCount, executionTime);

        return ret;
    }

    private T createDummySample(final int ts) {
        final T dummySample = createActivitySample();
        dummySample.setTimestamp(ts);
        dummySample.setRawKind(ActivityKind.UNKNOWN.getCode());
        dummySample.setRawIntensity(ActivitySample.NOT_MEASURED);
        dummySample.setSteps(ActivitySample.NOT_MEASURED);
        dummySample.setHeartRate(ActivitySample.NOT_MEASURED);
        dummySample.setDistanceCm(ActivitySample.NOT_MEASURED);
        dummySample.setActiveCalories(ActivitySample.NOT_MEASURED);
        dummySample.setProvider(this);
        return dummySample;
    }
}
