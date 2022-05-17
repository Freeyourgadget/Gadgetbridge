package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsProDeviceCoordinator extends GalaxyBudsGenericCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {

        String name = candidate.getName();

        if (name != null && (
                name.startsWith("Galaxy Buds Pro (")
        )) {
            return DeviceType.GALAXY_BUDS_PRO;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new GalaxyBudsSettingsCustomizer(device);
    }
    @Override
    public DeviceType getDeviceType() {
        return DeviceType.GALAXY_BUDS_PRO;
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_buds_pro_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_buds_pro_left, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_buds_pro_right, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds_pro,
        };
    }
}