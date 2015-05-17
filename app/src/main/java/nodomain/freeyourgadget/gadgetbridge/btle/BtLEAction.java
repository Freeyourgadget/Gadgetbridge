package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * The Bluedroid implementation only allows performing one GATT request at a time.
 * As they are asynchronous anyway, we encapsulate every GATT request (read and write)
 * inside a runnable action.
 * <p/>
 * These actions are then executed one after another, ensuring that every action's result
 * has been posted before invoking the next action.
 */
public abstract class BtLEAction {
    private final BluetoothGattCharacteristic characteristic;

    public BtLEAction() {
        this(null);
    }

    public BtLEAction(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    /**
     * Returns true if this actions expects an (async) result which must
     * be waited for, before continuing with other actions.
     *
     * This is needed because the current Bluedroid stack can only deal
     * with one single bluetooth operation at a time.
     */
    public abstract boolean expectsResult();

    /**
     * Executes this action, e.g. reads or write a GATT characteristic.
     * @param gatt the characteristic to manipulate, or null if none.
     * @return true if the action was successful, false otherwise
     */
    public abstract boolean run(BluetoothGatt gatt);

    /**
     * Returns the GATT characteristic being read/written/...
     *
     * @return the GATT characteristic, or <code>null</code>
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public String toString() {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        String uuid = characteristic == null ? "(null)" : characteristic.getUuid().toString();
        return getClass().getSimpleName() + " on characteristic: " + uuid;
    }
}
