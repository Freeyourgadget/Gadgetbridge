package nodomain.freeyourgadget.gadgetbridge.impl;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class GBActivitySample implements ActivitySample {
    private final int timestamp;
    private final SampleProvider provider;
    private final short intensity;
    private final short steps;
    private final byte type;

    public GBActivitySample(SampleProvider provider, int timestamp, short intensity, short steps, byte type) {
        this.timestamp = timestamp;
        this.provider = provider;
        this.intensity = intensity;
        this.steps = steps;
        this.type = type;
        validate();
    }

    private void validate() {
        if (steps < 0) {
            throw new IllegalArgumentException("steps must be > 0");
        }
        if (intensity < 0) {
            throw new IllegalArgumentException("intensity must be > 0");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be > 0");
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
}
