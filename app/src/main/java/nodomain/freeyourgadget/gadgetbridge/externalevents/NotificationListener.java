package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.ActivityManager;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;

public class NotificationListener extends NotificationListenerService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationListener.class);

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
       /*
        * return early if BluetoothCommunicationService is not running,
        * else the service would get started every time we get a notification.
        * unfortunately we cannot enable/disable NotificationListener at runtime like we do with
        * broadcast receivers because it seems to invalidate the permissions that are
        * neccessery for NotificationListenerService
        */
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BluetoothCommunicationService.class.getName().equals(service.service.getClassName())) {
                isServiceRunning = true;
            }
        }

        if (!isServiceRunning) {
            return;
        }


        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPrefs.getBoolean("notifications_generic_whenscreenon", false)) {
            PowerManager powermanager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        String source = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            return;
        }

        /* do not display messages from "android"
         * This includes keyboard selection message, usb connection messages, etc
         * Hope it does not filter out too much, we will see...
         */

        if (source.equals("android") ||
                source.equals("com.android.systemui") ||
                source.equals("com.android.dialer") ||
                source.equals("com.android.mms") ||
                source.equals("com.cyanogenmod.eleven") ||
                source.equals("com.fsck.k9")) {
            return;
        }

        if (source.equals("eu.siacs.conversations")) {
            if (!"never".equals(sharedPrefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
                return;
            }
        }

        LOG.info("Processing notification from source " + source);

        Bundle extras = notification.extras;
        String title = extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        String content = null;
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