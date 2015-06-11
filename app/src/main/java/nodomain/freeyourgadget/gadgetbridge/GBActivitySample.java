package nodomain.freeyourgadget.gadgetbridge;

public class GBActivitySample {
    public static final byte PROVIDER_MIBAND = 0;
    public static final byte PROVIDER_PEBBLE_MORPHEUZ = 1;

    public static final byte TYPE_DEEP_SLEEP = 4;
    public static final byte TYPE_LIGHT_SLEEP = 5;
    public static final byte TYPE_UNKNOWN = -1;
    // add more here

    private final int timestamp;
    private final byte provider;
    private final short intensity;
    private final byte steps;
    private final byte type;

    public GBActivitySample(int timestamp, byte provider, short intensity, byte steps, byte type) {
        this.timestamp = timestamp;
        this.provider = provider;
        this.intensity = intensity;
        this.steps = steps;
        this.type = type;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public byte getProvider() {
        return provider;
    }

    public short getIntensity() {
        return intensity;
    }

    public byte getSteps() {
        return steps;
    }

    public byte getType() {
        return type;
    }

}
