package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

/**
 * Tasker plugin hook as BLE profile.
 * <p>
 * Needs {@link TaskerSpec} for configuration.
 *
 * @param <T> Bluetooth LE support class extends {@link AbstractBTLEDeviceSupport}
 */
public class TaskerBleProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {

    private TaskerService taskerService;
    private TaskerSpec taskerSpec;

    public TaskerBleProfile(T support, TaskerSpec taskerSpec) {
        super(support);
        this.taskerSpec = taskerSpec;
        taskerService = new TaskerService(taskerSpec.isEnabled());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        TaskerEventType eventType = taskerSpec.getEventType(gatt, characteristic);
        if (TaskerEventType.NO_OP.equals(eventType)) {
            return false;
        }
        TaskerSettings settings = taskerSpec.getTaskerSettings(eventType);
        if (settings.isEnabled().isPresent() && settings.isEnabled().get()) {
            TaskerService service = taskerService;
            if (settings.getThreshold().isPresent()) {
                service = service.withThreshold(eventType, settings.getThreshold().get());
            }
            if (settings.getTaskProvider().isPresent()) {
                service = service.withProvider(eventType, settings.getTaskProvider().get());
            }
            boolean run = false;
            try {
                run = service.runForType(eventType);
            } catch (NoTaskDefinedException e) {
                TaskerUtil.noTaskDefinedInformation().show();
            }
            if (settings.isConsumingEvents().isPresent() && settings.isConsumingEvents().get()) {
                return run;
            }
        }
        return false;
    }

}

