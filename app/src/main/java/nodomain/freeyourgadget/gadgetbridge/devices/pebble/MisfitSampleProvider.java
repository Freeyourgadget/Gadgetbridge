package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

public class MisfitSampleProvider extends AbstractPebbleSampleProvider {

    protected final float movementDivisor = 300f;

    public MisfitSampleProvider(DaoSession session) {
        super(session);
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
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }
}
