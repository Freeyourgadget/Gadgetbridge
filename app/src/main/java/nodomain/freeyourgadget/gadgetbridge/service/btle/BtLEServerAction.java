/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/**
 * The Bluedroid implementation only allows performing one GATT request at a time.
 * As they are asynchronous anyway, we encapsulate every GATT request (read and write)
 * inside a runnable action.
 * <p/>
 * These actions are then executed one after another, ensuring that every action's result
 * has been posted before invoking the next action.
 */
public abstract class BtLEServerAction {
    private final BluetoothDevice device;
    private final long creationTimestamp;

    public BtLEServerAction(BluetoothDevice device) {
        this.device = device;
        creationTimestamp = System.currentTimeMillis();
    }


    public BluetoothDevice getDevice() {
        return this.device;
    }

    /**
     * Returns true if this action expects an (async) result which must
     * be waited for, before continuing with other actions.
     * <p/>
     * This is needed because the current Bluedroid stack can only deal
     * with one single bluetooth operation at a time.
     */
    public abstract boolean expectsResult();

    /**
     * Executes this action, e.g. reads or write a GATT characteristic.
     *
     * @return true if the action was successful, false otherwise
     */
    public abstract boolean run(BluetoothGattServer server);


    protected String getCreationTime() {
        return DateTimeUtils.formatDateTime(new Date(creationTimestamp));
    }

    public String toString() {
        return getCreationTime() + ":" + getClass().getSimpleName() + " on device: " + getDevice().getAddress();
    }
}
