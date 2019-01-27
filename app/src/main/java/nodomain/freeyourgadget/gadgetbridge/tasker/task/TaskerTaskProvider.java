package nodomain.freeyourgadget.gadgetbridge.tasker.task;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;

/**
 * Tasker task provider provides task names {@link TaskerEvent} based.
 */
public interface TaskerTaskProvider {

    /**
     * Task name for specific {@link TaskerEvent}
     *
     * @param event {@link TaskerEvent}
     * @return Task name
     */
    String getTask(TaskerEvent event);

    /**
     * Add a task to the task provider and specific {@link TaskerEvent}.
     *
     * @param event {@link TaskerEvent}
     * @param task  Task name
     */
    void addTask(TaskerEvent event, String task);

}
