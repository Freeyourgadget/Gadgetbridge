package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEvent;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.NoTaskDefinedException;
import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;
import nodomain.freeyourgadget.gadgetbridge.tasker.spec.TaskerSpec;
import nodomain.freeyourgadget.gadgetbridge.tasker.task.TaskerTaskProvider;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * Tasker event {@link AbstractSettingsActivity} takes {@link TaskerSpec} from {@link TaskerDevice#getSpec()}
 * to configure its {@link TaskerSettings} via {@link TaskerSpec#getSettings(TaskerEventType)}.
 * <p>
 * If you extend {@link TaskerSettings} this is the point to implement the new features for user configuration.
 */
public class TaskerEventActivity extends AbstractSettingsActivity {

    private TaskerDevice device;
    private TaskerEventType eventType;
    private Prefs prefs = GBApplication.getPrefs();
    private List<EditTextPreference> taskPreferences = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_event_preferences);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        device = (TaskerDevice) getIntent().getSerializableExtra(TaskerConstants.INTENT_DEVICE);
        eventType = (TaskerEventType) getIntent().getSerializableExtra(TaskerConstants.INTENT_EVENT);
        final TaskerSettings settings = device.getSpec().getSettings(eventType);
        SwitchPreference enabled = (SwitchPreference) findPreference(scoped(TaskerConstants.ACTIVITY_EVENT_ENABLED));
        settings.isEnabled().set(prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_EVENT_ENABLED), false));
        enabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                settings.isEnabled().set((Boolean) newValue);
                return true;
            }
        });
        eventType = (TaskerEventType) getIntent().getSerializableExtra(TaskerConstants.INTENT_EVENT);
        final PreferenceScreen tasks = (PreferenceScreen) findPreference(scoped(TaskerConstants.ACTIVITY_TASKS));
        initThreshold(settings, tasks);
        initTasks(settings, tasks);
    }

    private String scoped(TaskerConstants.ScopedString scopedString) {
        return scopedString.withScope(device.name()).withScope(eventType.getType()).toString();
    }

    private void initThreshold(final TaskerSettings settings, final PreferenceScreen tasks) {
        final EditTextPreference threshold = (EditTextPreference) findPreference(scoped(TaskerConstants.ACTIVITY_THRESHOLD));
        setThresholdIfDefined(settings);
        threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                settings.getThreshold().set(Long.valueOf(newValue.toString()));
                return true;
            }
        });
        final Preference thresholdEnabled = findPreference(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED));
        if (prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED), false)) {
            settings.getThreshold().set(null);
        }
        thresholdEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals(Boolean.FALSE)) {
                    for (EditTextPreference taskPreference : taskPreferences) {
                        if (!taskPreference.getKey().equals(scoped(TaskerConstants.ACTIVITY_TASK))) {
                            tasks.removePreference(taskPreference);
                        }
                    }
                    settings.getThreshold().set(null);
                    return true;
                }
                setThresholdIfDefined(settings);
                return true;
            }
        });
    }

    private void setThresholdIfDefined(TaskerSettings settings) {
        long thresholdValue = prefs.getLong(scoped(TaskerConstants.ACTIVITY_THRESHOLD), 0L);
        if (thresholdValue != 0L) {
            settings.getThreshold().set(prefs.getLong(scoped(TaskerConstants.ACTIVITY_THRESHOLD), 50L));
        }
    }

    private void initTasks(final TaskerSettings settings, final PreferenceScreen tasks) {
        ButtonPreference addTaskButton = (ButtonPreference) findPreference(TaskerConstants.ACTIVITY_TASK_ADD);
        final EditTextPreference taskNamePreference = (EditTextPreference) findPreference(scoped(TaskerConstants.ACTIVITY_TASK));
        taskPreferences.add(taskNamePreference);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED), false)) {
                    tasks.addPreference(task(tasks, taskNamePreference));
                }
            }
        });
        TaskerTaskProvider taskerTaskProvider = new TaskerTaskProvider() {
            @Override
            public String getTask(TaskerEvent event) {
                if (event.getCount() < taskPreferences.size()) {
                    String text = taskPreferences.get(event.getCount()).getText();
                    if (StringUtils.isEmpty(text)) {
                        throw new NoTaskDefinedException();
                    }
                    return text;
                }
                return null;
            }

        };
        settings.getTaskProvider().set(taskerTaskProvider);
    }

    private Preference task(final PreferenceScreen tasks, Preference build) {
        final ButtonPreference task = new ButtonPreference(this);
        task.setKey(scoped(TaskerConstants.ACTIVITY_TASK) + "_" + tasks.getPreferenceCount());
        task.setTitle(build.getTitle());
        task.setSummary(build.getSummary());
        task.setButtonText(R.string.tasker_remove);
        task.setWidgetLayoutResource(R.layout.button_preference_layout);
        task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tasks.removePreference(task);
            }
        });
        taskPreferences.add(task);
        return task;
    }

}
