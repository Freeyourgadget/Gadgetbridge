/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import androidx.annotation.NonNull;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HuamiExtendedSampleProvider extends AbstractSampleProvider<HuamiExtendedActivitySample> {
    public static final int TYPE_CUSTOM_UNSET = -1;
    public static final int TYPE_OUTDOOR_RUNNING = 64;
    public static final int TYPE_NOT_WORN = 115;
    public static final int TYPE_CHARGING = 118;
    public static final int TYPE_SLEEP = 120;
    public static final int TYPE_CUSTOM_DEEP_SLEEP = TYPE_SLEEP + 1;
    public static final int TYPE_CUSTOM_REM_SLEEP = TYPE_SLEEP + 2;

    public HuamiExtendedSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 256.0f;
    }

    @Override
    public AbstractDao<HuamiExtendedActivitySample, ?> getSampleDao() {
        return getSession().getHuamiExtendedActivitySampleDao();
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
    protected Property getRawKindSampleProperty() {
        return HuamiExtendedActivitySampleDao.Properties.RawKind;
    }

    @Override
    public HuamiExtendedActivitySample createActivitySample() {
        return new HuamiExtendedActivitySample();
    }

    @Override
    protected List<HuamiExtendedActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to) {
        final List<HuamiExtendedActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to);
        postProcess(samples);
        return samples;
    }

    private void postProcess(final List<HuamiExtendedActivitySample> samples) {
        if (samples.isEmpty()) {
            return;
        }

        for (final HuamiExtendedActivitySample sample : samples) {
            if (sample.getRawKind() == TYPE_SLEEP) {
                // Band reports type sleep regardless of sleep type, so we map it to custom raw types
                // These thresholds are arbitrary, but seem to somewhat match the data that's displayed on the band

                sample.setDeepSleep(sample.getDeepSleep() & 127);
                sample.setRemSleep(sample.getRemSleep() & 127);

                if (sample.getRemSleep() > 55) {
                    sample.setRawKind(TYPE_CUSTOM_REM_SLEEP);
                    sample.setRawIntensity(sample.getRemSleep());
                } else if (sample.getDeepSleep() > 42) {
                    sample.setRawKind(TYPE_CUSTOM_DEEP_SLEEP);
                    sample.setRawIntensity(sample.getDeepSleep());
                } else {
                    sample.setRawIntensity(sample.getSleep());
                }
            }
        }
    }

    @Override
    public ActivityKind normalizeType(final int rawType) {
        switch (rawType) {
            case TYPE_OUTDOOR_RUNNING:
                return ActivityKind.RUNNING;
            case TYPE_NOT_WORN:
            case TYPE_CHARGING:
                return ActivityKind.NOT_WORN;
            case TYPE_SLEEP:
                return ActivityKind.LIGHT_SLEEP;
            case TYPE_CUSTOM_DEEP_SLEEP:
                return ActivityKind.DEEP_SLEEP;
            case TYPE_CUSTOM_REM_SLEEP:
                return ActivityKind.REM_SLEEP;
        }

        return ActivityKind.UNKNOWN;
    }

    @Override
    public int toRawActivityKind(final ActivityKind activityKind) {
        switch (activityKind) {
            case RUNNING:
                return TYPE_OUTDOOR_RUNNING;
            case NOT_WORN:
                return TYPE_NOT_WORN;
            case LIGHT_SLEEP:
            case DEEP_SLEEP:
            case REM_SLEEP:
                return TYPE_SLEEP;
        }

        return TYPE_CUSTOM_UNSET;
    }
}
