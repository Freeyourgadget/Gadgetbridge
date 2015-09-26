package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.devices.EventHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * Provides the communication support for a specific device. Instances will <b>only</b>
 * be used inside the DeviceCommunicationService. Has hooks to manage the life cycle
 * of a device: instances of this interface will be created, initialized, and disposed
 * as needed.
 * <p/>
 * Implementations need to act accordingly, in order to establish, reestablish or close
 * the connection to the device.
 * <p/>
 * In principle, this interface is agnostic to the kind of transport, i.e. whether the
 * device is connected via Bluetooth, Bluetooth LE, Wifi or something else, however at the
 * moment, only the BluetoothAdapter is passed to implementations.
 */
public interface DeviceSupport extends EventHandler {
    /**
     * Sets all context information needed for the instance to operate.
     *
     * @param gbDevice  the device to operate with
     * @param btAdapter the bluetooth adapter to use
     * @param context   the android context, e.g. to look up resources
     */
    void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context);

    /**
     * Returns whether a transport-level connection is established with the device
     *
     * @return whether the device is connected with the system running this software
     */
    boolean isConnected();

    /**
     * Attempts to establish a connection to the device. Implementations may perform
     * the connection in a synchronous or asynchronous way.
     * Returns true if a connection attempt was made. If the implementation is synchronous
     * it may also return true if the connection was successfully established, however
     * callers shall not rely on that.
     * <p/>
     * The actual connection state change (successful or not) will be reported via the
     * #getDevice device as a device change Intent.
     *
     * @see GBDevice#ACTION_DEVICE_CHANGED
     */
    boolean connect();

    /**
     * Disposes of this instance, closing all connections and freeing all resources.
     * Instances will not be reused after having been disposed.
     */
    void dispose();

    /**
     * Returns true if a connection attempt shall be made automatically whenever
     * needed (e.g. when a notification shall be sent to the device while not connected.
     */
    boolean useAutoConnect();

    /**
     * Attempts to pair and connect this device with the gadget device. Success
     * will be reported via a device change Intent.
     *
     * @see GBDevice#ACTION_DEVICE_CHANGED
     */
    void pair();

    /**
     * Returns the associated device this instance communicates with.
     */
    GBDevice getDevice();

    /**
     * Returns the bluetooth adapter. When we support different transports
     * than Bluetooth, we should use a generic type T and rename this method
     * to getTransportAdapter()
     */
    BluetoothAdapter getBluetoothAdapter();

    /**
     * Returns the Android context to use, e.g. to look up resources.
     */
    Context getContext();
}
