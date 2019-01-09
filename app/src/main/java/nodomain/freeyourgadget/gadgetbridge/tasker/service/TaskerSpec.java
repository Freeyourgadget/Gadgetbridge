package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

public interface TaskerSpec {

    TaskerEventType getEventType(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    List<TaskerEventType> getSupportedTypes();

    TaskerSettings getTaskerSettings(TaskerEventType eventType);

}
