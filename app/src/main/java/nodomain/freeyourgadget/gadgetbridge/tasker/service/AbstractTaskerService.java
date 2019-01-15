package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplierImpl;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTask;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Tasker service for scheduling task with specific thresholds.
 * <p>
 * One instance per thread/device! The service is not threadsafe.
 */
public abstract class AbstractTaskerService {

    protected SettingSupplier<Boolean> enabled;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Map<TaskerEventType, TaskerTask> tasks = new HashMap<>();
    private static final long DEFAULT_THRESHOLD = 50L;

    public AbstractTaskerService() {
        this.enabled = new SettingSupplierImpl<Boolean>() {
            @Override
            public Boolean get() {
                return GBApplication.getPrefs().getBoolean(TaskerConstants.TASKER_ENABLED, false);
            }
        };
    }

    public boolean isActive() {
        return enabled.get();
    }

    public boolean runForType(TaskerEventType type) {
        if (type != null && !TaskerEventType.NO_OP.equals(type) && isActive() && ready()) {
            if (!tasks.containsKey(type)) {
                SettingSupplier<TaskerTaskProvider> taskProvider = taskProvider(type);
                if (taskProvider.isPresent()) {
                    SettingSupplier<Long> threshold = threshold(type);
                    tasks.put(type, new TaskerTask(type, taskProvider.get(), threshold.isPresent() ? threshold.get() : DEFAULT_THRESHOLD));
                }
            }
            tasks.get(type).schedule(executor);
            return true;
        }
        return false;
    }

    protected abstract SettingSupplier<Long> threshold(TaskerEventType type);

    protected abstract SettingSupplier<TaskerTaskProvider> taskProvider(TaskerEventType type);

    public static boolean ready() {
        return TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK);
    }

}
