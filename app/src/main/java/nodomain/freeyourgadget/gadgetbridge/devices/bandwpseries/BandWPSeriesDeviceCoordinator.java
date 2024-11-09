package nodomain.freeyourgadget.gadgetbridge.devices.bandwpseries;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries.BandWPSeriesDeviceSupport;

public class BandWPSeriesDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_bandw_pseries;
    }

    @Override
    public String getManufacturer() {
        return "Bowers and Wilkins";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return BandWPSeriesDeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("LE_BWHP");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery0 = new BatteryConfig(0, R.drawable.ic_earbuds_battery, R.string.left_earbud);
        BatteryConfig battery1 = new BatteryConfig(1, R.drawable.ic_earbuds_battery, R.string.right_earbud);
        BatteryConfig battery2 = new BatteryConfig(2, R.drawable.ic_tws_case, R.string.battery_case);
        return new BatteryConfig[]{battery0, battery1, battery2};
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[] {
                R.xml.devicesettings_active_noise_cancelling_toggle,
                R.xml.devicesettings_bandw_pseries
        };
    }

}
