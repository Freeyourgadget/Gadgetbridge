package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

public interface TaskerSpec {

    TaskerEventType getEventType(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    TaskerSettings getTaskerSettings(TaskerEventType eventType);

}
