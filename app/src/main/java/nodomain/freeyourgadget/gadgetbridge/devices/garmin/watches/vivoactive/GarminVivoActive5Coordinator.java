package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.vivoactive;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVivoActive5Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívoactive 5$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive_5;
    }
}