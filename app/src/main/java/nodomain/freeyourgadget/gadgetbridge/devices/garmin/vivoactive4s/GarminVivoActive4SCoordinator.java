package nodomain.freeyourgadget.gadgetbridge.devices.garmin.vivoactive4s;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminVivoActive4SCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("vívoactive 4S");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive_4s;
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public boolean supportsAgpsUpdates() {
        return true;
    }
}
