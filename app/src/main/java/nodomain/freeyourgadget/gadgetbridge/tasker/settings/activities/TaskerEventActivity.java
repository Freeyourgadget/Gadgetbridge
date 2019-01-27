package nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TaskerEventFragment taskerEventFragment = new TaskerEventFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable(TaskerConstants.INTENT_DEVICE, getIntent().getSerializableExtra(TaskerConstants.INTENT_DEVICE));
        arguments.putSerializable(TaskerConstants.INTENT_EVENT, getIntent().getSerializableExtra(TaskerConstants.INTENT_EVENT));
        taskerEventFragment.setArguments(arguments);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                taskerEventFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent resultIntent = new Intent();
                resultIntent.putExtra(TaskerConstants.INTENT_DEVICE, getIntent().getSerializableExtra(TaskerConstants.INTENT_DEVICE));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TaskerEventFragment extends PreferenceFragment {

        private TaskerDevice device;
        private TaskerEventType eventType;
        private Prefs prefs = GBApplication.getPrefs();

        private SwitchPreference enableEvent;
        private SwitchPreference enableThreshold;
        private NumberPreference threshold;
        private ButtonPreference addTask;
        private List<EditTextPreference> tasks = new ArrayList<>();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
            device = (TaskerDevice) getArguments().get(TaskerConstants.INTENT_DEVICE);
            eventType = (TaskerEventType) getArguments().get(TaskerConstants.INTENT_EVENT);
            initEnableEvent();
            initAddTask();
            initEnableThreshold();
            initThreshold();
            initTasks();
        }

        @Override
        public void onOptionsMenuClosed(Menu menu) {
            super.onOptionsMenuClosed(menu);
        }

        private void initEnableEvent() {
            String key = scoped(TaskerConstants.ACTIVITY_EVENT_ENABLED);
            enableEvent = new SwitchPreference(getActivity());
            enableEvent.setKey(key);
            enableEvent.setTitle(R.string.tasker_event_enabled);
//            enableEvent.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    settings.isEnabled().set((Boolean) newValue);
//                    return true;
//                }
//            });
//            settings.isEnabled().set(prefs.getBoolean(key, false));
            getPreferenceScreen().addPreference(enableEvent);
        }

        private void initEnableThreshold() {
            String key = scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED);
            enableThreshold = new SwitchPreference(getActivity());
            enableThreshold.setKey(key);
            enableThreshold.setTitle(R.string.tasker_threshold_enable);
            enableThreshold.setSummary(R.string.tasker_threshold_enable_sum);
            getPreferenceScreen().addPreference(enableThreshold);
        }

        private void initThreshold() {
            final String key = scoped(TaskerConstants.ACTIVITY_THRESHOLD);
            threshold = new NumberPreference(getActivity());
            threshold.setKey(key);
            threshold.setTitle(R.string.tasker_threshold);
            threshold.setSummary(R.string.tasker_threshold_sum);
            threshold.getNumberPicker().setMinValue(50);
            threshold.getNumberPicker().setMaxValue(10000);
//            setThresholdIfDefined(settings);
//            threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    settings.getThreshold().set(Long.valueOf(newValue.toString()));
//                    return true;
//                }
//            });
//            if (prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED), false)) {
//                settings.getThreshold().set(null);
//            }
            enableThreshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.equals(Boolean.FALSE)) {
                        for (EditTextPreference taskPreference : tasks) {
                            if (!taskPreference.getKey().equals(scoped(TaskerConstants.ACTIVITY_TASK))) {
                                getPreferenceScreen().removePreference(taskPreference);
                            }
                        }
                        prefs.getPreferences().edit().remove(key).commit();
                    }
                    return true;
                }
            });
            getPreferenceScreen().addPreference(threshold);
        }

        private void initAddTask() {
            addTask = new ButtonPreference(getActivity());
            addTask.setTitle(R.string.tasker_task);
            addTask.setSummary(R.string.tasker_task_sum);
            getPreferenceScreen().addPreference(addTask);
        }


        private String scoped(TaskerConstants.ScopedString scopedString) {
            return scopedString.withScope(device.name()).withScope(eventType.getType()).toString();
        }

        private void loadTasks() {
            tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (!prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED), false) && i > 0) {
                    break;
                }
                String key = scoped(TaskerConstants.ACTIVITY_TASK) + "_" + i;
                if (prefs.getPreferences().contains(key)) {
                    EditTextPreference task = task(key);
                    tasks.add(task);
                    getPreferenceScreen().addPreference(task);
                }
            }
            // Add default task
            if (tasks.isEmpty()) {
                EditTextPreference task = task(scoped(TaskerConstants.ACTIVITY_TASK) + "_" + 0);
                tasks.add(task);
                getPreferenceScreen().addPreference(task);
            }
        }

        private void initTasks() {
            loadTasks();
            addTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (prefs.getBoolean(scoped(TaskerConstants.ACTIVITY_THRESHOLD_ENABLED), false)) {
                        getPreferenceScreen().addPreference(task(scoped(TaskerConstants.ACTIVITY_TASK) + "_" + (tasks.size() - 1)));
                    }
                }
            });
//            TaskerTaskProvider taskerTaskProvider = new TaskerTaskProvider() {
//                @Override
//                public String getTask(TaskerEvent event) {
//                    if (event.getCount() < tasks.size()) {
//                        String text = tasks.get(event.getCount()).getText();
//                        if (text == null || StringUtils.isEmpty(text)) {
//                            throw new NoTaskDefinedException();
//                        }
//                        return text;
//                    }
//                    return null;
//                }
//
//            };
//            settings.getTaskProvider().set(taskerTaskProvider);
        }

        private EditTextPreference task(String key) {
            final ButtonPreference task = new ButtonPreference(getActivity());
            task.setKey(key);
            task.setSummary(R.string.tasker_task_sum);
            task.setButtonText(R.string.tasker_remove);
            task.setWidgetLayoutResource(R.layout.button_preference_layout);
            task.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPreferenceScreen().removePreference(task);
                }
            });
            return task;
        }

    }

}
