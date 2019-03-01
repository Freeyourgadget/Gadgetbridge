package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;

public interface GattServerCallback {

    /**
     * @param device
     * @param status
     * @param newState
     * @see BluetoothGattServerCallback#onConnectionStateChange(BluetoothDevice, int, int)
     */
    void onConnectionStateChange(BluetoothDevice device, int status, int newState);

    /**
     * @param device
     * @param requestId
     * @param offset
     * @param characteristic
     * @see BluetoothGattServerCallback#onCharacteristicReadRequest(BluetoothDevice, int, int, BluetoothGattCharacteristic)
     */
    boolean onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic);

    /**
     * @param device
     * @param requestId
     * @param characteristic
     * @param preparedWrite
     * @param responseNeeded
     * @param offset
     * @param value
     * @see BluetoothGattServerCallback#onCharacteristicWriteRequest(BluetoothDevice, int, BluetoothGattCharacteristic, boolean, boolean, int, byte[])
     */
    boolean onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);

    /**
     * @param device
     * @param requestId
     * @param offset
     * @param descriptor
     * @see BluetoothGattServerCallback#onDescriptorReadRequest(BluetoothDevice, int, int, BluetoothGattDescriptor)
     */
    boolean onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor);

    /**
     * @param device
     * @param requestId
     * @param descriptor
     * @param preparedWrite
     * @param responseNeeded
     * @param offset
     * @param value
     * @see BluetoothGattServerCallback#onDescriptorWriteRequest(BluetoothDevice, int, BluetoothGattDescriptor, boolean, boolean, int, byte[])
     */
    boolean onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);

}
