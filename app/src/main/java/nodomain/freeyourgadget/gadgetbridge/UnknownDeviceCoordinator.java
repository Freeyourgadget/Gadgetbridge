package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;

public class UnknownDeviceCoordinator implements DeviceCoordinator {
    @Override
    public boolean supports(DeviceCandidate candidate) {
        return false;
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.UNKNOWN;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return ControlCenter.class;
    }
}
