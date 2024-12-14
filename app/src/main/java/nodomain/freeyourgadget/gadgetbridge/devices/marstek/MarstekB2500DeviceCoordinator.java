package nodomain.freeyourgadget.gadgetbridge.devices.marstek;


import androidx.annotation.NonNull;


import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.marstek.MarstekB2500DeviceSupport;


public class MarstekB2500DeviceCoordinator extends AbstractDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_marstek_b2500;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_vesc;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_vesc_disabled;
    }

    @Override
    public String getManufacturer() {
        return "Marstek";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MarstekB2500DeviceSupport.class;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("HM_B2500_.*");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_battery_minimum_charge,
                R.xml.devicesettings_battery_discharge_5
        };
    }
}
