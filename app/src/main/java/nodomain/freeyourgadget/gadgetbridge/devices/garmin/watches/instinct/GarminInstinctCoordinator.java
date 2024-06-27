package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminInstinctCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct;
    }
}
