package nodomain.freeyourgadget.gadgetbridge.impl;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class GBActivitySample implements ActivitySample {
    private final int timestamp;
    private final SampleProvider provider;
    private final short intensity;
    private final short steps;
    private final byte type;
    private final short customShortValue;

    public GBActivitySample(SampleProvider provider, int timestamp, short intensity, short steps, byte type) {
        this(provider, timestamp, intensity, steps, type, (short) 0);
    }

    public GBActivitySample(SampleProvider provider, int timestamp, short intensity, short steps, byte type, short customShortValue) {
        this.timestamp = timestamp;
        this.provider = provider;
        this.intensity = intensity;
        this.steps = steps;
        this.customShortValue = customShortValue;
        this.type = type;
        validate();
    }

    private void validate() {
        if (steps < 0) {
            throw new IllegalArgumentException("steps must be >= 0");
        }
        if (intensity < 0) {
            throw new IllegalArgumentException("intensity must be >= 0");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be >= 0");
        }
        if (customShortValue < 0) {
            throw new IllegalArgumentException("customShortValue must be >= 0");
        }
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public SampleProvider getProvider() {
        return provider;
    }

    @Override
    public short getRawIntensity() {
        return intensity;
    }

    @Override
    public float getIntensity() {
        return getProvider().normalizeIntensity(getRawIntensity());
    }

    @Override
    public short getSteps() {
        return steps;
    }

    @Override
    public byte getRawKind() {
        return type;
    }

    @Override
    public int getKind() {
        return getProvider().normalizeType(getRawKind());
    }

    @Override
    public short getCustomShortValue() {
        return customShortValue;
    }

    @Override
    public String toString() {
        return "GBActivitySample{" +
                "timestamp=" + DateTimeUtils.formatDateTime(DateTimeUtils.parseTimeStamp(timestamp)) +
                ", intensity=" + getIntensity() +
                ", steps=" + getSteps() +
                ", customShortValue=" + getCustomShortValue() +
                ", type=" + getKind() +
                '}';
    }
}
