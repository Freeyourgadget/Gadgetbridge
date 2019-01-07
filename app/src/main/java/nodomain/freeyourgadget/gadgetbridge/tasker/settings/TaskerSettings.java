package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

public interface TaskerSettings {

    SettingSupplier<Boolean> isConsumingEvents();

    SettingSupplier<Boolean> isEnabled();

    SettingSupplier<Long> getThreshold();

    SettingSupplier<TaskerTaskProvider> getTaskProvider();

}
