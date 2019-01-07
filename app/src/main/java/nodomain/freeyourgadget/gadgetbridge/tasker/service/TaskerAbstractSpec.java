package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

public abstract class TaskerAbstractSpec implements TaskerSpec {

    @Override
    public TaskerSettings getTaskerSettings(TaskerEventType eventType) {
        return null;
    }
}
