package nodomain.freeyourgadget.gadgetbridge.tasker.plugin;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * Tasker constants.
 */
public class TaskerConstants {

    /**
     * Tasker intent's between {@link nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivity}'s.
     */
    public static final String INTENT_DEVICE = "int_tasker_device";
    public static final String INTENT_EVENT = "int_tasker_event";

    /**
     * Correspond to {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerActivity}
     */
    public static final String ACTIVITY_TASKER = "act_tasker";
    public static final String ACTIVITY_TASKER_ENABLED = "act_tasker_enabled";
    public static final String ACTIVITY_TASKER_GROUP = "act_tasker_group";
    /**
     * Correspond to {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventsActivity}
     */
    public static final String ACTIVITY_EVENT_GROUP = "act_event_group";

    /**
     * Correspond to {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventActivity}.
     */
    public static final String ACTIVITY_TASK_ADD = "act_tasker_task_add";
    public static final ScopedString ACTIVITY_CONSUME_EVENT = new ScopedString("act_tasker_consume_event");
    public static final ScopedString ACTIVITY_THRESHOLD = new ScopedString("act_tasker_threshold");
    public static final ScopedString ACTIVITY_TASK = new ScopedString("act_tasker_task");
    public static final ScopedString ACTIVITY_THRESHOLD_ENABLED = new ScopedString("act_tasker_threshold_enabled");
    public static final ScopedString ACTIVITY_TASKS = new ScopedString("act_tasker_task_group");
    public static final ScopedString ACTIVITY_EVENT_ENABLED = new ScopedString("act_tasker_event_enabled");

    public static class ScopedString {

        private String constant;
        private List<String> scopes;

        private ScopedString(String constant) {
            this.constant = constant;
        }

        public ScopedString withScope(String scope) {
            ScopedString scoped = new ScopedString(constant);
            List<String> addScope;
            if (scopes == null) {
                addScope = new ArrayList<>();
            } else {
                addScope = new ArrayList<>(scopes);
            }
            addScope.add(scope);
            scoped.scopes = addScope;
            return scoped;
        }

        public String getConstant() {
            return constant;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(constant);
            if (scopes != null && !scopes.isEmpty()) {
                for (String scope : scopes) {
                    if (StringUtils.isEmpty(scope)) {
                        continue;
                    }
//                    if (scopes.indexOf(scope) <= scopes.size() - 1) {
                    builder.append("_");
//                    }
                    builder.append(scope.toLowerCase());
                }
            }
            return builder.toString();
        }
    }

}
