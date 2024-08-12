/*  Copyright (C) 2019-2024 Cre3per

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
package nodomain.freeyourgadget.gadgetbridge.devices.makibeshr3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MakibesHR3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class MakibesHR3SampleProvider extends AbstractSampleProvider<MakibesHR3ActivitySample> {
    public MakibesHR3SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        return ActivityKind.fromCode(rawType);
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        return activityKind.getCode();
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity;
    }

    @Override
    public MakibesHR3ActivitySample createActivitySample() {
        return new MakibesHR3ActivitySample();
    }

    @Override
    public AbstractDao<MakibesHR3ActivitySample, ?> getSampleDao() {
        return getSession().getMakibesHR3ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return MakibesHR3ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return MakibesHR3ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return MakibesHR3ActivitySampleDao.Properties.DeviceId;
    }
}
