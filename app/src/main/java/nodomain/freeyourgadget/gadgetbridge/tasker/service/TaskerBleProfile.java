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

    private SpecTaskerService taskerService;
    private TaskerConstants.TaskerDevice taskerDevice;

    public TaskerBleProfile(T support, TaskerConstants.TaskerDevice taskerDevice) {
        super(support);
        this.taskerDevice = taskerDevice;
        taskerService = new SpecTaskerService(taskerDevice.getSpec());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        TaskerEventType eventType = taskerDevice.getSpec().getEventType(gatt, characteristic);
        if (TaskerEventType.NO_OP.equals(eventType)) {
            return false;
        }
        TaskerSettings settings = taskerDevice.getSpec().getSettings(eventType);
        if (settings.isEnabled().isPresent() && settings.isEnabled().get()) {
            boolean run = false;
            try {
                run = taskerService.runForType(eventType);
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

