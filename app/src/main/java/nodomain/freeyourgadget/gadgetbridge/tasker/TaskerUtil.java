package nodomain.freeyourgadget.gadgetbridge.tasker;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class TaskerUtil {

    public static boolean sendTask() {

        if (TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK) || GBApplication.getPrefs().getBoolean(TaskerPrefs.TASKER_ACTIVE, true)) {
            String task = GBApplication.getPrefs().getString(TaskerPrefs.TASKER_TASK, "watch");
            if (task != null) {
                GBApplication.getContext().sendBroadcast(new TaskerIntent(task));
                return true;
            }
        }
        return false;
    }

}
