/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.entities;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public abstract class AbstractActivitySample implements ActivitySample {
    private SampleProvider mProvider;

    @Override
    public SampleProvider getProvider() {
        return mProvider;
    }

    public void setProvider(SampleProvider provider) {
        mProvider = provider;
    }

    @Override
    public int getKind() {
        return getProvider().normalizeType(getRawKind());
    }

    @Override
    public int getRawKind() {
        return NOT_MEASURED;
    }

    @Override
    public float getIntensity() {
        return getProvider().normalizeIntensity(getRawIntensity());
    }

    public void setRawKind(int kind) {
    }

    public void setRawIntensity(int intensity) {
    }

    public void setSteps(int steps) {
    }

    /**
     * Unix timestamp of the sample, i.e. the number of seconds since 1970-01-01 00:00:00 UTC.
     */
    public abstract void setTimestamp(int timestamp);

    public abstract void setUserId(long userId);

    @Override
    public void setHeartRate(int heartRate) {
    }

    @Override
    public int getHeartRate() {
        return NOT_MEASURED;
    }

    public abstract void setDeviceId(long deviceId);

    public abstract long getDeviceId();

    public abstract long getUserId();

    @Override
    public int getRawIntensity() {
        return NOT_MEASURED;
    }

    @Override
    public int getSteps() {
        return NOT_MEASURED;
    }

    @Override
    public String toString() {
        int kind = getProvider() != null ? getKind() : ActivitySample.NOT_MEASURED;
        float intensity = getProvider() != null ? getIntensity() : ActivitySample.NOT_MEASURED;
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(getTimestamp())) +
                ", intensity=" + intensity +
                ", steps=" + getSteps() +
                ", heartrate=" + getHeartRate() +
                ", type=" + kind +
                ", userId=" + getUserId() +
                ", deviceId=" + getDeviceId() +
                '}';
    }
}
