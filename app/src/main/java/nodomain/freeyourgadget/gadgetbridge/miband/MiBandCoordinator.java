package nodomain.freeyourgadget.gadgetbridge.miband;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;

public class MiBandCoordinator implements DeviceCoordinator {
    @Override
    public boolean supports(DeviceCandidate candidate) {
        return candidate.getMacAddress().toUpperCase().startsWith(MiBandService.MAC_ADDRESS_FILTER);
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return MiBandPairingActivity.class;
    }
}
