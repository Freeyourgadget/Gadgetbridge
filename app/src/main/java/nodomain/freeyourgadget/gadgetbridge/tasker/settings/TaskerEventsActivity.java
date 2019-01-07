package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.event.TaskerEventType;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerConstants;

public class TaskerEventsActivity extends AbstractSettingsActivity {

    private GBDevice device;

    public TaskerEventsActivity() {
        device = (GBDevice) getIntent().getSerializableExtra(TaskerConstants.TASKER_DEVICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_event_preferences);
        for (TaskerEventType eventType : TaskerEventType.getTypes()) {
            Preference preference = new Preference(this);
            preference.setTitle(eventType.getLocalization());
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(TaskerEventsActivity.this, TaskerEventActivity.class);
                    intent.putExtra(TaskerConstants.TASKER_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
        }
    }
}
