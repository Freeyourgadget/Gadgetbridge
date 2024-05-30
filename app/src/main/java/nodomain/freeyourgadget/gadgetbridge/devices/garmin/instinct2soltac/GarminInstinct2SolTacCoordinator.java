package nodomain.freeyourgadget.gadgetbridge.devices.garmin.instinct2soltac;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;

public class GarminInstinct2SolTacCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct 2 SolTac$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2_soltac;
    }
}
