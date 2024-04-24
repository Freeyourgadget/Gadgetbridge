package nodomain.freeyourgadget.gadgetbridge.devices.garmin.instinct2solar;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import java.util.regex.Pattern;

public class GarminInstinct2SolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Instinct 2 Solar");
    }
    
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2_solar;
    }
}
