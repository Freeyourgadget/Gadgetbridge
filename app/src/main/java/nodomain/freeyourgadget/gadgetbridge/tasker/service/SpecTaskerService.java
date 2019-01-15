package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * {@link TaskerSpec} impl for {@link AbstractTaskerService}.
 */
public class SpecTaskerService extends AbstractTaskerService {

    private TaskerSpec spec;

    public SpecTaskerService(TaskerSpec spec) {
        this.spec = spec;
    }

    @Override
    protected SettingSupplier<Long> threshold(TaskerEventType type) {
        return spec.getSettings(type).getThreshold();
    }

    @Override
    protected SettingSupplier<TaskerTaskProvider> taskProvider(TaskerEventType type) {
        return spec.getSettings(type).getTaskProvider();
    }
}
