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
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(getTimestamp())) +
                ", intensity=" + getIntensity() +
                ", steps=" + getSteps() +
                ", heartrate=" + getHeartRate() +
                ", type=" + getKind() +
                ", userId=" + getUserId() +
                ", deviceId=" + getDeviceId() +
                '}';
    }
}
