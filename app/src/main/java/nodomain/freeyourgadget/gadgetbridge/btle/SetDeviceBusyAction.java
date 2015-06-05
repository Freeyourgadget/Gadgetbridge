package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBDevice;

public class SetDeviceBusyAction extends PlainAction {
    private final GBDevice device;
    private final Context context;
    private final String busyTask;

    /**
     * When run, will mark the device as busy (or not busy).
     * @param device the device to mark
     * @param busyTask the task name to set as busy task, or null to mark as not busy
     * @param context
     */
    public SetDeviceBusyAction(GBDevice device, String busyTask, Context context) {
        this.device = device;
        this.busyTask = busyTask;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        device.setBusyTask(busyTask);
        device.sendDeviceUpdateIntent(context);
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + busyTask;
    }
}
