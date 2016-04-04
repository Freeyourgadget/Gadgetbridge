package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public abstract class ConditionalWriteAction extends WriteAction {
    public ConditionalWriteAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic, null);
    }

    @Override
    protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        byte[] conditionalValue = checkCondition();
        if (conditionalValue != null) {
            return super.writeValue(gatt, characteristic, conditionalValue);
        }
        return true;
    }

    /**
     * Checks the condition whether the write shall happen or not.
     * Returns the actual value to be written or null in case nothing shall be written.
     * <p/>
     * Note that returning null will not cause run() to return false, in other words,
     * the rest of the queue will still be executed.
     *
     * @return the value to be written or null to not write anything
     */
    protected abstract byte[] checkCondition();
}
