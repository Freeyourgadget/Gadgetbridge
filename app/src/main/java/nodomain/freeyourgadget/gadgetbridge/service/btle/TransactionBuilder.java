package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WriteAction;

public class TransactionBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);

    private Transaction mTransaction;

    public TransactionBuilder(String taskName) {
        mTransaction = new Transaction(taskName);
    }

    public TransactionBuilder read(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            LOG.warn("Unable to read characteristic: null");
            return this;
        }
        ReadAction action = new ReadAction(characteristic);
        return add(action);
    }

    public TransactionBuilder write(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (characteristic == null) {
            LOG.warn("Unable to write characteristic: null");
            return this;
        }
        WriteAction action = new WriteAction(characteristic, data);
        return add(action);
    }

    public TransactionBuilder notify(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (characteristic == null) {
            LOG.warn("Unable to notify characteristic: null");
            return this;
        }
        NotifyAction action = createNotifyAction(characteristic, enable);
        return add(action);
    }

    protected NotifyAction createNotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        return new NotifyAction(characteristic, enable);
    }

    public TransactionBuilder wait(int millis) {
        WaitAction action = new WaitAction(millis);
        return add(action);
    }

    public TransactionBuilder add(BtLEAction action) {
        mTransaction.add(action);
        return this;
    }

    /**
     * To be used as the final step to execute the transaction by the given queue.
     *
     * @param queue
     */
    public void queue(BtLEQueue queue) {
        queue.add(mTransaction);
    }

    public Transaction getTransaction() {
        return mTransaction;
    }
}
