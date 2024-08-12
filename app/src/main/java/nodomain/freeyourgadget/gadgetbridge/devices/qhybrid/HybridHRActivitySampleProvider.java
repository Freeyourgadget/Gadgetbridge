/*  Copyright (C) 2020-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser.ActivityEntry;

public class HybridHRActivitySampleProvider extends AbstractSampleProvider<HybridHRActivitySample> {
    public HybridHRActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<HybridHRActivitySample, ?> getSampleDao() {
        return getSession().getHybridHRActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HybridHRActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HybridHRActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        if (rawType == -1) return ActivityKind.UNKNOWN;
        return ActivityEntry.WEARING_STATE.fromValue((byte) rawType).getActivityKind();
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return 0;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return Math.min(rawIntensity / 128f, 1f);
    }

    @Override
    public HybridHRActivitySample createActivitySample() {
        return new HybridHRActivitySample();
    }
}
