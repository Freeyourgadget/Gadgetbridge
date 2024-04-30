package nodomain.freeyourgadget.gadgetbridge.devices.garmin.swim2;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminSwim2Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Swim 2");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_swim_2;
    }
}
