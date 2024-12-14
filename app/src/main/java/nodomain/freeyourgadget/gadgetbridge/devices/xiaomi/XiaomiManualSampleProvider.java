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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class XiaomiManualSampleProvider extends AbstractTimeSampleProvider<XiaomiManualSample> {
    public static final int TYPE_HR = 0x11;
    public static final int TYPE_SPO2 = 0x12;
    public static final int TYPE_STRESS = 0x13;
    public static final int TYPE_TEMPERATURE = 0x44;

    public XiaomiManualSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<XiaomiManualSample, ?> getSampleDao() {
        return getSession().getXiaomiManualSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XiaomiManualSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XiaomiManualSampleDao.Properties.DeviceId;
    }

    @Override
    public XiaomiManualSample createSample() {
        return new XiaomiManualSample();
    }

    @Nullable
    public XiaomiManualSample getLatestSample(final int type) {
        final QueryBuilder<XiaomiManualSample> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()))
                .where(XiaomiManualSampleDao.Properties.Type.eq(type))
                .orderDesc(getTimestampSampleProperty()).limit(1);
        final List<XiaomiManualSample> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    public XiaomiManualSample getLatestSample(final int type, final long until) {
        final QueryBuilder<XiaomiManualSample> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()))
                .where(XiaomiManualSampleDao.Properties.Timestamp.le(until))
                .where(XiaomiManualSampleDao.Properties.Type.eq(type))
                .orderDesc(getTimestampSampleProperty()).limit(1);
        final List<XiaomiManualSample> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    @Nullable
    public XiaomiManualSample getFirstSample(final int type) {
        final QueryBuilder<XiaomiManualSample> qb = getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        final Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()))
                .where(XiaomiManualSampleDao.Properties.Type.eq(type))
                .orderAsc(getTimestampSampleProperty()).limit(1);
        final List<XiaomiManualSample> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }
}
