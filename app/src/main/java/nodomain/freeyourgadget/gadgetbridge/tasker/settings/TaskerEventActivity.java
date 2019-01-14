package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        device = (TaskerConstants.TaskerDevice) getIntent().getSerializableExtra(TaskerConstants.DEVICE_INTENT);
        eventType = (TaskerEventType) getIntent().getSerializableExtra(TaskerConstants.EVENT_INTENT);
        final PreferenceScreen tasks = (PreferenceScreen) findPreference(TaskerConstants.ACTIVITY_TASKS);
        ButtonPreference addTaskButton = (ButtonPreference) findPreference(TaskerConstants.ACTIVITY_TASK_ADD);
        final EditTextPreference taskNamePreference = (EditTextPreference) findPreference(TaskerConstants.ACTIVITY_TASK);
        final Preference thresholdEnabled = findPreference(TaskerConstants.ACTIVITY_THESHOLD_ENABELD);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prefs.getBoolean(thresholdEnabled.getKey(), false)) {
                    tasks.addPreference(task(tasks, taskNamePreference));
                }
            }
        });
        thresholdEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals(Boolean.FALSE)) {
                    for (int i = 2; i < tasks.getPreferenceCount(); i++) {
                        tasks.removePreference(tasks.getPreference(tasks.getPreferenceCount()));
                    }
                }
                return true;
            }
        });
    }

    private Preference task(final PreferenceScreen tasks, Preference build) {
        final ButtonPreference task = new ButtonPreference(this);
        task.setKey(TaskerConstants.ACTIVITY_TASK + "_" + tasks.getPreferenceCount());
        task.setTitle(build.getTitle());
        task.setSummary(build.getSummary());
        task.setButtonText(R.string.tasker_remove);
        task.setWidgetLayoutResource(R.layout.tasker_add_button);
        task.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tasks.removePreference(task);
            }
        });
        return task;
    }

}
