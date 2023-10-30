package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsLiveDeviceCoordinator extends GalaxyBudsGenericCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Galaxy Buds Live \\(.*");
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_live_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_galaxy_buds_live_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }


    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds_live,
        };
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_galaxybuds_live;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds_live;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_galaxy_buds_live_disabled;
    }
}
