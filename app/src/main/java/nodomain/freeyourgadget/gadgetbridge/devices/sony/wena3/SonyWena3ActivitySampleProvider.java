/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.BehaviorSample;

public class SonyWena3ActivitySampleProvider extends AbstractSampleProvider<Wena3ActivitySample> {
    public SonyWena3ActivitySampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<Wena3ActivitySample, ?> getSampleDao() {
        return getSession().getWena3ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return Wena3ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return Wena3ActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public ActivityKind normalizeType(int rawType) {
        if(rawType < 0 || rawType >= BehaviorSample.Type.LUT.length) return ActivityKind.UNKNOWN;

        BehaviorSample.Type internalType = BehaviorSample.Type.LUT[rawType];
        switch(internalType) {
            case NOT_WEARING:
                return ActivityKind.NOT_WORN;

            case WALK:
                return ActivityKind.WALKING;
            case RUN:
                return ActivityKind.RUNNING;
            case EXERCISE:
                return ActivityKind.EXERCISE;

            case SLEEP_LIGHT:
                return ActivityKind.LIGHT_SLEEP;
            case SLEEP_REM:
                return ActivityKind.REM_SLEEP;
            case SLEEP_DEEP:
                return ActivityKind.DEEP_SLEEP;

            case STATIC:
            case SLEEP_AWAKE:
            case UNKNOWN:
            default:
                return ActivityKind.UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(ActivityKind activityKind) {
        switch(activityKind) {
            case NOT_MEASURED:
            case NOT_WORN:
                return BehaviorSample.Type.NOT_WEARING.ordinal();
            case WALKING:
                return BehaviorSample.Type.WALK.ordinal();
            case RUNNING:
                return BehaviorSample.Type.RUN.ordinal();
            case LIGHT_SLEEP:
                return BehaviorSample.Type.SLEEP_LIGHT.ordinal();
            case REM_SLEEP:
                return BehaviorSample.Type.SLEEP_REM.ordinal();
            case DEEP_SLEEP:
                return BehaviorSample.Type.SLEEP_DEEP.ordinal();
            case EXERCISE:
                return BehaviorSample.Type.EXERCISE.ordinal();
            default:
                return BehaviorSample.Type.UNKNOWN.ordinal();
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return 0;
    }

    @Override
    public Wena3ActivitySample createActivitySample() {
        return new Wena3ActivitySample();
    }
}
