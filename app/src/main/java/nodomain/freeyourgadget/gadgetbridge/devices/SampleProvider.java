package nodomain.freeyourgadget.gadgetbridge.devices;

public interface SampleProvider {
    byte PROVIDER_MIBAND = 0;
    byte PROVIDER_PEBBLE_MORPHEUZ = 1;
    byte PROVIDER_PEBBLE_GADGETBRIDGE = 2;
    byte PROVIDER_PEBBLE_MISFIT = 3;
    byte PROVIDER_PEBBLE_HEALTH = 4;

    byte PROVIDER_UNKNOWN = 100;

    int normalizeType(byte rawType);

    byte toRawActivityKind(int activityKind);

    float normalizeIntensity(short rawIntensity);

    byte getID();
}
