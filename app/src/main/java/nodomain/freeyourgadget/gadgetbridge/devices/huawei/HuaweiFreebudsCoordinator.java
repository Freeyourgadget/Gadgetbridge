package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;

public abstract class HuaweiFreebudsCoordinator extends AbstractBLClassicDeviceCoordinator implements HuaweiCoordinatorSupplier {

    private final HuaweiCoordinator huaweiCoordinator = new HuaweiCoordinator(this);
    private GBDevice gbDevice;

    public HuaweiFreebudsCoordinator() {
        huaweiCoordinator.setTransactionCrypted(false);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        // TODO: implement
    }

    @Override
    public String getManufacturer() {
        return "Huawei";
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_nothingear;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_nothingear_disabled;
    }

    @Override
    public HuaweiCoordinator getHuaweiCoordinator() {
        return huaweiCoordinator;
    }

    @Override
    public HuaweiDeviceType getHuaweiType() {
        return HuaweiDeviceType.BR;
    }

    @Override
    public void setDevice(GBDevice gbDevice) {
        this.gbDevice = gbDevice;
    }

    @Override
    public GBDevice getDevice() {
        return this.gbDevice;
    }

    @Override
    public int getBondingStyle() {
        // TODO: Check if correct
        return BONDING_STYLE_ASK;
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(2, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(0, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(1, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(GBDevice device) {
        DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_huawei_freebuds);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }
}
