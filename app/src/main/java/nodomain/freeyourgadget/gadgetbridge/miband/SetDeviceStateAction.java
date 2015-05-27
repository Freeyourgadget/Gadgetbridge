package nodomain.freeyourgadget.gadgetbridge.miband;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.btle.BtLEAction;

public class SetDeviceStateAction extends BtLEAction {
    private final GBDevice device;
    private final GBDevice.State deviceState;
    private final Context context;

    public SetDeviceStateAction(GBDevice device, GBDevice.State deviceState, Context context) {
        this.device = device;
        this.deviceState = deviceState;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        device.setState(deviceState);
        device.sendDeviceUpdateIntent(getContext());
        return true;
    }

    @Override
    public boolean expectsResult() {
        return false;
    }

    public Context getContext() {
        return context;
    }
}
