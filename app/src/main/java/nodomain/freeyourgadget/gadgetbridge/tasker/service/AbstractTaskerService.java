package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerIntent;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplierImpl;
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
                return GBApplication.getPrefs().getBoolean(TaskerConstants.ACTIVITY_TASKER_ENABLED, false);
            }
        };
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Schedules tasker task for {@link TaskerEventType} if
     * {@link #isEnabled()},
     * {@link #isReady()} and
     * event type is not {@link TaskerEventType#NO_OP}.
     * <p>
     * Uses {@link #DEFAULT_THRESHOLD} of '50' milliseconds if threshold is not set.
     *
     * Not thread safe, but that should be no problem.
     *
     * @param type
     * @return
     */
    public boolean runForType(final TaskerEventType type) {
        if (type != null && !TaskerEventType.NO_OP.equals(type) && isEnabled() && isReady()) {
            if (!tasks.containsKey(type)) {
                SettingSupplier<TaskerTaskProvider> taskProvider = taskProvider(type);
                if (taskProvider.isPresent()) {
                    SettingSupplier<Long> threshold = threshold(type);
                    tasks.put(type, new TaskerTask(type, taskProvider.get(), threshold.isPresent() ? threshold.get() : DEFAULT_THRESHOLD, new TaskerTask.OnDoneListener() {
                        @Override
                        public void done(TaskerTask task) {
                            tasks.remove(type);
                        }
                    }));
                }
            }
            tasks.get(type).schedule(executor);
            return true;
        }
        return false;
    }

    protected abstract SettingSupplier<Long> threshold(TaskerEventType type);

    protected abstract SettingSupplier<TaskerTaskProvider> taskProvider(TaskerEventType type);

    /**
     * Determines of tasker is installed and ready.
     *
     * @return True if installed and ready
     */
    public static boolean isReady() {
        return TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK);
    }

}
