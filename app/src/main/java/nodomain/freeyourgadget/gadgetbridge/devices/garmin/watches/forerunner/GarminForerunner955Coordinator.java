package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.forerunner;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminForerunner955Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 955$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_955;
    }
}
