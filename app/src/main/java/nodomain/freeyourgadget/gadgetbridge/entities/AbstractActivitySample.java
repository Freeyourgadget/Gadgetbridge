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
    public float getIntensity() {
        return getProvider().normalizeIntensity(getRawIntensity());
    }

    public abstract void setRawKind(int kind);

    public abstract void setRawIntensity(int intensity);

    public abstract void setSteps(int steps);

    public abstract void setTimestamp(int timestamp);

    public abstract void setUserId(Long userId);

    public abstract Long getUserId();

    public abstract void setDeviceId(Long deviceId);

    public abstract Long getDeviceId();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(getTimestamp())) +
                ", intensity=" + getIntensity() +
                ", steps=" + getSteps() +
                ", type=" + getKind() +
                ", userId=" + getUserId() +
                ", deviceId=" + getDeviceId() +
                '}';
    }
}
