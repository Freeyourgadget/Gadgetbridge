package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables or disables notifications for a given GATT characteristic.
 * The result will be made available asynchronously through the
 * {@link BluetoothGattCallback}.
 */
public class NotifyAction extends BtLEAction {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);
    protected final boolean enableFlag;

    public NotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        super(characteristic);
        enableFlag = enable;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        return gatt.setCharacteristicNotification(getCharacteristic(), enableFlag);
    }

    @Override
    public boolean expectsResult() {
        return false;
    }
}
