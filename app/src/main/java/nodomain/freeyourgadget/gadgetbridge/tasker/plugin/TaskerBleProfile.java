package nodomain.freeyourgadget.gadgetbridge.tasker.plugin;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.NoTaskDefinedException;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.SpecTaskerService;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerUtil;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;
import nodomain.freeyourgadget.gadgetbridge.tasker.spec.TaskerSpec;

/**
 * Tasker plugin hook as BLE profile.
 * <p>
 * Needs {@link TaskerSpec} for configuration.
 *
 * @param <T> Bluetooth LE support class extends {@link AbstractBTLEDeviceSupport}
 */
public class TaskerBleProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {

    private SpecTaskerService taskerService;
    private TaskerDevice taskerDevice;

    public TaskerBleProfile(T support, TaskerDevice taskerDevice) {
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
                TaskerUtil.noTaskDefinedInformation();
            }
            if (settings.isConsumeEvent().isPresent() && settings.isConsumeEvent().get()) {
                return run;
            }
        }
        return false;
    }

}

