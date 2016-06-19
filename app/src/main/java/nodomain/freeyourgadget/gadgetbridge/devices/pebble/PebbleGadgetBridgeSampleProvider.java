package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

public class PebbleGadgetBridgeSampleProvider extends MorpheuzSampleProvider {
    public PebbleGadgetBridgeSampleProvider(DaoSession session) {
        super(session);
        movementDivisor = 63.0f;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE;
    }
}
