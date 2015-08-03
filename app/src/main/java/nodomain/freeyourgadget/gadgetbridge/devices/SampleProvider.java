package nodomain.freeyourgadget.gadgetbridge.devices;

public interface SampleProvider {
    public static final byte PROVIDER_MIBAND = 0;
    public static final byte PROVIDER_PEBBLE_MORPHEUZ = 1;
    public static final byte PROVIDER_PEBBLE_GADGETBRIDGE = 2;
    public static final byte PROVIDER_UNKNOWN = 100;

    int normalizeType(byte rawType);

    byte toRawActivityKind(int activityKind);

    float normalizeIntensity(short rawIntensity);

    byte getID();
}
