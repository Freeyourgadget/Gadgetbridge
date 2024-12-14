package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;

public interface Vo2MaxSampleProvider<T extends Vo2MaxSample> extends TimeSampleProvider<T> {
    @Nullable
    T getLatestSample(Vo2MaxSample.Type type, long until);

    @Nullable
    default T getLatestSample(long until) {
        return getLatestSample(Vo2MaxSample.Type.ANY, until);
    }
}
