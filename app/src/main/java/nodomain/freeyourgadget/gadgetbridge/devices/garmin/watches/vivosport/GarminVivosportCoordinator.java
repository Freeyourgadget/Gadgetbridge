package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosport;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVivosportCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívosport$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivosport;
    }
}
