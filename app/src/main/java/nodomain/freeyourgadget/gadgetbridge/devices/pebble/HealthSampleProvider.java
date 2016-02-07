package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HealthSampleProvider implements SampleProvider {

    protected final float movementDivisor = 8000f;

    @Override
    public int normalizeType(byte rawType) {
        return ActivityKind.TYPE_UNKNOWN;
    }

    @Override
    public byte toRawActivityKind(int activityKind) {
        return ActivityKind.TYPE_UNKNOWN;
    }


    @Override
    public float normalizeIntensity(short rawIntensity) {
        return rawIntensity / movementDivisor;
    }


    @Override
    public byte getID() {
        return SampleProvider.PROVIDER_PEBBLE_HEALTH;
    }
}
