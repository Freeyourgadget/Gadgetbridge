/*  Copyright (C) 2018 Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.ZeTimeActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ZeTimeActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ZeTimeSampleProvider extends AbstractSampleProvider<ZeTimeActivitySample> {

    private final float movementDivisor = 6000.0f;
    private GBDevice mDevice;
    private DaoSession mSession;

    public ZeTimeSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);

        mSession = session;
        mDevice = device;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity/movementDivisor;
    }

    @Override
    public ZeTimeActivitySample createActivitySample() {
        return new ZeTimeActivitySample();
    }

    @Override
    public AbstractDao<ZeTimeActivitySample, ?> getSampleDao() {
        return getSession().getZeTimeActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ZeTimeActivitySampleDao.Properties.DeviceId;
    }
}
