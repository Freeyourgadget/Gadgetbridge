package nodomain.freeyourgadget.gadgetbridge.tasker.task;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;

/**
 * Tasker task provider provides task names {@link TaskerEvent} based.
 */
public interface TaskerTaskProvider {

    /**
     * Task name for specific {@link TaskerEvent}
     *
     * @param event
     * @return Task name
     */
    String getTask(TaskerEvent event);

}
