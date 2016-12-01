package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

/**
 * Invokes a write operation on a given GATT characteristic.
 * The result status will be made available asynchronously through the
 * {@link BluetoothGattCallback}
 */
public class WriteAction extends BtLEAction {
    private static final Logger LOG = LoggerFactory.getLogger(WriteAction.class);

    private final byte[] value;

    public WriteAction(BluetoothGattCharacteristic characteristic, byte[] value) {
        super(characteristic);
        this.value = value;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        int properties = characteristic.getProperties();
        //TODO: expectsResult should return false if PROPERTY_WRITE_NO_RESPONSE is true, but this leads to timing issues
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 || ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)) {
            return writeValue(gatt, characteristic, value);
        }
        return false;
    }

    protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("writing to characteristic: " + characteristic.getUuid() + ": " + Logging.formatBytes(value));
        }
        if (characteristic.setValue(value)) {
            return gatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    protected final byte[] getValue() {
        return value;
    }

    @Override
    public boolean expectsResult() {
        return true;
    }
}
