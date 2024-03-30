package nodomain.freeyourgadget.gadgetbridge.devices.scannable;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.unknown.UnknownDeviceSupport;

public class ScannableDeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return false;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_scannable
        };
    }

    @Override
    public boolean isConnectable() {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "unknown";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return UnknownDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_scannable;
    }

    @Override
    public int getBatteryCount() {
        return 0;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_scannable;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_scannable_disabled;
    }
}
