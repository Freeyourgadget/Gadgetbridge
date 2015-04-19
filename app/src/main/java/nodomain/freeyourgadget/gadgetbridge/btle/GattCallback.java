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

package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Callback interface handling gatt events.
 * Pretty much the same as {@link BluetoothGattCallback}, except it's an interface
 * instead of an abstract class. Some handlers commented out, because not used (yet).
 */
public interface GattCallback {

    /**
     * @param gatt
     * @param status
     * @param newState
     * @see BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)
     */
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * @param gatt
     * @see BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)
     */
    public void onServicesDiscovered(BluetoothGatt gatt);

    /**
     * @param gatt
     * @param characteristic
     * @param status
     * @see BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)
     */
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    /**
     * @param gatt
     * @param characteristic
     * @param status
     * @see BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)
     */
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status);

    /**
     * @param gatt
     * @param characteristic
     * @see BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     */
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic);

//    /**
//     * @see BluetoothGattCallback#onDescriptorRead(BluetoothGatt, BluetoothGattDescriptor, int)
//     * @param gatt
//     * @param descriptor
//     * @param status
//     */
//    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
//                                 int status);
//
//    /**
//     * @see BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)
//     * @param gatt
//     * @param descriptor
//     * @param status
//     */
//    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
//                                  int status);
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
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

//    /**
//     * @see BluetoothGattCallback#onMtuChanged(BluetoothGatt, int, int)
//     * @param gatt
//     * @param mtu
//     * @param status
//     */
//    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
}
