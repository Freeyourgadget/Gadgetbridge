package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class TransactionBuilder {
    private static final String TAG = TransactionBuilder.class.getSimpleName();

    private Transaction mTransaction;
    
    public TransactionBuilder(String taskName) {
        mTransaction = new Transaction(taskName);
    }
    
    public TransactionBuilder read(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.w(TAG, "Unable to read characteristic: null");
            return this;
        }
        ReadAction action = new ReadAction(characteristic);
        return add(action);
    }
    
    public TransactionBuilder write(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (characteristic == null) {
            Log.w(TAG, "Unable to write characteristic: null");
            return this;
        }
        WriteAction action = new WriteAction(characteristic, data);
        return add(action);
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
     * @param queue
     */
    public void queue(BtLEQueue queue) {
        queue.add(mTransaction);
    }

    public Transaction getTransaction() {
        return mTransaction;
    }
}
