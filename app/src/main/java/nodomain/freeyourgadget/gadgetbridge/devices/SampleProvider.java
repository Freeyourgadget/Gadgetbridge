package nodomain.freeyourgadget.gadgetbridge.devices;

public interface SampleProvider {
    int PROVIDER_MIBAND = 0;
    int PROVIDER_PEBBLE_MORPHEUZ = 1;
    int PROVIDER_PEBBLE_GADGETBRIDGE = 2;
    int PROVIDER_PEBBLE_MISFIT = 3;
    int PROVIDER_PEBBLE_HEALTH = 4;

    int PROVIDER_UNKNOWN = 100;

    int normalizeType(int rawType);

    int toRawActivityKind(int activityKind);

    float normalizeIntensity(int rawIntensity);

    int getID();
}
