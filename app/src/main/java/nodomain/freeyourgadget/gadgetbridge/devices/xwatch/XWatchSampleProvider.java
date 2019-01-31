/*  Copyright (C) 2017-2018 ladbsoft, protomors

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
package nodomain.freeyourgadget.gadgetbridge.devices.xwatch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XWatchActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.XWatchActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

//TODO: extend own class
public class XWatchSampleProvider extends AbstractSampleProvider<XWatchActivitySample> {
    public static final int TYPE_ACTIVITY = -1;

    public XWatchSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public int normalizeType(int rawType) {
        return ActivityKind.TYPE_ACTIVITY;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return TYPE_ACTIVITY;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 180.0f;
    }

    @Override
    public XWatchActivitySample createActivitySample() {
        return new XWatchActivitySample();
    }

    @Override
    public AbstractDao<XWatchActivitySample, ?> getSampleDao() {
        return getSession().getXWatchActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return XWatchActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XWatchActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XWatchActivitySampleDao.Properties.DeviceId;
    }
}
