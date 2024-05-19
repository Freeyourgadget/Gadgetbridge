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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.threeten.bp.LocalDate;

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

/**
 * Base class for all sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 * @param <T> the sample type
 */
public abstract class AbstractSampleProvider<T extends AbstractActivitySample> implements SampleProvider<T> {
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

    @Override
    public List<T> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    @Override
    public List<T> getActivitySamples(int timestamp_from, int timestamp_to) {
        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
        }
    }

    @Override
    public List<T> getSleepSamples(int timestamp_from, int timestamp_to) {
        final DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();

        // If the device does not support REM sleep, we need to exclude its bit from the activity type
        int sleepActivityType = ActivityKind.TYPE_SLEEP;
        if (!coordinator.supportsRemSleep()) {
            sleepActivityType &= ~ActivityKind.TYPE_REM_SLEEP;
        }

        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to, sleepActivityType);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, sleepActivityType);
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

    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        if (getRawKindSampleProperty() == null && activityType != ActivityKind.TYPE_ALL) {
            // if we do not have a raw kind property we cannot query anything else then TYPE_ALL
            return Collections.emptyList();
        }
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestamp_from))
            .where(timestampProperty.le(timestamp_to), getClauseForActivityType(qb, activityType));
        List<T> samples = qb.build().list();
        for (T sample : samples) {
            sample.setProvider(this);
        }
        detachFromSession();
        return samples;
    }

    /**
     * Detaches all samples of this type from the session. Changes to them may not be
     * written back to the database.
     *
     * Subclasses should call this method after performing custom queries.
     */
    protected void detachFromSession() {
        getSampleDao().detachAll();
    }

    private WhereCondition[] getClauseForActivityType(QueryBuilder qb, int activityTypes) {
        if (activityTypes == ActivityKind.TYPE_ALL) {
            return NO_CONDITIONS;
        }

        int[] dbActivityTypes = ActivityKind.mapToDBActivityTypes(activityTypes, this);
        WhereCondition activityTypeCondition = getActivityTypeConditions(qb, dbActivityTypes);
        return new WhereCondition[] { activityTypeCondition };
    }

    private WhereCondition getActivityTypeConditions(QueryBuilder qb, int[] dbActivityTypes) {
        // What a crappy QueryBuilder API ;-( QueryBuilder.or(WhereCondition[]) with a runtime array length
        // check would have worked just fine.
        if (dbActivityTypes.length == 0) {
            return null;
        }
        Property rawKindProperty = getRawKindSampleProperty();
        if (rawKindProperty == null) {
            return null;
        }

        if (dbActivityTypes.length == 1) {
            return rawKindProperty.eq(dbActivityTypes[0]);
        }
        if (dbActivityTypes.length == 2) {
            return qb.or(rawKindProperty.eq(dbActivityTypes[0]),
                    rawKindProperty.eq(dbActivityTypes[1]));
        }
        final int offset = 2;
        int len = dbActivityTypes.length - offset;
        WhereCondition[] trailingConditions = new WhereCondition[len];
        for (int i = 0; i < len; i++) {
            trailingConditions[i] = rawKindProperty.eq(dbActivityTypes[i + offset]);
        }
        return qb.or(rawKindProperty.eq(dbActivityTypes[0]),
                rawKindProperty.eq(dbActivityTypes[1]),
                trailingConditions);
    }

    private List<T> getActivitySamplesByActivityFilter(int timestamp_from, int timestamp_to, int activityFilter) {
        List<T> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<T> filteredSamples = new ArrayList<>();

        for (T sample : samples) {
            if ((sample.getKind() & activityFilter) != 0) {
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
        final T lastSample = getLastSampleWithStepsBefore(samples.get(0).getTimestamp(), stepsSampleProperty);
        if (lastSample != null && sameDay(lastSample, samples.get(0)) && samples.get(0).getSteps() > 0) {
            samples.get(0).setSteps(samples.get(0).getSteps() - lastSample.getSteps());
        }

        // Steps on the Garmin Watch are reported cumulatively per day - convert them to
        // This slightly breaks activity recognition, because we don't have per-minute granularity...
        int prevSteps = samples.get(0).getSteps();
        samples.get(0).setTimestamp((samples.get(0).getTimestamp() / 60) * 60);

        for (int i = 1; i < samples.size(); i++) {
            final T s1 = samples.get(i - 1);
            final T s2 = samples.get(i);
            s2.setTimestamp((s2.getTimestamp() / 60) * 60);

            if (!sameDay(s1, s2)) {
                // went past midnight - reset steps
                prevSteps = s2.getSteps() > 0 ? s2.getSteps() : 0;
            } else if (s2.getSteps() > 0) {
                // New steps sample for the current day - subtract the previous seen sample
                int bak = s2.getSteps();
                s2.setSteps(s2.getSteps() - prevSteps);
                prevSteps = bak;
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
}
