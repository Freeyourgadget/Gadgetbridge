package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

/**
 * Provides support for a specific device. Has hooks to manage the life cycle
 * of a device: instances of this interface will be created, initialized, and disposed
 * as needed.
 * <p/>
 * Implementations need to act accordingly, in order to establish, reestablish or close
 * the connection to the device.
 * <p/>
 * This interface is agnostic to the kind transport, i.e. whether the device is connected
 * via Bluetooth, Bluetooth LE, Wifi or something else.
 */
public interface DeviceSupport extends EventHandler {
    void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context);

    boolean isConnected();

    boolean connect();

    void dispose();

    GBDevice getDevice();

    BluetoothAdapter getBluetoothAdapter();

    Context getContext();

    boolean useAutoConnect();

    void pair();
}
