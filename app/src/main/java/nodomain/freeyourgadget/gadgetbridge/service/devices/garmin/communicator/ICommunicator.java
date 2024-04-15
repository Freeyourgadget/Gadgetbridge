package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public interface ICommunicator {
    void sendMessage(byte[] message);

    void onMtuChanged(final int mtu);

    void initializeDevice(TransactionBuilder builder);

    boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    interface Callback {
        void onMessage(byte[] message);
    }
}
