package nodomain.freeyourgadget.gadgetbridge.tasker.spec;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplierImpl;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * Abstract implementation that supplies a {@link TaskerSettings} object to gather user configuration
 * via {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventsActivity}.
 * <p>
 * Its recommended to use this implementation so you don't have to take care of user configurations yourself.
 * Always provides a non null {@link TaskerSettings} object regardless of the {@link TaskerEventType}.
 */
public abstract class AbstractTaskerSpec implements TaskerSpec {

    private Map<TaskerEventType, TaskerSettings> settings = new HashMap<>();
    private TaskerDevice device;

    protected AbstractTaskerSpec(TaskerDevice device) {
        this.device = device;
    }

    @Override
    public TaskerSettings getSettings(TaskerEventType eventType) {
        if (!settings.containsKey(eventType)) {
            if (!getSupportedTypes().contains(eventType)) {
                settings.put(eventType, new NoOpTaskerSettings());
            } else {
                settings.put(eventType, new PreferenceTaskerSettings(device, eventType));
            }
        }
        return settings.get(eventType);
    }

    private class NoOpTaskerSettings implements TaskerSettings {
        @Override
        public SettingSupplier<Boolean> isConsumeEvent() {
            return new NoOpSupplier<>();
        }

        @Override
        public SettingSupplier<Boolean> isEnabled() {
            return new NoOpSupplier<>();
        }

        @Override
        public SettingSupplier<Long> getThreshold() {
            return new NoOpSupplier<>();
        }

        @Override
        public SettingSupplier<TaskerTaskProvider> getTaskProvider() {
            return new NoOpSupplier<>();
        }
    }

    private class NoOpSupplier<T> implements SettingSupplier<T> {

        @Override
        public T get() {
            return null;
        }

        @Override
        public void set(T object) {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public SettingSupplier<T> onChanged(SettingListener<T> onChanged) {
            return this;
        }

    }

}
