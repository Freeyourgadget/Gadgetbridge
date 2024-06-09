package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivosmart;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVivosmart5Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívosmart 5$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivosmart_5;
    }
}
