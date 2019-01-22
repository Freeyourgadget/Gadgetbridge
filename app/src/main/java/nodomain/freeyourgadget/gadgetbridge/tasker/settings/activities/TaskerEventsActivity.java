package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TaskerEventsFragment taskerEventsFragment = new TaskerEventsFragment();
        Bundle arguments = new Bundle();
        if (getIntent().hasExtra(TaskerConstants.INTENT_DEVICE)) {
            arguments.putSerializable(TaskerConstants.INTENT_DEVICE, getIntent().getSerializableExtra(TaskerConstants.INTENT_DEVICE));
        }
        taskerEventsFragment.setArguments(arguments);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                taskerEventsFragment).commit();
    }

    public static class TaskerEventsFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
            if (getArguments().get(TaskerConstants.INTENT_DEVICE) == null) {
                return;
            }
            final TaskerDevice device = (TaskerDevice) getArguments().get(TaskerConstants.INTENT_DEVICE);
            for (final TaskerEventType eventType : device.getSpec().getSupportedTypes()) {
                Preference preference = new Preference(getActivity());
                preference.setTitle(eventType.getLocalization());
                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(getActivity(), TaskerEventActivity.class);
                        intent.putExtra(TaskerConstants.INTENT_EVENT, eventType);
                        intent.putExtra(TaskerConstants.INTENT_DEVICE, device);
                        startActivityForResult(intent, 0);
                        return true;
                    }
                });
                getPreferenceScreen().addPreference(preference);
            }

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            getArguments().putSerializable(TaskerConstants.INTENT_DEVICE, data.getSerializableExtra(TaskerConstants.INTENT_DEVICE));
        }
    }
}
