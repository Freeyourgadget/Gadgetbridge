package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

public abstract class TaskerAbstractSpec implements TaskerSpec {

    @Override
    public TaskerSettings getSettings(TaskerEventType eventType) {
        return null;
    }
}
