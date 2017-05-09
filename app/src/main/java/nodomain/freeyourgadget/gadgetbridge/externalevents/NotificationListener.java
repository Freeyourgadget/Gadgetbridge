/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Hasan Ammar, Julien Pivotto, Kevin Richter, Normano64, Steffen
    Liebergeld

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.AppNotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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
    public static final String ACTION_REPLY
            = "nodomain.freeyourgadget.gadgetbridge.notificationlistener.action.reply";

    private LimitedQueue mActionLookup = new LimitedQueue(16);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case GBApplication.ACTION_QUIT:
                    stopSelf();
                    break;
                case ACTION_MUTE:
                case ACTION_OPEN: {
                    StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                    int handle = intent.getIntExtra("handle", -1);
                    for (StatusBarNotification sbn : sbns) {
                        if ((sbn.getPackageName().hashCode() * 31 + sbn.getId()) == handle) {
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
                        if ((sbn.getPackageName().hashCode() * 31 + sbn.getId()) == handle) {
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
                case ACTION_REPLY:
                    int id = intent.getIntExtra("handle", -1);
                    String reply = intent.getStringExtra("reply");
                    NotificationCompat.Action replyAction = (NotificationCompat.Action) mActionLookup.lookup(id);
                    if (replyAction != null && replyAction.getRemoteInputs() != null) {
                        RemoteInput[] remoteInputs = replyAction.getRemoteInputs();
                        PendingIntent actionIntent = replyAction.getActionIntent();
                        Intent localIntent = new Intent();
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle extras = new Bundle();
                        extras.putCharSequence(remoteInputs[0].getResultKey(), reply);
                        RemoteInput.addResultsToIntent(remoteInputs, localIntent, extras);

                        try {
                            LOG.info("will send reply intent to remote application");
                            actionIntent.send(context, 0, localIntent);
                            mActionLookup.remove(id);
                        } catch (PendingIntent.CanceledException e) {
                            LOG.warn("replyToLastNotification error: " + e.getLocalizedMessage());
                        }
                    }
                    break;
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(ACTION_OPEN);
        filterLocal.addAction(ACTION_DISMISS);
        filterLocal.addAction(ACTION_DISMISS_ALL);
        filterLocal.addAction(ACTION_MUTE);
        filterLocal.addAction(ACTION_REPLY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (shouldIgnore(sbn))
            return;

        switch (GBApplication.getGrantedInterruptionFilter()) {
            case NotificationManager.INTERRUPTION_FILTER_ALL:
                break;
            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return;
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                // FIXME: Handle Reminders and Events if they are enabled in Do Not Disturb
                return;
        }

        String source = sbn.getPackageName();
        Notification notification = sbn.getNotification();
        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.id = source.hashCode() * 31 + sbn.getId();

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

        boolean preferBigText = true; //changed to true since now we update the former ID

        notificationSpec.type = AppNotificationType.getInstance().get(source);

        if (notificationSpec.type == null) {
            notificationSpec.type = NotificationType.UNKNOWN;
        }

        LOG.info("Processing notification " + notificationSpec.id + " from source " + source + " with flags: " + notification.flags);

        dissectNotificationTo(notification, notificationSpec, preferBigText);

        // ignore Gadgetbridge's very own notifications, except for those from the debug screen
        if (getApplicationContext().getPackageName().equals(source)) {
            if (!getApplicationContext().getString(R.string.test_notification).equals(notificationSpec.title)) {
                return;
            }
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                LOG.info("found wearable action: " + act.getTitle() + "  " + sbn.getTag());
                mActionLookup.add(notificationSpec.id, act);
                notificationSpec.flags |= NotificationSpec.FLAG_WEARABLE_REPLY;
                break;
            }
        }

        if ((notificationSpec.flags & NotificationSpec.FLAG_WEARABLE_REPLY) == 0 && NotificationCompat.isGroupSummary(notification)) { //this could cause #395 to come back
            LOG.info("Not forwarding notification, FLAG_GROUP_SUMMARY is set and no wearable action present. Notification flags: " + notification.flags);
            return;
        }

        GBApplication.deviceService().onNotification(notificationSpec);
    }

    private void dissectNotificationTo(Notification notification, NotificationSpec notificationSpec, boolean preferBigText) {

        Bundle extras = NotificationCompat.getExtras(notification);

        //dumpExtras(extras);

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title != null) {
            notificationSpec.title = title.toString();
        }

        CharSequence contentCS = null;
        if (extras.containsKey(Notification.EXTRA_MESSAGES)) {
            Parcelable[] parcelables = extras.getParcelableArray(NotificationCompat.EXTRA_MESSAGES);
            String contentBuilder = "";
            CharSequence sender;
            CharSequence prevSender = "";
            CharSequence message;
            for (Parcelable p : parcelables) {
                if (!(p instanceof Bundle))
                    continue;
                sender = ((Bundle) p).getCharSequence("sender");
                message = ((Bundle) p).getCharSequence("text");
                if (sender == null || message == null)
                    continue;
                if (!sender.equals(prevSender) && !sender.equals(notificationSpec.title)) {
                    contentBuilder += sender.toString() + ": ";
                    prevSender = sender;
                }
                contentBuilder += message.toString() + "\n";
            }
            contentCS = contentBuilder;
        } else {
            if (preferBigText && extras.containsKey(Notification.EXTRA_BIG_TEXT)) {
                contentCS = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT);
            } else if (extras.containsKey(Notification.EXTRA_TEXT)) {
                contentCS = extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
            }
        }
        if (contentCS != null) {
            notificationSpec.body = contentCS.toString();
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

    /**
     * Try to handle media session notifications that tell info about the current play state.
     *
     * @param mediaSession The mediasession to handle.
     * @return true if notification was handled, false otherwise
     */
    public boolean handleMediaSessionNotification(MediaSessionCompat.Token mediaSession) {
        MusicSpec musicSpec = new MusicSpec();
        MusicStateSpec stateSpec = new MusicStateSpec();

        MediaControllerCompat c;
        try {
            c = new MediaControllerCompat(getApplicationContext(), mediaSession);

            PlaybackStateCompat s = c.getPlaybackState();
            stateSpec.position = (int) (s.getPosition() / 1000);
            stateSpec.playRate = Math.round(100 * s.getPlaybackSpeed());
            stateSpec.repeat = 1;
            stateSpec.shuffle = 1;
            switch (s.getState()) {
                case PlaybackState.STATE_PLAYING:
                    stateSpec.state = MusicStateSpec.STATE_PLAYING;
                    break;
                case PlaybackState.STATE_STOPPED:
                    stateSpec.state = MusicStateSpec.STATE_STOPPED;
                    break;
                case PlaybackState.STATE_PAUSED:
                    stateSpec.state = MusicStateSpec.STATE_PAUSED;
                    break;
                default:
                    stateSpec.state = MusicStateSpec.STATE_UNKNOWN;
                    break;
            }

            MediaMetadataCompat d = c.getMetadata();
            if (d == null)
                return false;
            if (d.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
                musicSpec.artist = d.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            if (d.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
                musicSpec.album = d.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            if (d.containsKey(MediaMetadata.METADATA_KEY_TITLE))
                musicSpec.track = d.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            if (d.containsKey(MediaMetadata.METADATA_KEY_DURATION))
                musicSpec.duration = (int) d.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000;
            if (d.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
                musicSpec.trackCount = (int) d.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS);
            if (d.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
                musicSpec.trackNr = (int) d.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);

            // finally, tell the device about it
            GBApplication.deviceService().onSetMusicInfo(musicSpec);
            GBApplication.deviceService().onSetMusicState(stateSpec);

            return true;
        } catch (NullPointerException | RemoteException e) {
            return false;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (shouldIgnore(sbn))
            return;

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("autoremove_notifications", false)) {
            LOG.info("notification removed, will ask device to delete it");
            GBApplication.deviceService().onDeleteNotification(sbn.getPackageName().hashCode() * 31 + sbn.getId());
        }
    }

    private void dumpExtras(Bundle bundle) {
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value == null) {
                continue;
            }
            LOG.debug(String.format("Notification extra: %s %s (%s)", key, value.toString(), value.getClass().getName()));
        }
    }

    private boolean shouldIgnore(StatusBarNotification sbn) {
        /*
        * return early if DeviceCommunicationService is not running,
        * else the service would get started every time we get a notification.
        * unfortunately we cannot enable/disable NotificationListener at runtime like we do with
        * broadcast receivers because it seems to invalidate the permissions that are
        * necessary for NotificationListenerService
        */
        if (!isServiceRunning() || sbn == null) {
            return true;
        }

        if (shouldIgnoreSource(sbn.getPackageName()))
            return true;

        if (shouldIgnoreNotification(sbn.getNotification()))
            return true;

        return false;
    }

    private boolean shouldIgnoreSource(String source) {
        Prefs prefs = GBApplication.getPrefs();

        /* do not display messages from "android"
         * This includes keyboard selection message, usb connection messages, etc
         * Hope it does not filter out too much, we will see...
         */

        if (source.equals("android") ||
                source.equals("com.android.systemui") ||
                source.equals("com.android.dialer") ||
                source.equals("com.cyanogenmod.eleven")) {
            LOG.info("Ignoring notification, is a system event");
            return true;
        }

        if (source.equals("com.moez.QKSMS") ||
                source.equals("com.android.mms") ||
                source.equals("com.sonyericsson.conversations") ||
                source.equals("com.android.messaging") ||
                source.equals("org.smssecure.smssecure")) {
            if (!"never".equals(prefs.getString("notification_mode_sms", "when_screen_off"))) {
                return true;
            }
        }

        if (GBApplication.isBlacklisted(source)) {
            LOG.info("Ignoring notification, application is blacklisted");
            return true;
        }

        return false;
    }

    private boolean shouldIgnoreNotification(Notification notification) {

        MediaSessionCompat.Token mediaSession = NotificationCompat.getMediaSession(notification);
        //try to handle media session notifications
        if (mediaSession != null && handleMediaSessionNotification(mediaSession))
            return true;

        //ignore notifications marked as LocalOnly https://developer.android.com/reference/android/app/Notification.html#FLAG_LOCAL_ONLY
        if (NotificationCompat.getLocalOnly(notification))
            return true;

        Prefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean("notifications_generic_whenscreenon", false)) {
            PowerManager powermanager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powermanager.isScreenOn()) {
//                LOG.info("Not forwarding notification, screen seems to be on and settings do not allow this");
                return true;
            }
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
//            LOG.info("Not forwarding notification, FLAG_ONGOING_EVENT is set. Notification flags: " + notification.flags);
            return true;
        }

        return false;
    }

}
