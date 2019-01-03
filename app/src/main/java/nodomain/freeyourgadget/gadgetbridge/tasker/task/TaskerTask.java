package nodomain.freeyourgadget.gadgetbridge.tasker.task;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerIntent;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerService;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;

public class TaskerTask implements Runnable {

    private Future task;
    private int count;
    private long creation;
    private TaskerEventType type;
    private TaskerTaskProvider provider;
    private long threshold;

    public TaskerTask(TaskerEventType type, TaskerTaskProvider provider, long threshold) {
        this.type = type;
        this.provider = provider;
        this.threshold = threshold;
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
        if (TaskerService.ready()) {
            if (task != null) {
                GBApplication.getContext().sendBroadcast(new TaskerIntent(provider.getTask(new TaskerEvent(type, count))));
            }
        }
    }

    public int getCount() {
        return count;
    }

    public boolean cancel() {
        return task.cancel(true);
    }

}
