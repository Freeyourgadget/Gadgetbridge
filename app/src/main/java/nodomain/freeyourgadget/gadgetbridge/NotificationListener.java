package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        /* do not display messages from "android"
         * This includes keyboard selection message, usb connection messages, etc
         * Hope it does not filter out too much, we will see...
         */
        String source = sbn.getPackageName();
        Log.i(TAG, source);

        if (source.equals("android") ||
                source.equals("com.android.dialer") ||
                source.equals("com.fsck.k9") ||
                source.equals("com.android.mms")) {
            return;
        }

        Log.i(TAG, "Processing notification from source " + source);

        Bundle extras = notification.extras;
        String title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();

        String content = "";
        if (extras.containsKey(Notification.EXTRA_TEXT)) {
            CharSequence contentCS = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (contentCS != null) {
                content = contentCS.toString();
            }
        }

        if (content != null) {
            Intent startIntent = new Intent(NotificationListener.this, BluetoothCommunicationService.class);
            startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_GENERIC);
            startIntent.putExtra("notification_title", title);
            startIntent.putExtra("notification_body", content);
            startService(startIntent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}