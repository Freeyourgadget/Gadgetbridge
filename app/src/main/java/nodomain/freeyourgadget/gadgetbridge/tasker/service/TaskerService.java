package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplierImpl;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Default implementation for {@link AbstractTaskerService}.
 * <p>
 * Preferred to use with java based configuration.
 */
public class TaskerService extends AbstractTaskerService {

    private Map<TaskerEventType, SettingSupplier<Long>> threshold = new HashMap<>();
    private Map<TaskerEventType, SettingSupplier<TaskerTaskProvider>> typeProvider = new HashMap<>();

    public TaskerService(boolean enabled) {
        this.enabled = new SettingSupplierImpl<>(enabled);
    }

    /**
     * Set threshold between task calls for {@link TaskerEventType}.
     *
     * @param type      Event
     * @param threshold Threshold in milliseconds
     * @return Itself
     */
    public TaskerService withThreshold(TaskerEventType type, long threshold) {
        this.threshold.put(type, new SettingSupplierImpl<>(threshold));
        return this;
    }

    /**
     * Sets single task name for {@link TaskerEventType}.
     *
     * @param type Event
     * @param task Single task name
     * @return Itself
     */
    public TaskerService withTask(TaskerEventType type, final String task) {
        typeProvider.put(type, new SettingSupplierImpl<TaskerTaskProvider>(new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                return task;
            }
        }));
        return this;
    }

    /**
     * Sets {@link TaskerTaskProvider} for {@link TaskerEventType}.
     *
     * @param type     Event
     * @param provider Task name provider
     * @return Itself
     */
    public TaskerService withProvider(TaskerEventType type, TaskerTaskProvider provider) {
        typeProvider.put(type, new SettingSupplierImpl<>(provider));
        return this;
    }

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
