package nodomain.freeyourgadget.gadgetbridge.tasker.spec;

import android.content.SharedPreferences;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.NoTaskDefinedException;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.PreferenceSettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventActivity;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

/**
 * {@link SharedPreferences} implementation of {@link TaskerSettings}.
 * Scoped with {@link TaskerDevice} and {@link TaskerEventType}.
 * <p>
 * If used the user settings are already implemented with {@link TaskerEventActivity}.
 */
public class PreferenceTaskerSettings implements TaskerSettings {

    private DeviceType device;
    private TaskerEventType eventType;
    private SettingSupplier<Boolean> consumingEvents;
    private SettingSupplier<Boolean> enabled;
    private SettingSupplier<Long> threshold;
    private SettingSupplier<TaskerTaskProvider> taskProvider;
    private SharedPreferences preferences;

    public PreferenceTaskerSettings(DeviceType device, TaskerEventType eventType) {
        this.device = device;
        this.eventType = eventType;
        this.preferences = GBApplication.getPrefs().getPreferences();
        consumingEvents = new PreferenceSettingSupplier<>(scope(TaskerConstants.ACTIVITY_CONSUME_EVENT).toString(), Boolean.class);
        enabled = new PreferenceSettingSupplier<>(scope(TaskerConstants.ACTIVITY_EVENT_ENABLED).toString(), Boolean.class);
        threshold = new PreferenceSettingSupplier<>(scope(TaskerConstants.ACTIVITY_THRESHOLD).toString(), Long.class);
        taskProvider = new TaskerTaskProviderPreferenceSettingSupplier();
    }

    @Override
    public SettingSupplier<Boolean> isConsumeEvent() {
        return consumingEvents;
    }

    @Override
    public SettingSupplier<Boolean> isEnabled() {
        return enabled;
    }

    @Override
    public SettingSupplier<Long> getThreshold() {
        return threshold;
    }

    @Override
    public SettingSupplier<TaskerTaskProvider> getTaskProvider() {
        return taskProvider;
    }

    private class TaskerTaskProviderPreferenceSettingSupplier implements SettingSupplier<TaskerTaskProvider> {

        private TaskerTaskProvider provider = new TaskerTaskProvider() {

            @Override
            public String getTask(TaskerEvent event) {
                String key = scope(TaskerConstants.ACTIVITY_TASK).withScope(String.valueOf(event.getCount())).toString();
                if (event.getType().equals(eventType) && preferences.contains(key)) {
                    String taskName = preferences.getString(key, null);
                    if(taskName != null && !taskName.isEmpty()){
                        return taskName;
                    }
                }
                throw new NoTaskDefinedException();
            }

        };

        @Override
        public TaskerTaskProvider get() {
            return provider;
        }

        @Override
        public void set(TaskerTaskProvider object) {
            // Not supported
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public SettingSupplier<TaskerTaskProvider> onChanged(final SettingListener<TaskerTaskProvider> onChanged) {
            preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.startsWith(scope(TaskerConstants.ACTIVITY_TASK).toString())) {
                        onChanged.changed(get());
                    }
                }
            });
            return this;
        }
    }

    private TaskerConstants.ScopedString scope(TaskerConstants.ScopedString constant) {
        return constant.withScope(device.name()).withScope(eventType.getType());
    }
}
