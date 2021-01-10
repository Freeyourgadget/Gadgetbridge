/*  Copyright (C) 2018-2021 Daniele Gobbetti, Gordon Williams, Vadim Kaushan

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
package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ID115ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class BangleJSSampleProvider extends AbstractSampleProvider<BangleJSActivitySample> {
    public BangleJSSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    public static final int TYPE_ACTIVITY = 0;

    @Override
    public AbstractDao<BangleJSActivitySample, ?> getSampleDao() {
        return getSession().getBangleJSActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return BangleJSActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return BangleJSActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return BangleJSActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
            case TYPE_ACTIVITY:
                return ActivityKind.TYPE_ACTIVITY;
            default: // fall through
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return TYPE_ACTIVITY;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public BangleJSActivitySample createActivitySample() {
        return new BangleJSActivitySample();
    }
}
