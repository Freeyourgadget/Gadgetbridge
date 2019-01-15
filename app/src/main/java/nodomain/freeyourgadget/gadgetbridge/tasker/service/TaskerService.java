package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplierImpl;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTask;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Default impl for {@link AbstractTaskerService}.
 * <p>
 * One instance per thread/device! The service is not threadsafe.
 */
public class TaskerService extends AbstractTaskerService {

    private Map<TaskerEventType, SettingSupplier<Long>> threshold = new HashMap<>();
    private Map<TaskerEventType, SettingSupplier<TaskerTaskProvider>> typeProvider = new HashMap<>();

    public TaskerService(boolean enabled) {
        this.enabled = new SettingSupplierImpl<>(enabled);
    }

    public TaskerService withThreshold(TaskerEventType type, long threshold) {
        this.threshold.put(type, new SettingSupplierImpl<>(threshold));
        return this;
    }

    public TaskerService withTask(TaskerEventType type, final String task) {
        typeProvider.put(type, new SettingSupplierImpl<TaskerTaskProvider>(new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                return task;
            }
        }));
        return this;
    }

    public TaskerService withProvider(TaskerEventType type, TaskerTaskProvider provider) {
        typeProvider.put(type, new SettingSupplierImpl<>(provider));
        return this;
    }

    // Private

    protected SettingSupplier<Long> threshold(TaskerEventType type) {
        return threshold.get(type);
    }

    protected SettingSupplier<TaskerTaskProvider> taskProvider(TaskerEventType type) {
        if (typeProvider.containsKey(type) && typeProvider.get(type).isPresent()) {
            return typeProvider.get(type);
        }
        throw new NoTaskDefinedException();
    }


}
