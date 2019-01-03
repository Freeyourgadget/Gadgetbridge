package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class TaskerUtil {

    public static boolean runTask(String task) {

        if (TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK)) {
            if (task != null) {
                GBApplication.getContext().sendBroadcast(new TaskerIntent(task));
                return true;
            }
        }
        return false;
    }

}
