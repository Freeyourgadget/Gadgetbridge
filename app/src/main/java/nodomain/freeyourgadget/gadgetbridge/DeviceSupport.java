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
    public void initialize(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context);

    public boolean isConnected();

    public boolean connect();

    public void dispose();

    public GBDevice getDevice();

    public BluetoothAdapter getBluetoothAdapter();

    public Context getContext();

    public boolean useAutoConnect();

    public void pair();
}
