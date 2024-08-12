/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.WithingsSteelHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.WithingsSteelHRActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class WithingsSteelHRSampleProvider extends AbstractSampleProvider<WithingsSteelHRActivitySample> {
    public WithingsSteelHRSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<WithingsSteelHRActivitySample, ?> getSampleDao() {
        return getSession().getWithingsSteelHRActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        switch (rawType) {
            case 1:
                return ActivityKind.LIGHT_SLEEP;
            case 2:
                return ActivityKind.DEEP_SLEEP;
            default:
                return ActivityKind.fromCode(rawType);
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch (activityKind) {
            case UNKNOWN:
                return 0;
            case LIGHT_SLEEP:
                return 1;
            case DEEP_SLEEP:
                return 2;
            default:
                return activityKind.getCode();
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        if (rawIntensity > 0) {
            return (float) (Math.log(rawIntensity) / 8);
        }

        return 0;
    }

    @Override
    public WithingsSteelHRActivitySample createActivitySample() {
        return new WithingsSteelHRActivitySample();
    }
}
