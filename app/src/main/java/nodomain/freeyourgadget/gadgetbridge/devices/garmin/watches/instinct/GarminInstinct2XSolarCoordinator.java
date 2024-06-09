package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminInstinct2XSolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct 2X Sol$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2x_solar;
    }
}
