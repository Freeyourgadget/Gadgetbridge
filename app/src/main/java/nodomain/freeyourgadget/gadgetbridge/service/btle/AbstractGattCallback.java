/*  Copyright (C) 2015-2019 Carsten Pfeiffer

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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Base class for GattCallbacks wishing to just implement a few of the methods.
 */
public abstract class AbstractGattCallback implements GattCallback {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return false;
    }

    @Override
    public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        return false;
    }

    @Override
    public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        return false;
    }

    @Override
    public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        return false;
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    }
}
