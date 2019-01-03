package nodomain.freeyourgadget.gadgetbridge.tasker.task;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;

public interface TaskerTaskProvider {

    String getTask(TaskerEvent event);

}
