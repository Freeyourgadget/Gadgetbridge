package nodomain.freeyourgadget.gadgetbridge.btle;

/**
 * An abstract non-BTLE action. It performs no bluetooth operation,
 * does not have a BluetoothGattCharacteristic instance and expects no result.
 */
public abstract class PlainAction extends BtLEAction {

    public PlainAction() {
        super(null);
    }

    @Override
    public boolean expectsResult() {
        return false;
    }
}
