package nodomain.freeyourgadget.gadgetbridge.tasker.service;

public class TaskerConstants {


    private static final String TASKER = "tasker";
    public static final String TASKER = "pref_key_tasker";
    public static final String TASKER_SETTINGS = "pref_key_tasker_settings";
    public static final String TASKER_ENABLED = "tasker_enabled";
    public static final String TASKER_PREFERENCES = "tasker_preferences";
    public static final String TASKER_PREF_GROUP = "tasker_group";
    public static final String TASKER_DEVICE = "tasker_device";
    public static final String TASKER_TASK = "tasker-task";
    public static final String TASKER_PREFERENCE = "tasker_list";


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
