package nodomain.freeyourgadget.gadgetbridge.devices.pavlok;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pavlok.PavlokSupport;

public class PavlokCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Pavlok-.*");
    }

    @Override
    public String getManufacturer() {
        return "Pavlok";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return PavlokSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_pavlok;
    }
}
