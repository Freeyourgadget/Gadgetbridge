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
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

// TODO s/HuamiExtendedActivitySample/XiaomiActivitySample/g
public class XiaomiSampleProvider extends AbstractSampleProvider<HuamiExtendedActivitySample> {
    public XiaomiSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<HuamiExtendedActivitySample, ?> getSampleDao() {
        return getSession().getHuamiExtendedActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return HuamiExtendedActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HuamiExtendedActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HuamiExtendedActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(final int rawType) {
        // TODO
        return rawType;
    }

    @Override
    public int toRawActivityKind(final int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(final int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public HuamiExtendedActivitySample createActivitySample() {
        return new HuamiExtendedActivitySample();
    }
}
