package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class PebbleGadgetBridgeSampleProvider extends MorpheuzSampleProvider {
    public PebbleGadgetBridgeSampleProvider() {
        movementDivisor = 63.0f;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE;
    }
}
