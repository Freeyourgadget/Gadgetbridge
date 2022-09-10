/*  Copyright (C) 2022  José Rebelo

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
    protected List<HuamiExtendedActivitySample> getGBActivitySamples(final int timestamp_from, final int timestamp_to, final int activityType) {
        final List<HuamiExtendedActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, activityType);
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
    public int normalizeType(final int rawType) {
        switch (rawType) {
            case TYPE_OUTDOOR_RUNNING:
                return ActivityKind.TYPE_RUNNING;
            case TYPE_NOT_WORN:
            case TYPE_CHARGING:
                return ActivityKind.TYPE_NOT_WORN;
            case TYPE_SLEEP:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            case TYPE_CUSTOM_DEEP_SLEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case TYPE_CUSTOM_REM_SLEEP:
                return ActivityKind.TYPE_REM_SLEEP;
        }

        return ActivityKind.TYPE_UNKNOWN;
    }

    @Override
    public int toRawActivityKind(final int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_RUNNING:
                return TYPE_OUTDOOR_RUNNING;
            case ActivityKind.TYPE_NOT_WORN:
                return TYPE_NOT_WORN;
            case ActivityKind.TYPE_LIGHT_SLEEP:
            case ActivityKind.TYPE_DEEP_SLEEP:
            case ActivityKind.TYPE_REM_SLEEP:
                return TYPE_SLEEP;
        }

        return TYPE_CUSTOM_UNSET;
    }
}
