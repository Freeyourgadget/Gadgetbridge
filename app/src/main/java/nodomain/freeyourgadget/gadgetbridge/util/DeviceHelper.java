package nodomain.freeyourgadget.gadgetbridge.util;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.UnknownDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;

public class DeviceHelper {
    private static DeviceHelper instance = new DeviceHelper();

    public static DeviceHelper getInstance() {
        return instance;
    }

    // lazily created
    private List<DeviceCoordinator> coordinators;
    // the current single coordinator (typically there's just one device connected
    private DeviceCoordinator coordinator;

    public boolean isSupported(GBDeviceCandidate candidate) {
        if (coordinator != null && coordinator.supports(candidate)) {
            return true;
        }
        for (DeviceCoordinator coordinator : getAllCoordinators()) {
            if (coordinator.supports(candidate)) {
                return true;
            }
        }
        return false;
    }

    public GBDevice toSupportedDevice(BluetoothDevice device) {
        GBDeviceCandidate candidate = new GBDeviceCandidate(device, GBDevice.RSSI_UNKNOWN);
        if (coordinator != null && coordinator.supports(candidate)) {
            return new GBDevice(device.getAddress(), device.getName(), coordinator.getDeviceType());
        }
        for (DeviceCoordinator coordinator : getAllCoordinators()) {
            if (coordinator.supports(candidate)) {
                return new GBDevice(device.getAddress(), device.getName(), coordinator.getDeviceType());
            }
        }
        return null;
    }

    public DeviceCoordinator getCoordinator(GBDeviceCandidate device) {
        if (coordinator != null && coordinator.supports(device)) {
            return coordinator;
        }
        synchronized (this) {
            for (DeviceCoordinator coord : getAllCoordinators()) {
                if (coord.supports(device)) {
                    coordinator = coord;
                    return coordinator;
                }
            }
        }
        return new UnknownDeviceCoordinator();
    }

    public DeviceCoordinator getCoordinator(GBDevice device) {
        if (coordinator != null && coordinator.supports(device)) {
            return coordinator;
        }
        synchronized (this) {
            for (DeviceCoordinator coord : getAllCoordinators()) {
                if (coord.supports(device)) {
                    coordinator = coord;
                    return coordinator;
                }
            }
        }
        return new UnknownDeviceCoordinator();
    }

    public synchronized List<DeviceCoordinator> getAllCoordinators() {
        if (coordinators == null) {
            coordinators = createCoordinators();
        }
        return coordinators;
    }

    private List<DeviceCoordinator> createCoordinators() {
        List<DeviceCoordinator> result = new ArrayList<>(2);
        result.add(new MiBandCoordinator());
        result.add(new PebbleCoordinator());
        return result;
    }
}
