package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.xwatch.XWatchService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class TaskerConstants {


    private static final String tasker = "tasker";
    public static final String TASKER = "pref_key_tasker";
    public static final String TASKER_SETTINGS = "pref_key_tasker_settings";
    public static final String TASKER_ENABLED = "tasker_enabled";
    public static final String TASKER_PREFERENCES = "tasker_preferences";
    public static final String TASKER_PREF_GROUP = "pref_key_tasker_group";
    public static final String DEVICE_INTENT = "intent_tasker_device";
    public static final String EVENT_INTENT = "intent_tasker_event";
    public static final String TASKER_TASK = "tasker-task";
    public static final String TASKER_PREFERENCE = "tasker_list";
    public static final String PREF_EVENT_GROUP = "pref_key_tasker_event_group";

    public static final String ACTIVITY_THRESHOLD = "act_tasker_threshold";
    public static final String ACTIVITY_TASK_ADD = "act_tasker_task_add";
    public static final String ACTIVITY_TASK = "act_tasker_task";
    public static final String ACTIVITY_THESHOLD_ENABELD = "act_tasker_threshold_enabled";
    public static final String ACTIVITY_TASKS = "act_tasker_task_group";


    public enum TaskerDevice implements Serializable {

        XWATCH(DeviceType.XWATCH, XWatchService.getTaskerSpec());

        private DeviceType type;
        private TaskerSpec spec;

        TaskerDevice(DeviceType type, TaskerSpec spec) {
            this.type = type;
            this.spec = spec;
        }

        public DeviceType getType() {
            return type;
        }

        public TaskerSpec getSpec() {
            return spec;
        }
    }

//    public static class Settings {
//
//        private static final String setting = "setting";
//        public static final String ENABLED = concate(tasker, setting, "enabled");
//        public static final String ENABLED = "tasker_enabled";
//
//    }
//
//    public static class Preferences {
//        private static final String prefKey = "pref_key";
//        public static final String TASKER = concate(prefKey, "tasker");
//        public static final String TASKER_SETTINGS = concate(prefKey, "tasker", "settings");
//    }

    private static String concate(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            builder.append(parts[i]);
            if (i < parts.length - 1) {
                builder.append("_");
            }
        }
        return builder.toString();
    }

}
