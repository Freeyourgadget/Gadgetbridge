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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class NotificationListener extends NotificationListenerService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationListener.class);

    public static final String ACTION_DISMISS
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.dismiss";
    public static final String ACTION_DISMISS_ALL
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.dismiss_all";
    public static final String ACTION_OPEN
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.open";
    public static final String ACTION_MUTE
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.mute";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_MUTE:
                case ACTION_OPEN: {
                    StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                    int handle = intent.getIntExtra("handle", -1);
                    for (StatusBarNotification sbn : sbns) {
                        if ((int) sbn.getPostTime() == handle) {
                            if (action.equals(ACTION_OPEN)) {
                                try {
                                    PendingIntent pi = sbn.getNotification().contentIntent;
                                    if (pi != null) {
                                        pi.send();
                                    }
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // ACTION_MUTE
                                LOG.info("going to mute " + sbn.getPackageName());
                                GBApplication.addToBlacklist(sbn.getPackageName());
                            }
                        }
                    }
                    break;
                }
                case ACTION_DISMISS: {
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
                    break;
                }
                case ACTION_DISMISS_ALL:
                    NotificationListener.this.cancelAllNotifications();
                    break;
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
        filterLocal.addAction(ACTION_MUTE);
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

        if (source.equals("com.moez.QKSMS") || source.equals("com.android.mms")) {
            if (!"never".equals(sharedPrefs.getString("notification_mode_sms", "when_screen_off"))) {
                return;
            }
        }

        if (GBApplication.blacklist != null && GBApplication.blacklist.contains(source)) {
            return;
        }

        NotificationSpec notificationSpec = new NotificationSpec();

        // determinate Source App Name ("Label")
        PackageManager pm = getPackageManager();
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo(source, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (ai != null) {
            notificationSpec.sourceName = (String) pm.getApplicationLabel(ai);
        }
        
        switch (source) {
            case "org.mariotaku.twidere":
            case "com.twitter.android":
            case "org.andstatus.app":
            case "org.mustard.android":
                notificationSpec.type = NotificationType.TWITTER;
                break;
            case "com.fsck.k9":
            case "com.android.email":
                notificationSpec.type = NotificationType.EMAIL;
                break;
            case "com.moez.QKSMS":
            case "com.android.mms":
                notificationSpec.type = NotificationType.SMS;
                break;
            case "eu.siacs.conversations":
            case "org.thoughtcrime.securesms":
                notificationSpec.type = NotificationType.CHAT;
                break;
            case "org.indywidualni.fblite":
                notificationSpec.type = NotificationType.FACEBOOK;
                break;
            default:
                notificationSpec.type = NotificationType.UNDEFINED;
                break;
        }

        LOG.info("Processing notification from source " + source);

        dissectNotificationTo(notification, notificationSpec);
        notificationSpec.id = (int) sbn.getPostTime(); //FIMXE: a truly unique id would be better
        GBApplication.deviceService().onNotification(notificationSpec);
    }

    private void dissectNotificationTo(Notification notification, NotificationSpec notificationSpec) {
        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title != null) {
            notificationSpec.title = title.toString();
        }
        if (extras.containsKey(Notification.EXTRA_TEXT)) {
            CharSequence contentCS = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (contentCS != null) {
                notificationSpec.body = contentCS.toString();
            }
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
