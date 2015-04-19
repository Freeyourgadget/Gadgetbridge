package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

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
}
