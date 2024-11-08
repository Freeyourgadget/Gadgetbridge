package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.fenix;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminFenix6SProCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^fenix 6S Pro$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_fenix_6s_pro;
    }
}
