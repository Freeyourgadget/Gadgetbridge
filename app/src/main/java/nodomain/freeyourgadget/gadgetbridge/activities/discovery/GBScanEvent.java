/*  Copyright (C) 2023-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.discovery;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import androidx.annotation.Nullable;

/**
 * A scan event originating from either BT or BLE scan. References the BluetoothDevice, rssi,
 * and service UUIDs, if any.
 */
public class GBScanEvent {
    private final BluetoothDevice device;
    private final short rssi;

    @Nullable
    private final ParcelUuid[] serviceUuids;

    public GBScanEvent(final BluetoothDevice device, final short rssi, @Nullable final ParcelUuid[] serviceUuids) {
        this.device = device;
        this.rssi = rssi;
        this.serviceUuids = serviceUuids;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public short getRssi() {
        return rssi;
    }

    @Nullable
    public ParcelUuid[] getServiceUuids() {
        return serviceUuids;
    }
}
