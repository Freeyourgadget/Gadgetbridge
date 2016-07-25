package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class MisfitSampleProvider implements SampleProvider {

    protected final float movementDivisor = 300f;

    public MisfitSampleProvider(GBDevice device, DaoSession session) {

    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return (byte) activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public List getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return null;
    }

    @Override
    public List getActivitySamples(int timestamp_from, int timestamp_to) {
        return null;
    }

    @Override
    public List getSleepSamples(int timestamp_from, int timestamp_to) {
        return null;
    }

    @Override
    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind) {

    }

    @Override
    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind) {

    }

    @Override
    public int fetchLatestTimestamp() {
        return 0;
    }

    @Override
    public void addGBActivitySample(AbstractActivitySample activitySample) {

    }

    @Override
    public void addGBActivitySamples(AbstractActivitySample[] activitySamples) {

    }

    @Override
    public AbstractActivitySample createActivitySample() {
        return null;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }
}
