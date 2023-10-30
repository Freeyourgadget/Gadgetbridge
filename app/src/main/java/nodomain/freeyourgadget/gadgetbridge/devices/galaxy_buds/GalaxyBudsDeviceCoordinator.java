package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsDeviceCoordinator extends GalaxyBudsGenericCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Galaxy Buds \\(.*");
    }

    @Override
    public int getBatteryCount() {
        return 2;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2};
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_galaxybuds;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds,
        };
    }
}
