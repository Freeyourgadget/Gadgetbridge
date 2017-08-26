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
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Callback interface handling gatt events.
 * Pretty much the same as {@link BluetoothGattCallback}, except it's an interface
 * instead of an abstract class. Some handlers commented out, because not used (yet).
 *
 * Note: the boolean return values indicate whether this callback "consumed" this event
 * or not. True means, the event was consumed by this instance and no further instances
 * shall be notified. Fallse means, this instance could not handle the event.
 */
public interface GattCallback {

    /**
     * @param gatt
     * @param status
     * @param newState
     * @see BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)
     */
    void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * @param gatt
     * @see BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)
     */
    void onServicesDiscovered(BluetoothGatt gatt);

    /**
     * @param gatt
     * @param characteristic
     * @param status
     * @see BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)
     */
    boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    /**
     * @param gatt
     * @param characteristic
     * @param status
     * @see BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)
     */
    boolean onCharacteristicWrite(BluetoothGatt gatt,
                               BluetoothGattCharacteristic characteristic, int status);

    /**
     * @param gatt
     * @param characteristic
     * @see BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     */
    boolean onCharacteristicChanged(BluetoothGatt gatt,
                                 BluetoothGattCharacteristic characteristic);

    /**
     * @param gatt
     * @param descriptor
     * @param status
     * @see BluetoothGattCallback#onDescriptorRead(BluetoothGatt, BluetoothGattDescriptor, int)
     */
    boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                          int status);

    /**
     * @param gatt
     * @param descriptor
     * @param status
     * @see BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)
     */
    boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                           int status);
//
//    /**
//     * @see BluetoothGattCallback#onReliableWriteCompleted(BluetoothGatt, int)
//     * @param gatt
//     * @param status
//     */
//    public void onReliableWriteCompleted(BluetoothGatt gatt, int status);

    /**
     * @param gatt
     * @param rssi
     * @param status
     * @see BluetoothGattCallback#onReadRemoteRssi(BluetoothGatt, int, int)
     */
    void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

//    /**
//     * @see BluetoothGattCallback#onMtuChanged(BluetoothGatt, int, int)
//     * @param gatt
//     * @param mtu
//     * @param status
//     */
//    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
}
