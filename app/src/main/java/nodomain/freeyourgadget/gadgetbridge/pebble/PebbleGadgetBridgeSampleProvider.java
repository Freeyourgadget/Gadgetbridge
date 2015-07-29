package nodomain.freeyourgadget.gadgetbridge.pebble;

import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

public class PebbleGadgetBridgeSampleProvider extends MorpheuzSampleProvider {
    public PebbleGadgetBridgeSampleProvider() {
        movementDivisor = 63.0f;
    }

    @Override
    public byte getID() {
        return SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE;
    }
}
