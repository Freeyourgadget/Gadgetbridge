package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.spec.TaskerSpec;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * {@link TaskerSpec} implementation for {@link TaskerService}.
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
