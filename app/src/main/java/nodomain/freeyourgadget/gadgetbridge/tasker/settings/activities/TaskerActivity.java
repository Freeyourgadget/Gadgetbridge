package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerConstants;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerDevice;

/**
 * Tasker main {@link AbstractSettingsActivity} builds an supported list of {@link TaskerDevice}.
 * <p>
 * Forwards to {@link TaskerEventsActivity}.
 */
public class TaskerActivity extends AbstractSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new TaskerFragment()).commit();
    }

    public static class TaskerFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.tasker_preferences);
            final PreferenceCategory group = (PreferenceCategory) findPreference(TaskerConstants.ACTIVITY_TASKER_GROUP);
            for (TaskerDevice device : TaskerDevice.values()) {
                group.addPreference(preference(device));
            }
        }

        private Preference preference(final TaskerDevice device) {
            Preference devicePreference = new Preference(getActivity());
            devicePreference.setTitle(device.getType().getName());
            devicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), TaskerEventsActivity.class);
                    intent.putExtra(TaskerConstants.INTENT_DEVICE, device);
                    startActivity(intent);
                    return true;
                }
            });
            return devicePreference;
        }
    }
}
