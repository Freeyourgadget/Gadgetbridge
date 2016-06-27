package nodomain.freeyourgadget.gadgetbridge.devices;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class AbstractDeviceCoordinator implements DeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceCoordinator.class);

    public boolean allowFetchActivityData(GBDevice device) {
        return device.isInitialized() && !device.isBusy() && supportsActivityDataFetching();
    }

    public boolean isHealthWearable(BluetoothDevice device) {
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass == null) {
            LOG.warn("unable to determine bluetooth device class of " + device);
            return false;
        }
        if (bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE
            || bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED) {
            int deviceClasses =
                    BluetoothClass.Device.HEALTH_BLOOD_PRESSURE
                    | BluetoothClass.Device.HEALTH_DATA_DISPLAY
                    | BluetoothClass.Device.HEALTH_PULSE_RATE
                    | BluetoothClass.Device.HEALTH_WEIGHING
                    | BluetoothClass.Device.HEALTH_UNCATEGORIZED
                    | BluetoothClass.Device.HEALTH_PULSE_OXIMETER
                    | BluetoothClass.Device.HEALTH_GLUCOSE;

            return (bluetoothClass.getDeviceClass() & deviceClasses) != 0;
        }
        return false;
    }
}
