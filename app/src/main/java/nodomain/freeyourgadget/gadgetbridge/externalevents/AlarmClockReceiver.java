package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class AlarmClockReceiver extends BroadcastReceiver {
    /**
     * AlarmActivity and AlarmService (when unbound) listen for this broadcast intent
     * so that other applications can snooze the alarm (after ALARM_ALERT_ACTION and before
     * ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    /**
     * AlarmActivity and AlarmService listen for this broadcast intent so that other
     * applications can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    /** A public action sent by AlarmService when the alarm has started. */
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    /** A public action sent by AlarmService when the alarm has stopped for any reason. */
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
    private int lastId;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ALARM_ALERT_ACTION.equals(action)) {
            sendAlarm(true);
        } else if (ALARM_DONE_ACTION.equals(action)) {
            sendAlarm(false);
        }
    }



    private synchronized void sendAlarm(boolean on) {
        dismissLastAlarm();
        if (on) {
            lastId = generateId();
            NotificationSpec spec = new NotificationSpec();
            spec.type = NotificationType.GENERIC_ALARM_CLOCK;
            spec.id = lastId;
            // can we get the alarm title somehow?
            GBApplication.deviceService().onNotification(spec);
        }
    }

    private void dismissLastAlarm() {
        if (lastId != 0) {
            GBApplication.deviceService().onDeleteNotification(lastId);
            lastId = 0;
        }
    }

    private int generateId() {
        // lacks negative values, but should be sufficient
        return (int) (Math.random() * Integer.MAX_VALUE);
    }
}
