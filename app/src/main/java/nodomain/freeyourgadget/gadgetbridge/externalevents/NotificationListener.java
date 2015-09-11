package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class NotificationListener extends NotificationListenerService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationListener.class);

    public static final String ACTION_DISMISS
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.dismiss";
    public static final String ACTION_DISMISS_ALL
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.dismiss_all";
    public static final String ACTION_OPEN
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.open";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_OPEN)) {
                StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                int handle = intent.getIntExtra("handle", -1);
                for (StatusBarNotification sbn : sbns) {
                    if ((int) sbn.getPostTime() == handle) {
                        try {
                            PendingIntent pi = sbn.getNotification().contentIntent;
                            if (pi != null) {
                                pi.send();
                            }
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (action.equals(ACTION_DISMISS)) {
                StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                int handle = intent.getIntExtra("handle", -1);
                for (StatusBarNotification sbn : sbns) {
                    if ((int) sbn.getPostTime() == handle) {
                        if (GBApplication.isRunningLollipopOrLater()) {
                            String key = sbn.getKey();
                            NotificationListener.this.cancelNotification(key);
                        } else {
                            int id = sbn.getId();
                            String pkg = sbn.getPackageName();
                            String tag = sbn.getTag();
                            NotificationListener.this.cancelNotification(pkg, tag, id);
                        }
                    }
                }
            } else if (action.equals(ACTION_DISMISS_ALL)) {
                NotificationListener.this.cancelAllNotifications();
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_OPEN);
        filterLocal.addAction(ACTION_DISMISS);
        filterLocal.addAction(ACTION_DISMISS_ALL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
       /*
        * return early if DeviceCommunicationService is not running,
        * else the service would get started every time we get a notification.
        * unfortunately we cannot enable/disable NotificationListener at runtime like we do with
        * broadcast receivers because it seems to invalidate the permissions that are
        * necessary for NotificationListenerService
        */
        if (!isServiceRunning()) {
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
                source.equals("com.moez.QKSMS") ||
                source.equals("com.cyanogenmod.eleven")) {
            return;
        }

        if (source.equals("eu.siacs.conversations")) {
            if (!"never".equals(sharedPrefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
                return;
            }
        }

        if (source.equals("com.fsck.k9")) {
            if (!"never".equals(sharedPrefs.getString("notification_mode_k9mail", "when_screen_off"))) {
                return;
            }
        }

        HashSet<String> blacklist = (HashSet<String>) sharedPrefs.getStringSet("package_blacklist", null);
        if (blacklist != null && blacklist.contains(source)) {
            return;
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
            GBApplication.deviceService().onGenericNotification(title, content, (int) sbn.getPostTime()); //FIMXE: a truly unique id would be better
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DeviceCommunicationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}