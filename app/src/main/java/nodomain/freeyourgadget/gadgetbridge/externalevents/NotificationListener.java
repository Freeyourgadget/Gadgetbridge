/*  Copyright (C) 2015-2019 abettenburg, Andreas Shimokawa, AndrewBedscastle,
    Carsten Pfeiffer, Daniele Gobbetti, Frank Slezak, Hasan Ammar, José Rebelo,
    Julien Pivotto, Kevin Richter, Matthieu Baerts, Normano64, Steffen Liebergeld,
    Taavi Eomäe, veecue, Zhong Jianxin

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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterDao;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntryDao;
import nodomain.freeyourgadget.gadgetbridge.model.AppNotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static androidx.media.app.NotificationCompat.MediaStyle.getMediaSession;
import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST;
import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST;
import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL;

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

    private LimitedQueue mActionLookup = new LimitedQueue(32);
    private LimitedQueue mPackageLookup = new LimitedQueue(64);
    private LimitedQueue mNotificationHandleLookup = new LimitedQueue(128);

    private HashMap<String, Long> notificationBurstPrevention = new HashMap<>();
    private HashMap<String, Long> notificationOldRepeatPrevention = new HashMap<>();

    private long activeCallPostTime;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                LOG.warn("no action");
                return;
            }

            int handle = (int) intent.getLongExtra("handle", -1);
            switch (action) {
                case GBApplication.ACTION_QUIT:
                    stopSelf();
                    break;

                case ACTION_OPEN: {
                    StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                    Long ts = (Long) mNotificationHandleLookup.lookup(handle);
                    if (ts == null) {
                        LOG.info("could not lookup handle for open action");
                        break;
                    }

                    for (StatusBarNotification sbn : sbns) {
                        if (sbn.getPostTime() == ts) {
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
                    break;
                }
                case ACTION_MUTE:
                    String packageName = (String) mPackageLookup.lookup(handle);
                    if (packageName == null) {
                        LOG.info("could not lookup handle for mute action");
                        break;
                    }

                    LOG.info("going to mute " + packageName);
                    GBApplication.addAppToNotifBlacklist(packageName);
                    break;
                case ACTION_DISMISS: {
                    StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                    Long ts = (Long) mNotificationHandleLookup.lookup(handle);
                    if (ts == null) {
                        LOG.info("could not lookup handle for dismiss action");
                        break;
                    }
                    for (StatusBarNotification sbn : sbns) {
                        if (sbn.getPostTime() == ts) {
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
                    NotificationCompat.Action wearableAction = (NotificationCompat.Action) mActionLookup.lookup(handle);
                    String reply = intent.getStringExtra("reply");
                    if (wearableAction != null) {
                        PendingIntent actionIntent = wearableAction.getActionIntent();
                        Intent localIntent = new Intent();
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if(wearableAction.getRemoteInputs()!=null) {
                            RemoteInput[] remoteInputs = wearableAction.getRemoteInputs();
                            Bundle extras = new Bundle();
                            extras.putCharSequence(remoteInputs[0].getResultKey(), reply);
                            RemoteInput.addResultsToIntent(remoteInputs, localIntent, extras);
                        }
                        try {
                            LOG.info("will send exec intent to remote application");
                            actionIntent.send(context, 0, localIntent);
                            mActionLookup.remove(handle);
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

    public String getAppName(String pkg) {
        // determinate Source App Name ("Label")
        PackageManager pm = getPackageManager();
        try {
            return (String)pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if ("call".equals(sbn.getNotification().category)) {
            handleCallNotification(sbn);
            return;
        }
        if (shouldIgnore(sbn)) {
            LOG.info("Ignore notification");
            return;
        }



        Prefs prefs = GBApplication.getPrefs();

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

        String source = sbn.getPackageName().toLowerCase();
        Notification notification = sbn.getNotification();
        if (notificationOldRepeatPrevention.containsKey(source)) {
            if (notification.when <= notificationOldRepeatPrevention.get(source)) {
                LOG.info("NOT processing notification, already sent newer notifications from this source.");
                return;
            }
        }

        // Ignore too frequent notifications, according to user preference
        long min_timeout = (long)prefs.getInt("notifications_timeout", 0) * 1000L;
        long cur_time = System.currentTimeMillis();
        if (notificationBurstPrevention.containsKey(source)) {
            long last_time = notificationBurstPrevention.get(source);
            if (cur_time - last_time < min_timeout) {
                LOG.info("Ignoring frequent notification, last one was " + (cur_time - last_time) + "ms ago");
                return;
            }
        }

        NotificationSpec notificationSpec = new NotificationSpec();

        // determinate Source App Name ("Label")
        String name = getAppName(source);
        if (name != null) {
            notificationSpec.sourceName = name;
        }

        boolean preferBigText = false;

        // Get the app ID that generated this notification. For now only used by pebble color, but may be more useful later.
        notificationSpec.sourceAppId = source;

        notificationSpec.type = AppNotificationType.getInstance().get(source);

        //FIXME: some quirks lookup table would be the minor evil here
        if (source.startsWith("com.fsck.k9")) {
            if (NotificationCompat.isGroupSummary(notification)) {
                LOG.info("ignore K9 group summary");
                return;
            }
            preferBigText = true;
        }

        if (notificationSpec.type == null) {
            notificationSpec.type = NotificationType.UNKNOWN;
        }

        // Get color
        notificationSpec.pebbleColor = getPebbleColorForNotification(notificationSpec);

        LOG.info("Processing notification " + notificationSpec.getId() + " age: " + (System.currentTimeMillis() - notification.when) + " from source " + source + " with flags: " + notification.flags);

        dissectNotificationTo(notification, notificationSpec, preferBigText);

        if (!checkNotificationContentForWhiteAndBlackList(sbn.getPackageName().toLowerCase(), notificationSpec.body)) {
            return;
        }


        // ignore Gadgetbridge's very own notifications, except for those from the debug screen
        if (getApplicationContext().getPackageName().equals(source)) {
            if (!getApplicationContext().getString(R.string.test_notification).equals(notificationSpec.title)) {
                return;
            }
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();


        if (actions.size() == 0 && NotificationCompat.isGroupSummary(notification)) { //this could cause #395 to come back
            LOG.info("Not forwarding notification, FLAG_GROUP_SUMMARY is set and no wearable action present. Notification flags: " + notification.flags);
            return;
        }

        notificationSpec.attachedActions = new ArrayList<>();

        // DISMISS action
        NotificationSpec.Action dismissAction = new NotificationSpec.Action();
        dismissAction.title = "Dismiss";
        dismissAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS;
        notificationSpec.attachedActions.add(dismissAction);

        for (NotificationCompat.Action act : actions) {
            if (act != null) {
                NotificationSpec.Action wearableAction = new NotificationSpec.Action();
                wearableAction.title = act.getTitle().toString();
                if(act.getRemoteInputs()!=null) {
                    wearableAction.type = NotificationSpec.Action.TYPE_WEARABLE_REPLY;
                } else {
                    wearableAction.type = NotificationSpec.Action.TYPE_WEARABLE_SIMPLE;
                }

                notificationSpec.attachedActions.add(wearableAction);
                mActionLookup.add((notificationSpec.getId()<<4) + notificationSpec.attachedActions.size(), act);
                LOG.info("found wearable action: " + notificationSpec.attachedActions.size() + " - "+ act.getTitle() + "  " + sbn.getTag());
            }
        }

        // OPEN action
        NotificationSpec.Action openAction = new NotificationSpec.Action();
        openAction.title = getString(R.string._pebble_watch_open_on_phone);
        openAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_OPEN;
        notificationSpec.attachedActions.add(openAction);

        // MUTE action
        NotificationSpec.Action muteAction = new NotificationSpec.Action();
        muteAction.title = getString(R.string._pebble_watch_mute);
        muteAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_MUTE;
        notificationSpec.attachedActions.add(muteAction);

        mNotificationHandleLookup.add(notificationSpec.getId(), sbn.getPostTime()); // for both DISMISS and OPEN
        mPackageLookup.add(notificationSpec.getId(), sbn.getPackageName()); // for MUTE

        notificationBurstPrevention.put(source, cur_time);
        if(0 != notification.when) {
            notificationOldRepeatPrevention.put(source, notification.when);
        }else {
            LOG.info("This app might show old/duplicate notifications. notification.when is 0 for " + source);
        }

        GBApplication.deviceService().onNotification(notificationSpec);
    }

    private boolean checkNotificationContentForWhiteAndBlackList(String packageName, String body) {
        long start = System.currentTimeMillis();

        List<String> wordsList = new ArrayList<>();
        NotificationFilter notificationFilter;

        try (DBHandler db = GBApplication.acquireDB()) {

            NotificationFilterDao notificationFilterDao = db.getDaoSession().getNotificationFilterDao();
            NotificationFilterEntryDao notificationFilterEntryDao = db.getDaoSession().getNotificationFilterEntryDao();

            Query<NotificationFilter> query = notificationFilterDao.queryBuilder().where(NotificationFilterDao.Properties.AppIdentifier.eq(packageName.toLowerCase())).build();
            notificationFilter = query.unique();

            if (notificationFilter == null) {
                LOG.debug("No Notification Filter found");
                return true;
            }

            LOG.debug("Loaded notification filter for '{}'", packageName);
            Query<NotificationFilterEntry> queryEntries = notificationFilterEntryDao.queryBuilder().where(NotificationFilterEntryDao.Properties.NotificationFilterId.eq(notificationFilter.getId())).build();

            List<NotificationFilterEntry> filterEntries = queryEntries.list();

            if (BuildConfig.DEBUG) {
                LOG.info("Database lookup took '{}' ms", System.currentTimeMillis() - start);
            }

            if (!filterEntries.isEmpty()) {
                for (NotificationFilterEntry temp : filterEntries) {
                    wordsList.add(temp.getNotificationFilterContent());
                    LOG.debug("Loaded filter word: " + temp.getNotificationFilterContent());
                }
            }

        } catch (Exception e) {
            LOG.error("Could not acquire DB.", e);
            return true;
        }

        return shouldContinueAfterFilter(body, wordsList, notificationFilter);
    }

    private void handleCallNotification(StatusBarNotification sbn) {
        String app = sbn.getPackageName();
        LOG.debug("got call from: " + app);
        if(app.equals("com.android.dialer")) {
            LOG.debug("Ignoring non-voip call");
            return;
        }
        Notification noti = sbn.getNotification();
        dumpExtras(noti.extras);
        if(noti.actions != null && noti.actions.length > 0) {
            for (Notification.Action action : noti.actions) {
                LOG.info("Found call action: " + action.title);
            }
            /*try {
                LOG.info("Executing first action");
                noti.actions[0].actionIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }*/
        }

        // figure out sender
        String number;
        if(noti.extras.containsKey(Notification.EXTRA_PEOPLE)) {
            number = noti.extras.getString(Notification.EXTRA_PEOPLE);
        } else if(noti.extras.containsKey(Notification.EXTRA_TITLE)) {
            number = noti.extras.getString(Notification.EXTRA_TITLE);
        } else {
            String appName = getAppName(app);
            number = appName != null ? appName : app;
        }
        activeCallPostTime = sbn.getPostTime();
        CallSpec callSpec = new CallSpec();
        callSpec.number = number;
        callSpec.command = CallSpec.CALL_INCOMING;
        GBApplication.deviceService().onSetCallState(callSpec);
    }

    boolean shouldContinueAfterFilter(@NonNull String body, @NonNull List<String> wordsList, @NonNull NotificationFilter notificationFilter) {

        LOG.debug("Mode: '{}' Submode: '{}' WordsList: '{}'", notificationFilter.getNotificationFilterMode(), notificationFilter.getNotificationFilterSubMode(), wordsList);

        boolean allMode = notificationFilter.getNotificationFilterSubMode() == NOTIFICATION_FILTER_SUBMODE_ALL;

        switch (notificationFilter.getNotificationFilterMode()) {
            case NOTIFICATION_FILTER_MODE_BLACKLIST:
                if (allMode) {
                    for (String word : wordsList) {
                        if (!body.contains(word)) {
                            LOG.info("Not every word was found, blacklist has no effect, processing continues.");
                            return true;
                        }
                    }
                    LOG.info("Every word was found, blacklist has effect, processing stops.");
                    return false;
                } else {
                    boolean containsAny = StringUtils.containsAny(body, wordsList.toArray(new CharSequence[0]));
                    if (!containsAny) {
                        LOG.info("No matching word was found, blacklist has no effect, processing continues.");
                    } else {
                        LOG.info("At least one matching word was found, blacklist has effect, processing stops.");
                    }
                    return !containsAny;
                }

            case NOTIFICATION_FILTER_MODE_WHITELIST:
                if (allMode) {
                    for (String word : wordsList) {
                        if (!body.contains(word)) {
                            LOG.info("Not every word was found, whitelist has no effect, processing stops.");
                            return false;
                        }
                    }
                    LOG.info("Every word was found, whitelist has effect, processing continues.");
                    return true;
                } else {
                    boolean containsAny = StringUtils.containsAny(body, wordsList.toArray(new CharSequence[0]));
                    if (containsAny) {
                        LOG.info("At least one matching word was found, whitelist has effect, processing continues.");
                    } else {
                        LOG.info("No matching word was found, whitelist has no effect, processing stops.");
                    }
                    return containsAny;
                }

            default:
                return true;
        }
    }

    // Strip Unicode control sequences: some apps like Telegram add a lot of them for unknown reasons
    private String sanitizeUnicode(String orig) {
        return orig.replaceAll("\\p{C}", "");
    }

    private void dissectNotificationTo(Notification notification, NotificationSpec notificationSpec,
                                       boolean preferBigText) {

        Bundle extras = NotificationCompat.getExtras(notification);

        //dumpExtras(extras);

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title != null) {
            notificationSpec.title = sanitizeUnicode(title.toString());
        }

        CharSequence contentCS = null;
        if (preferBigText && extras.containsKey(Notification.EXTRA_BIG_TEXT)) {
            contentCS = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT);
        } else if (extras.containsKey(Notification.EXTRA_TEXT)) {
            contentCS = extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
        }
        if (contentCS != null) {
            notificationSpec.body = sanitizeUnicode(contentCS.toString());
        }

    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
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
        LOG.info("Notification removed: " + sbn.getPackageName() + ": " + sbn.getNotification().category);
        if(Notification.CATEGORY_CALL.equals(sbn.getNotification().category) && activeCallPostTime == sbn.getPostTime()) {
            activeCallPostTime = 0;
            CallSpec callSpec = new CallSpec();
            callSpec.command = CallSpec.CALL_END;
            GBApplication.deviceService().onSetCallState(callSpec);
        }
        // FIXME: DISABLED for now
        /*
        if (shouldIgnore(sbn))
            return;

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("autoremove_notifications", false)) {
            LOG.info("notification removed, will ask device to delete it");
            GBApplication.deviceService().onDeleteNotification((int) sbn.getPostTime());
        }
        */
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

        return shouldIgnoreSource(sbn.getPackageName()) || shouldIgnoreNotification(
                sbn.getNotification(), sbn.getPackageName());

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

        if (GBApplication.appIsNotifBlacklisted(source)) {
            LOG.info("Ignoring notification, application is blacklisted");
            return true;
        }

        return false;
    }

    private boolean shouldIgnoreNotification(Notification notification, String source) {

        MediaSessionCompat.Token mediaSession = getMediaSession(notification);
        //try to handle media session notifications
        if (mediaSession != null && handleMediaSessionNotification(mediaSession))
            return true;

        NotificationType type = AppNotificationType.getInstance().get(source);
        //ignore notifications marked as LocalOnly https://developer.android.com/reference/android/app/Notification.html#FLAG_LOCAL_ONLY
        //some Apps always mark their notifcations as read-only
        if (NotificationCompat.getLocalOnly(notification) &&
                type != NotificationType.WECHAT &&
                type != NotificationType.OUTLOOK &&
                type != NotificationType.SKYPE) { //see https://github.com/Freeyourgadget/Gadgetbridge/issues/1109
            LOG.info("local only");
            return true;
        }

        Prefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean("notifications_generic_whenscreenon", false)) {
            PowerManager powermanager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powermanager != null && powermanager.isScreenOn()) {
//                LOG.info("Not forwarding notification, screen seems to be on and settings do not allow this");
                return true;
            }
        }

        return (notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT;

    }


    /**
     * Get the notification color that should be used for this Pebble notification.
     *
     * Note that this method will *not* edit the NotificationSpec passed in. It will only evaluate the PebbleColor.
     *
     * See Issue #815 on GitHub to see how notification colors are set.
     *
     * @param notificationSpec The NotificationSpec to read from.
     * @return Returns a PebbleColor that best represents this notification.
     */
    private byte getPebbleColorForNotification(NotificationSpec notificationSpec) {
        String appId = notificationSpec.sourceAppId;
        NotificationType existingType = notificationSpec.type;

        // If the notification type is known, return the associated color.
        if (existingType != NotificationType.UNKNOWN) {
            return existingType.color;
        }

        // Otherwise, we go and attempt to find the color from the app icon.
        Drawable icon;
        try {
            icon = getApplicationContext().getPackageManager().getApplicationIcon(appId);
            Objects.requireNonNull(icon);
        } catch (Exception ex) {
            // If we can't get the icon, we go with the default defined above.
            LOG.warn("Could not get icon for AppID " + appId, ex);
            return PebbleColor.IslamicGreen;
        }

        Bitmap bitmapIcon = BitmapUtil.convertDrawableToBitmap(icon);
        int iconPrimaryColor = new Palette.Builder(bitmapIcon)
                .generate()
                .getVibrantColor(Color.parseColor("#aa0000"));

        return PebbleUtils.getPebbleColor(iconPrimaryColor);
    }
}
