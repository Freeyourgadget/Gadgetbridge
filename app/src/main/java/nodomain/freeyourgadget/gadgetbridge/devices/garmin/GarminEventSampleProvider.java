/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSample;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminEventSampleProvider extends AbstractTimeSampleProvider<GarminEventSample> {
    public GarminEventSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<GarminEventSample, ?> getSampleDao() {
        return getSession().getGarminEventSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return GarminEventSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return GarminEventSampleDao.Properties.DeviceId;
    }

    @Override
    public GarminEventSample createSample() {
        return new GarminEventSample();
    }

    public List<GarminEventSample> getSleepEvents(final long timestampFrom, final long timestampTo) {
        final QueryBuilder<GarminEventSample> qb = getSampleDao().queryBuilder();
        final Property timestampProperty = getTimestampSampleProperty();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestampFrom))
                .where(timestampProperty.le(timestampTo))
                .where(GarminEventSampleDao.Properties.Event.eq(74));

        final List<GarminEventSample> samples = qb.build().list();
        detachFromSession();
        return samples;
    }

    public GarminEventSample getNextSleepEventAfter(final long timestampFrom) {
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }

        final Property deviceIdSampleProp = getDeviceIdentifierSampleProperty();
        final Property timestampSampleProp = getTimestampSampleProperty();
        final List<GarminEventSample> samples = getSampleDao().queryBuilder()
                .where(
                        deviceIdSampleProp.eq(dbDevice.getId()),
                        timestampSampleProp.ge(timestampFrom),
                        GarminEventSampleDao.Properties.Event.eq(74)
                ).orderAsc(getTimestampSampleProperty())
                .limit(1)
                .list();

        return !samples.isEmpty() ? samples.get(0) : null;
    }
}
