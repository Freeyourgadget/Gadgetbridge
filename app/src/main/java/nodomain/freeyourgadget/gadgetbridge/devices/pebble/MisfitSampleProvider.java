package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class MisfitSampleProvider implements SampleProvider {

    protected float movementDivisor = 300f;

    @Override
    public int normalizeType(byte rawType) {
        return (int) rawType;
    }

    @Override
    public byte toRawActivityKind(int activityKind) {
        return (byte) activityKind;
    }


    @Override
    public float normalizeIntensity(short rawIntensity) {
        return rawIntensity / movementDivisor;
    }


    @Override
    public byte getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }
}
