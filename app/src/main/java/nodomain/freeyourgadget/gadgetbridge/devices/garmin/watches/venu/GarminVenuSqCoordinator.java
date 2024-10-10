package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.venu;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVenuSqCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Venu Sq$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_venu_sq;
    }
}
