package nodomain.freeyourgadget.gadgetbridge.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.UnknownDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class DeviceHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceHelper.class);

    private static final DeviceHelper instance = new DeviceHelper();

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

    public GBDevice findAvailableDevice(String deviceAddress, Context context) {
        Set<GBDevice> availableDevices = getAvailableDevices(context);
        for (GBDevice availableDevice : availableDevices) {
            if (deviceAddress.equals(availableDevice.getAddress())) {
                return availableDevice;
            }
        }
        return null;
    }

    public Set<GBDevice> getAvailableDevices(Context context) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<GBDevice> availableDevices = new LinkedHashSet<GBDevice>();

        if (btAdapter == null) {
            GB.toast(context, context.getString(R.string.bluetooth_is_not_supported_), Toast.LENGTH_SHORT, GB.WARN);
        } else if (!btAdapter.isEnabled()) {
            GB.toast(context, context.getString(R.string.bluetooth_is_disabled_), Toast.LENGTH_SHORT, GB.WARN);
        } else {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            DeviceHelper deviceHelper = DeviceHelper.getInstance();
            for (BluetoothDevice pairedDevice : pairedDevices) {
                GBDevice device = deviceHelper.toSupportedDevice(pairedDevice);
                if (device != null) {
                    availableDevices.add(device);
                }
            }

            Prefs prefs = GBApplication.getPrefs();
            String miAddr = prefs.getString(MiBandConst.PREF_MIBAND_ADDRESS, "");
            if (miAddr.length() > 0) {
                GBDevice miDevice = new GBDevice(miAddr, "MI", DeviceType.MIBAND);
                if (!availableDevices.contains(miDevice)) {
                    availableDevices.add(miDevice);
                }
            }

            String pebbleEmuAddr = prefs.getString("pebble_emu_addr", "");
            String pebbleEmuPort = prefs.getString("pebble_emu_port", "");
            if (pebbleEmuAddr.length() >= 7 && pebbleEmuPort.length() > 0) {
                GBDevice pebbleEmuDevice = new GBDevice(pebbleEmuAddr + ":" + pebbleEmuPort, "Pebble qemu", DeviceType.PEBBLE);
                availableDevices.add(pebbleEmuDevice);
            }
        }
        return availableDevices;
    }

    public GBDevice toSupportedDevice(BluetoothDevice device) {
        GBDeviceCandidate candidate = new GBDeviceCandidate(device, GBDevice.RSSI_UNKNOWN);

        String deviceName = device.getName();
        try {
            Method method = device.getClass().getMethod("getAliasName");
            if (method != null) {
                deviceName = (String) method.invoke(device);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
            LOG.info("Could not get device alias for " + deviceName);
        }

        if (coordinator != null && coordinator.supports(candidate)) {
            return new GBDevice(device.getAddress(), deviceName, coordinator.getDeviceType());
        }
        for (DeviceCoordinator coordinator : getAllCoordinators()) {
            if (coordinator.supports(candidate)) {
                return new GBDevice(device.getAddress(), deviceName, coordinator.getDeviceType());
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
