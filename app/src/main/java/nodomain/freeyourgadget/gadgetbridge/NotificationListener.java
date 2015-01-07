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
        Intent i = new Intent("nodomain.freeyourgadget.gadgetbridge.NOTIFICATION_LISTENER");
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        String content = extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        i.putExtra("notification_title", title);
        i.putExtra("notification_content", content);
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}