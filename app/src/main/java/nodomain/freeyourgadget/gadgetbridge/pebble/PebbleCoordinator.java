package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;

public class PebbleCoordinator implements DeviceCoordinator {
    @Override
    public boolean supports(DeviceCandidate candidate) {
        return candidate.getName().startsWith("Pebble");
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.PEBBLE;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }
}
