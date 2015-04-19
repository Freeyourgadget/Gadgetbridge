package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;

public class WaitAction extends BtLEAction {

    private int mMillis;

    public WaitAction(int millis) {
        mMillis = millis;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        try {
            Thread.sleep(mMillis);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
