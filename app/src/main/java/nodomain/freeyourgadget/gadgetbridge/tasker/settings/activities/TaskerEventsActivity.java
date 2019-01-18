package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;

/**
 * Tasker events {@link AbstractSettingsActivity}. Lists supported {@link TaskerEventType}'s for the specific {@link TaskerDevice}
 * <p>
 * Forwards to {@link TaskerEventActivity}.
 */
public class TaskerEventsActivity extends AbstractSettingsActivity {

    private TaskerDevice device;

    public TaskerEventsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_events_preferences);
        device = (TaskerDevice) getIntent().getSerializableExtra(TaskerConstants.INTENT_DEVICE);
        PreferenceCategory category = (PreferenceCategory) findPreference(TaskerConstants.ACTIVITY_EVENT_GROUP);
        for (final TaskerEventType eventType : device.getSpec().getSupportedTypes()) {
            Preference preference = new Preference(this);
            preference.setTitle(eventType.getLocalization());
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(TaskerEventsActivity.this, TaskerEventActivity.class);
                    intent.putExtra(TaskerConstants.INTENT_EVENT, eventType);
                    intent.putExtra(TaskerConstants.INTENT_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
            category.addPreference(preference);
        }
    }
}
