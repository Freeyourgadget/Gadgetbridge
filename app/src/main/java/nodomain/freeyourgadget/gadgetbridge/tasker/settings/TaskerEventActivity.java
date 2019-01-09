package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class TaskerEventActivity extends AbstractSettingsActivity {

    private TaskerConstants.TaskerDevice device;
    private TaskerEventType eventType;
    private Prefs prefs = GBApplication.getPrefs();

    public TaskerEventActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_event_preferences);
        device = (TaskerConstants.TaskerDevice) getIntent().getSerializableExtra(TaskerConstants.DEVICE_INTENT);
        eventType = (TaskerEventType) getIntent().getSerializableExtra(TaskerConstants.EVENT_INTENT);
        final PreferenceCategory tasks = (PreferenceCategory) findPreference(TaskerConstants.ACTIVITY_TASKS);
        Preference addTaskButton = findPreference(TaskerConstants.ACTIVITY_TASK_ADD);
        final EditTextPreference removeTaskButton = (EditTextPreference) findPreference(TaskerConstants.ACTIVITY_TASK_REMOVE);
        final Preference thresholdEnabled = findPreference(TaskerConstants.ACTIVITY_THESHOLD_ENABELD);
        addTaskButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (prefs.getBoolean(thresholdEnabled.getKey(), false)) {
                    tasks.addPreference(task(tasks, removeTaskButton));
                }
                return true;
            }
        });
        removeTaskButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return tasks.removePreference(preference);
            }
        });
        thresholdEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals(Boolean.TRUE)) {
                    for (int i = 1; i < tasks.getPreferenceCount(); i++) {
                        int identifier = Resources.getSystem().getIdentifier("preference", "layout", "android");
                        removeTaskButton.setWidgetLayoutResource(identifier);
                    }
                } else {
                    for (int i = 1; i < tasks.getPreferenceCount(); i++) {
                        tasks.removePreference(tasks.getPreference(i));
                    }
                }
                return true;
            }
        });
    }

    private Preference task(PreferenceCategory tasks, Preference build) {
        EditTextPreference task = new EditTextPreference(this);
        task.setKey(TaskerConstants.ACTIVITY_TASK_REMOVE + "_" + tasks.getPreferenceCount());
        task.setTitle(build.getTitle());
        task.setSummary(build.getSummary());
        task.setWidgetLayoutResource(build.getWidgetLayoutResource());
        return task;
    }

}
