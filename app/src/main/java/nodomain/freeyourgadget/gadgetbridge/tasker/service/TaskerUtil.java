package nodomain.freeyourgadget.gadgetbridge.tasker.service;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.tasker.plugin.TaskerIntent;

/**
 * Tasker convenience methods for direct access to tasker without {@link TaskerService}.
 */
public class TaskerUtil {

    /**
     * Sends a broadcast to Tasker via {@link TaskerIntent}.
     *
     * @param task Task name
     * @return True if sending the broadcast was successful,
     * false if {@link TaskerIntent#testStatus} returns false.
     * <p>
     * Does not guaranty running of actual task!
     */
    public static boolean runTask(String task) {

        if (TaskerIntent.testStatus(GBApplication.getContext()).equals(TaskerIntent.Status.OK)) {
            if (task != null) {
                GBApplication.getContext().sendBroadcast(new TaskerIntent(task));
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a {@link android.widget.Toast} for the user to show that Tasker is enabled but no task is defined.
     */
    public static void noTaskDefinedInformation() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GBApplication.getContext(), R.string.tasker_no_task_defined, Toast.LENGTH_LONG).show();
            }
        });
    }

}
