package nodomain.freeyourgadget.gadgetbridge.service.devices.gatt_client;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;

public class BleGattClientCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return BleGattClientSupport.class;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        // can only add through debug settings
        return false;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ble_gatt_client;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_scannable;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_scannable_disabled;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }
}
