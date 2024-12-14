/*  Copyright (C) 2023-2024 José Rebelo

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

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractTimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Base class for all time sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 *
 * @param <T> the sample type
 */
public abstract class AbstractTimeSampleProvider<T extends AbstractTimeSample> implements TimeSampleProvider<T> {
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractTimeSampleProvider(final GBDevice device, final DaoSession session) {
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
    public List<T> getAllSamples(final long timestampFrom, final long timestampTo) {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Property timestampProperty = getTimestampSampleProperty();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestampFrom))
                .where(timestampProperty.le(timestampTo));
        final List<T> samples = qb.build().list();
        detachFromSession();
        return samples;
    }

    @Override
    public void addSample(final T activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addSamples(final List<T> activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    @Nullable
    @Override
    public T getLatestSample() {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderDesc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    @Override
    public T getLatestSample(final long until) {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(getTimestampSampleProperty().le(until))
                .where(deviceProperty.eq(dbDevice.getId()))
                .orderDesc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    public T getLastSampleBefore(final long timestampTo) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final Property deviceIdSampleProp = getDeviceIdentifierSampleProperty();
        final Property timestampSampleProp = getTimestampSampleProperty();
        final List<T> samples = getSampleDao().queryBuilder()
                .where(deviceIdSampleProp.eq(dbDevice.getId()),
                        timestampSampleProp.le(timestampTo))
                .orderDesc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    @Nullable
    public T getNextSampleAfter(final long timestampFrom) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final Property deviceIdSampleProp = getDeviceIdentifierSampleProperty();
        final Property timestampSampleProp = getTimestampSampleProperty();
        final List<T> samples = getSampleDao().queryBuilder()
                .where(deviceIdSampleProp.eq(dbDevice.getId()),
                        timestampSampleProp.ge(timestampFrom))
                .orderAsc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }

    @Nullable
    @Override
    public T getFirstSample() {
        final QueryBuilder<T> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderAsc(getTimestampSampleProperty()).limit(1);
        final List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
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

    @NonNull
    public abstract AbstractDao<T, ?> getSampleDao();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();
}
