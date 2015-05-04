package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;

public interface DeviceCoordinator {
    String EXTRA_DEVICE_MAC_ADDRESS = "nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate.EXTRA_MAC_ADDRESS";

    public boolean supports(DeviceCandidate candidate);
    public boolean supports(GBDevice device);
    public DeviceType getDeviceType();

    Class<? extends Activity> getPairingActivity();
}
