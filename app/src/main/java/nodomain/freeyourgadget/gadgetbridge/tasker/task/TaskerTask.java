package nodomain.freeyourgadget.gadgetbridge.tasker.task;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerIntent;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.NoTaskDefinedException;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerService;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerUtil;

/**
 * Tasker task used by {@link TaskerService} to run a scheduled asynchronous task.
 * <p>
 * Uses {@link ScheduledExecutorService} with {@link Future} for scheduling.
 */
public class TaskerTask implements Runnable {

    private Future task;
    private int count;
    private long creation;
    private TaskerEventType type;
    private TaskerTaskProvider provider;
    private long threshold;
    private OnDoneListener onDone;

    public TaskerTask(TaskerEventType type, TaskerTaskProvider provider, long threshold, OnDoneListener onDone) {
        this.type = type;
        this.provider = provider;
        this.threshold = threshold;
        this.onDone = onDone;
    }

    public TaskerTask schedule(ScheduledExecutorService executeService) {
        if (System.currentTimeMillis() - creation < threshold && this.task != null && !this.task.isDone()) {
            this.count++;
            this.task.cancel(true);
        } else {
            this.count = 0;
            this.creation = System.currentTimeMillis();
        }
        this.task = executeService.schedule(this, threshold, TimeUnit.MILLISECONDS);
        return this;
    }

    @Override
    public void run() {
        try {
            if (TaskerService.isReady()) {
                if (task != null) {
                    GBApplication.getContext().sendBroadcast(new TaskerIntent(provider.getTask(new TaskerEvent(type, count))));
                }
            }
        } catch (NoTaskDefinedException e) {
            TaskerUtil.noTaskDefinedInformation();
        }
        onDone.done(this);
    }

    public int getCount() {
        return count;
    }

    public boolean cancel() {
        return task.cancel(true);
    }

    public interface OnDoneListener {
        void done(TaskerTask task);
    }

}
