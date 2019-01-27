package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import nodomain.freeyourgadget.gadgetbridge.tasker.spec.TaskerSpec;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Tasker settings. There is one setting per {@link nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType}.
 * This is usually wrapped in {@link TaskerSpec}
 * and used by {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventActivity} to let the user customize the settings.
 * {@link nodomain.freeyourgadget.gadgetbridge.tasker.service.SpecTaskerService} is the out of the box implementation that uses this settings to call tasker.
 * <p>
 * Extend here for more settings.
 */
public interface TaskerSettings {

    /**
     * Consumes events or just listens to them.
     *
     * @return True if consumes events.
     */
    SettingSupplier<Boolean> isConsumeEvent();

    /**
     * Enables the settings.
     *
     * @return True if settings are enabled.
     */
    SettingSupplier<Boolean> isEnabled();

    /**
     * Threshold for tasker calls. Determines the delay between {@link nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent}'s
     * and is therefore the main attribute to determine which tasker task is called.
     *
     * @return Threshold in milliseconds
     */
    SettingSupplier<Long> getThreshold();

    /**
     * {@link TaskerTaskProvider} determines the task names for {@link nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent}.
     *
     * @return Task provider
     */
    SettingSupplier<TaskerTaskProvider> getTaskProvider();

}
