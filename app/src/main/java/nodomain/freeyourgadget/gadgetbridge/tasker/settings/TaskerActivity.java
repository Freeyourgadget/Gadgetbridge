package nodomain.freeyourgadget.gadgetbridge.tasker.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.tasker.service.TaskerConstants;

public class TaskerActivity extends AbstractSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tasker_preferences);
        final PreferenceCategory group = (PreferenceCategory) findPreference(TaskerConstants.TASKER_PREF_GROUP);
        for (TaskerConstants.TaskerDevice device : TaskerConstants.TaskerDevice.values()) {
            group.addPreference(preference(device));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private Preference preference(final TaskerConstants.TaskerDevice device) {
        Preference devicePreference = new Preference(this);
        devicePreference.setTitle(device.getType().getName());
        devicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(TaskerActivity.this, TaskerEventsActivity.class);
                intent.putExtra(TaskerConstants.DEVICE_INTENT, device);
                startActivity(intent);
                return true;
            }
        });
        return devicePreference;
    }
}
