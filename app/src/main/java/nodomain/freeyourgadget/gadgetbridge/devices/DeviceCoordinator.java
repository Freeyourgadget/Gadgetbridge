package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;

import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface DeviceCoordinator {
    String EXTRA_DEVICE_MAC_ADDRESS = "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_MAC_ADDRESS";

    boolean supports(GBDeviceCandidate candidate);

    boolean supports(GBDevice device);

    DeviceType getDeviceType();

    Class<? extends Activity> getPairingActivity();

    Class<? extends Activity> getPrimaryActivity();

    SampleProvider getSampleProvider();
}
