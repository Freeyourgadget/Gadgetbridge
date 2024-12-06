package nodomain.freeyourgadget.gadgetbridge.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GarminInstinctSolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct (Solar|Dual Power)$");
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 16;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_solar;
    }
}
