package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class Ear1Coordinator extends AbstractEarCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Nothing ear (1)", Pattern.LITERAL);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothingear1;
    }

    @Override
    public boolean incrementCounter() {
        return false;
    }

    @Override
    public boolean supportsLightAncAndTransparency() {
        return true;
    }
}
