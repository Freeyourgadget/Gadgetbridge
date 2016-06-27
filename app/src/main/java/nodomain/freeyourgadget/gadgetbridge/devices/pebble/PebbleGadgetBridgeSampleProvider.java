package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PebbleGadgetBridgeSampleProvider extends MorpheuzSampleProvider {
    public PebbleGadgetBridgeSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
        movementDivisor = 63.0f;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE;
    }
}
