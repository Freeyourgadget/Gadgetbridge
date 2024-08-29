package nodomain.freeyourgadget.gadgetbridge.devices.soundcore.liberty4_nc;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty4_nc.SoundcoreLiberty4NCDeviceSupport;

public class SoundcoreLiberty4NCCoordinator extends AbstractDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_soundcore_liberty4_nc;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_galaxy_buds_disabled;
    }

    @Override
    public String getManufacturer() {
        return "Anker";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("soundcore Liberty 4 NC");
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }


    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_buds_pro_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS, R.xml.devicesettings_sony_headphones_ambient_sound_control_button_modes);
        deviceSpecificSettings.addSubScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS, R.xml.devicesettings_soundcore_touch_options);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_soundcore_headphones);
        return deviceSpecificSettings;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return SoundcoreLiberty4NCDeviceSupport.class;
    }
}
