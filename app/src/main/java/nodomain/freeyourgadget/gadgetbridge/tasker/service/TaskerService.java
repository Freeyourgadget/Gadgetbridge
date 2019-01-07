package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTask;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Tasker service for scheduling task with specific thresholds.
 * <p>
 * One instance per thread/device! The service is not threadsafe.
 */
public class TaskerService {

    private SettingSupplier<Boolean> enabled;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Map<TaskerEventType, TaskerTask> tasks = new HashMap<>();
    private Map<TaskerEventType, SettingSupplier<Long>> threshold = new HashMap<>();
    private Map<TaskerEventType, SettingSupplier<TaskerTaskProvider>> typeProvider = new HashMap<>();
    private long defaultThreshold = 50L;

    // Builder

    public TaskerService() {
        this.enabled = new SettingSupplier<Boolean>() {
            @Override
            public Boolean get() {
                return GBApplication.getPrefs().getBoolean(TaskerConstants.TASKER_ENABLED, false);
            }

            @Override
            public boolean isPresent() {
                return true;
            }
        };
    }

    public TaskerService(SettingSupplier<Boolean> enabled) {
        this.enabled = enabled;
    }

    public TaskerService(boolean enabled) {
        this.enabled = new StaticSettingSupplier<>(enabled);
    }

    public TaskerService withThreshold(TaskerEventType type, long threshold) {
        this.threshold.put(type, new StaticSettingSupplier<>(threshold));
        return this;
    }

    public TaskerService withThreshold(TaskerEventType type, SettingSupplier<Long> threshold) {
        this.threshold.put(type, threshold);
        return this;
    }

    public TaskerService withTask(TaskerEventType type, final String task) {
        typeProvider.put(type, new StaticSettingSupplier<TaskerTaskProvider>(new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                return task;
            }
        }));
        return this;
    }

    public TaskerService withProvider(TaskerEventType type, TaskerTaskProvider provider) {
        typeProvider.put(type, new StaticSettingSupplier<>(provider));
        return this;
    }

    public TaskerService withProvider(TaskerEventType type, SettingSupplier<TaskerTaskProvider> provider) {
        typeProvider.put(type, provider);
        return this;
    }

    // Public

    public boolean isActive() {
        return enabled.get();
    }

    public boolean buttonPressed(int index) {
        return runForType(TaskerEventType.BUTTON.withIndex(index));
    }

    public boolean deviceConnected(int index) {
        return runForType(TaskerEventType.CONNECTION.withIndex(index));
    }

    public boolean dataReceived(int index) {
        return runForType(TaskerEventType.DATA.withIndex(index));
    }

    public boolean runForType(TaskerEventType type) {
        if (type != null && !TaskerEventType.NO_OP.equals(type) && isActive() && ready()) {
            if (!tasks.containsKey(type)) {
                if (taskProvider(type) != null) {
                    tasks.put(type, new TaskerTask(type, taskProvider(type), threshold(type)));
                }
            }
            tasks.get(type).schedule(executor);
            return true;
        }
        return false;
    }

    // Private

    private long threshold(TaskerEventType type) {
        return threshold.containsKey(type) && threshold.get(type).isPresent() ? threshold.get(type).get() : defaultThreshold;
    }

    private TaskerTaskProvider taskProvider(TaskerEventType type) {
        if (typeProvider.containsKey(type) && typeProvider.get(type).isPresent()) {
            return typeProvider.get(type).get();
        }
        throw new NoTaskDefinedException();
    }

    // Static

    public static boolean ready() {
        return TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK);
    }

    private static class StaticSettingSupplier<T> implements SettingSupplier<T> {

        private T setting;

        public StaticSettingSupplier(T setting) {
            this.setting = setting;
        }

        @Override
        public T get() {
            return setting;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

    }

}
