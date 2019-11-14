package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import androidx.annotation.RequiresApi;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

public class RequestMtuAction extends BtLEAction {
    private int mtu;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RequestMtuAction(int mtu) {
        super(null);
        this.mtu = mtu;
    }


    @Override
    public boolean expectsResult() {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean run(BluetoothGatt gatt) {
        return gatt.requestMtu(this.mtu);
    }
}
