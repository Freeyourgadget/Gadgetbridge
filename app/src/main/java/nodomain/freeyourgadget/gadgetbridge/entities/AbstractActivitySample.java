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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(getTimestamp())) +
                ", intensity=" + getIntensity() +
                ", steps=" + getSteps() +
                ", type=" + getKind() +
                '}';
    }
}
