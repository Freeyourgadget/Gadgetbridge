package nodomain.freeyourgadget.gadgetbridge.devices.garmin.venu2plus;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVenu2PlusCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Venu 2 Plus");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_venu_2_plus;
    }
}
