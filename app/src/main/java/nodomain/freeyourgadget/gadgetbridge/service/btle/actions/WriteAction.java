package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

/**
 * Invokes a write operation on a given GATT characteristic.
 * The result status will be made available asynchronously through the
 * {@link BluetoothGattCallback}
 */
public class WriteAction extends BtLEAction {

    private byte[] value;

    public WriteAction(BluetoothGattCharacteristic characteristic, byte[] value) {
        super(characteristic);
        this.value = value;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        int properties = getCharacteristic().getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            if (getCharacteristic().setValue(value)) {
                return gatt.writeCharacteristic(getCharacteristic());
            }
        }
        return false;
    }

    @Override
    public boolean expectsResult() {
        return true;
    }
}
