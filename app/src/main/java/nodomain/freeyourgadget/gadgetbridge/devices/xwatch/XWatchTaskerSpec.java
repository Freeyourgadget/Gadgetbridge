package nodomain.freeyourgadget.gadgetbridge.devices.xwatch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.spec.AbstractTaskerSpec;

public class XWatchTaskerSpec extends AbstractTaskerSpec {

    protected XWatchTaskerSpec(TaskerDevice device) {
        super(device);
    }

    @Override
    public TaskerEventType getEventType(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (XWatchService.UUID_NOTIFY.equals(characteristic.getUuid())) {
            byte[] data = characteristic.getValue();
            if (data[0] == XWatchService.COMMAND_ACTIVITY_DATA) {
                return TaskerEventType.DATA;
            }
            if (data[0] == XWatchService.COMMAND_ACTION_BUTTON) {
                return TaskerEventType.BUTTON;
            }
            if (data[0] == XWatchService.COMMAND_CONNECTED) {
                return TaskerEventType.CONNECTION;
            }
        }
        return TaskerEventType.NO_OP;
    }

    @Override
    public List<TaskerEventType> getSupportedTypes() {
        return Arrays.asList(TaskerEventType.BUTTON, TaskerEventType.DATA, TaskerEventType.CONNECTION);
    }

}
