package nodomain.freeyourgadget.gadgetbridge.tasker.spec;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

/**
 * Tasker specification for a {@link TaskerConstants.TaskerDevice}.
 * <p>
 * First and only thing to do if you want to support more {@link nodomain.freeyourgadget.gadgetbridge.impl.GBDevice}.
 */
public interface TaskerSpec {

    TaskerEventType getEventType(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    List<TaskerEventType> getSupportedTypes();

    TaskerSettings getSettings(TaskerEventType eventType);

}
