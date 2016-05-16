package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public interface SampleProvider<T extends ActivitySample> {
    int PROVIDER_MIBAND = 0;
    int PROVIDER_PEBBLE_MORPHEUZ = 1;
    int PROVIDER_PEBBLE_GADGETBRIDGE = 2;
    int PROVIDER_PEBBLE_MISFIT = 3;
    int PROVIDER_PEBBLE_HEALTH = 4;

    int PROVIDER_UNKNOWN = 100;

    int normalizeType(int rawType);

    int toRawActivityKind(int activityKind);

    float normalizeIntensity(int rawIntensity);

    List<T> getAllActivitySamples(int timestamp_from, int timestamp_to);

    List<T> getActivitySamples(int timestamp_from, int timestamp_to);

    List<T> getSleepSamples(int timestamp_from, int timestamp_to);

    int fetchLatestTimestamp();

    void addGBActivitySample(AbstractActivitySample activitySample);

    void addGBActivitySamples(AbstractActivitySample[] activitySamples);

    int getID();
}
