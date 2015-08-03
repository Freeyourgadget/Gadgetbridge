package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

/**
 * Invokes a read operation on a given GATT characteristic.
 * The result will be made available asynchronously through the
 * {@link BluetoothGattCallback}
 */
public class ReadAction extends BtLEAction {

    public ReadAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic);
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        return gatt.readCharacteristic(getCharacteristic());
    }

    @Override
    public boolean expectsResult() {
        return true;
    }
}
