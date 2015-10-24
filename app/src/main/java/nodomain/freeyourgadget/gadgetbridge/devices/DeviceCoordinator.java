package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public interface DeviceCoordinator {
    String EXTRA_DEVICE_MAC_ADDRESS = "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_MAC_ADDRESS";

    boolean supports(GBDeviceCandidate candidate);

    boolean supports(GBDevice device);

    DeviceType getDeviceType();

    Class<? extends Activity> getPairingActivity();

    Class<? extends Activity> getPrimaryActivity();

    SampleProvider getSampleProvider();

    InstallHandler findInstallHandler(Uri uri, Context context);

    boolean allowFetchActivityData(GBDevice device);

    boolean supportsActivityDataFetching();

    boolean supportsScreenshots();
}
