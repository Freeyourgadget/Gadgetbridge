package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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
        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String content = "";
        if (extras.containsKey(Notification.EXTRA_TEXT))
            content = extras.getString(Notification.EXTRA_TEXT);

        if (content != null) {
            Intent startIntent = new Intent(NotificationListener.this, BluetoothCommunicationService.class);
            startIntent.setAction(BluetoothCommunicationService.ACTION_SENDBLUETOOTHMESSAGE);
            startIntent.putExtra("notification_title", title);
            startIntent.putExtra("notification_content", content);
            startService(startIntent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}