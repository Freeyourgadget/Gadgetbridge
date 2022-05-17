package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsDeviceCoordinator extends GalaxyBudsGenericCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {

        String name = candidate.getName();

        if (name != null && (
                name.startsWith("Galaxy Buds (")
        )) {
            return DeviceType.GALAXY_BUDS;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.GALAXY_BUDS;
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
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds,
        };
    }
}
