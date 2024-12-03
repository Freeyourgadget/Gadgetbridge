package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public interface ICommunicator {
    void sendMessage(String taskName, byte[] message);

    void onMtuChanged(final int mtu);

    boolean initializeDevice(TransactionBuilder builder);

    boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    void onHeartRateTest();

    void onEnableRealtimeHeartRateMeasurement(final boolean enable);

    void onEnableRealtimeSteps(final boolean enable);

    interface Callback {
        void onMessage(byte[] message);
    }
}
