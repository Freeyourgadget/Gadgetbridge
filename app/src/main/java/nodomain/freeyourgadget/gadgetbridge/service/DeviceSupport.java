/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
     * Attempts an initial connection to the device, typically after the user "discovered"
     * and connects to it for the first time. Some implementations may perform an additional
     * initialization or application-level pairing compared to the regular {@link #connect()}.
     * <p/>
     * Implementations may perform the connection in a synchronous or asynchronous way.
     * Returns true if a connection attempt was made. If the implementation is synchronous
     * it may also return true if the connection was successfully established, however
     * callers shall not rely on that.
     * <p/>
     * The actual connection state change (successful or not) will be reported via the
     * #getDevice device as a device change Intent.
     *
     * Note: the default implementation {@link AbstractDeviceSupport#connectFirstTime()} just
     * calls {@link #connect()}
     *
     * @see #connect()
     * @see GBDevice#ACTION_DEVICE_CHANGED
     */
    boolean connectFirstTime();

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
     * @see #connectFirstTime()
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
     * Configures this instance to automatically attempt to reconnect after a connection loss.
     * How, how long, or how often is up to the implementation.
     * Note that tome implementations may not support automatic reconnection at all.
     * @param enable
     */
    void setAutoReconnect(boolean enable);

    /**
     * Returns whether this instance to configured to automatically attempt to reconnect after a
     * connection loss.
     */
    boolean getAutoReconnect();

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
