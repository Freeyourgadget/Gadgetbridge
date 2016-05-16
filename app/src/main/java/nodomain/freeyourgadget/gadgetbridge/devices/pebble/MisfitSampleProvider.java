package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class MisfitSampleProvider implements SampleProvider {

    protected final float movementDivisor = 300f;

    @Override
    public int normalizeType(int rawType) {
        return (int) rawType;
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
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }
}
