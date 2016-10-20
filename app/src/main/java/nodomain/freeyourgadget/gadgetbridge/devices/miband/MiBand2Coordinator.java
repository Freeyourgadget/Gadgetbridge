package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class MiBand2Coordinator extends MiBandCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand2Coordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND2;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        // and a heuristic
        try {
            BluetoothDevice device = candidate.getDevice();
            if (isHealthWearable(device)) {
                String name = device.getName();
                return name != null && name.equalsIgnoreCase(MiBandConst.MI_BAND2_NAME);
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return false;

    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false; // not yet
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false; // not yet
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null; // not supported at the moment
    }
}
