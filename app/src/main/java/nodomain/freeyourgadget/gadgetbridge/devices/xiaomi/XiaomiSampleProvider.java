/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class XiaomiSampleProvider extends AbstractSampleProvider<XiaomiActivitySample> {
    public XiaomiSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<XiaomiActivitySample, ?> getSampleDao() {
        return getSession().getXiaomiActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return XiaomiActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XiaomiActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XiaomiActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(final int rawType) {
        // TODO
        return rawType;
    }

    @Override
    public int toRawActivityKind(final int activityKind) {
        // TODO
        return activityKind;
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        // TODO
        return rawIntensity;
    }

    @Override
    public XiaomiActivitySample createActivitySample() {
        return new XiaomiActivitySample();
    }
}
