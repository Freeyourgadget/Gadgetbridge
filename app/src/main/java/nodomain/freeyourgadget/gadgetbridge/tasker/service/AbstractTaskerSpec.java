package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplier;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.SettingSupplierImpl;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;

public abstract class AbstractTaskerSpec implements TaskerSpec {

    private Map<TaskerEventType, TaskerSettings> settings = new HashMap<>();

    @Override
    public TaskerSettings getSettings(TaskerEventType eventType) {
        if (!settings.containsKey(eventType)) {
            if (!getSupportedTypes().contains(eventType)) {
                settings.put(eventType, new NoOpTaskerSettings());
            } else {
                settings.put(eventType, new SimpleTaskerSettings());
            }
        }
        return settings.get(eventType);
    }

    private class SimpleTaskerSettings implements TaskerSettings {
        private SettingSupplier<Boolean> consumingEvents = new SettingSupplierImpl<>();
        private SettingSupplier<Boolean> enabled = new SettingSupplierImpl<>();
        private SettingSupplier<Long> threshold = new SettingSupplierImpl<>();
        private SettingSupplier<TaskerTaskProvider> taskProvider = new SettingSupplierImpl<>();

        @Override
        public SettingSupplier<Boolean> isConsumingEvents() {
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
    }


    private class NoOpTaskerSettings implements TaskerSettings {
        @Override
        public SettingSupplier<Boolean> isConsumingEvents() {
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
