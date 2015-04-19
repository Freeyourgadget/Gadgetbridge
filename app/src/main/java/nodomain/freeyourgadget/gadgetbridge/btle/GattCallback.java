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
     * @see BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)
     * @param gatt
     * @param status
     * @param newState
     */
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * @see BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)
     * @param gatt
     */
    public void onServicesDiscovered(BluetoothGatt gatt);

    /**
     * @see BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    /**
     * @see BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status);

    /**
     * @see BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     * @param gatt
     * @param characteristic
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
     * @see BluetoothGattCallback#onReadRemoteRssi(BluetoothGatt, int, int)
     * @param gatt
     * @param rssi
     * @param status
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
