package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;

public interface SampleProvider<T extends AbstractActivitySample> {
    // TODO: these constants can all be removed
    int PROVIDER_MIBAND = 0;
    int PROVIDER_PEBBLE_MORPHEUZ = 1;
    int PROVIDER_PEBBLE_GADGETBRIDGE = 2; // removed
    int PROVIDER_PEBBLE_MISFIT = 3;
    int PROVIDER_PEBBLE_HEALTH = 4;

    int PROVIDER_UNKNOWN = 100;
    // TODO: can also be removed
    int getID();

    int normalizeType(int rawType);

    int toRawActivityKind(int activityKind);

    float normalizeIntensity(int rawIntensity);

    List<T> getAllActivitySamples(int timestamp_from, int timestamp_to);

    List<T> getActivitySamples(int timestamp_from, int timestamp_to);

    List<T> getSleepSamples(int timestamp_from, int timestamp_to);

    void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind);

    void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind);

    int fetchLatestTimestamp();

    void addGBActivitySample(T activitySample);

    void addGBActivitySamples(T[] activitySamples);

    T createActivitySample();
}
