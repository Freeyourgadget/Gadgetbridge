package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivomove;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVivomoveStyleCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívomove Style$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivomove_style;
    }
}
