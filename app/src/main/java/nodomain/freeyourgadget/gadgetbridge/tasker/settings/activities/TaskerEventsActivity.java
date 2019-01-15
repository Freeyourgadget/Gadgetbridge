package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerConstants;

public class TaskerEventsActivity extends AbstractSettingsActivity {

    private TaskerConstants.TaskerDevice device;

    public TaskerEventsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_events_preferences);
        device = (TaskerConstants.TaskerDevice) getIntent().getSerializableExtra(TaskerConstants.DEVICE_INTENT);
        PreferenceCategory category = (PreferenceCategory) findPreference(TaskerConstants.PREF_EVENT_GROUP);
        for (final TaskerEventType eventType : device.getSpec().getSupportedTypes()) {
            Preference preference = new Preference(this);
            preference.setTitle(eventType.getLocalization());
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(TaskerEventsActivity.this, TaskerEventActivity.class);
                    intent.putExtra(TaskerConstants.EVENT_INTENT, eventType);
                    intent.putExtra(TaskerConstants.DEVICE_INTENT, device);
                    startActivity(intent);
                    return true;
                }
            });
            category.addPreference(preference);
        }
    }
}
