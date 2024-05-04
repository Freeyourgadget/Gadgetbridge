/*  Copyright (C) 2015-2024 abettenburg, Andreas Böhler, Andreas Shimokawa,
    AndrewBedscastle, Arjan Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele
    Gobbetti, Davis Mosenkovs, Dmitriy Bogdanov, Dmitry Markin, Frank Slezak,
    gnufella, Gordon Williams, Hasan Ammar, José Rebelo, Julien Pivotto,
    Kevin Richter, mamucho, Matthieu Baerts, mvn23, Normano64, Petr Kadlec,
    Petr Vaněk, Steffen Liebergeld, Taavi Eomäe, theghostofheathledger, t-m-w,
    veecue, Zhong Jianxin

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilter;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterDao;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntry;
import nodomain.freeyourgadget.gadgetbridge.entities.NotificationFilterEntryDao;
import nodomain.freeyourgadget.gadgetbridge.externalevents.notifications.GoogleMapsNotificationHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.AppNotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_MODE_BLACKLIST;
import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_MODE_WHITELIST;
import static nodomain.freeyourgadget.gadgetbridge.activities.NotificationFilterActivity.NOTIFICATION_FILTER_SUBMODE_ALL;
import static nodomain.freeyourgadget.gadgetbridge.util.StringUtils.ensureNotNull;

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

    private final LimitedQueue<Integer, NotificationCompat.Action> mActionLookup = new LimitedQueue<>(32);
    private final LimitedQueue<Integer, String> mPackageLookup = new LimitedQueue<>(64);
    private final LimitedQueue<Integer, Long> mNotificationHandleLookup = new LimitedQueue<>(128);

    private final HashMap<String, Long> notificationBurstPrevention = new HashMap<>();
    private final HashMap<String, Long> notificationOldRepeatPrevention = new HashMap<>();

    private static final Set<String> GROUP_SUMMARY_WHITELIST = new HashSet<String>() {{
        add("com.microsoft.office.lync15");
        add("com.skype.raider");
        add("mikado.bizcalpro");
    }};

    public static final ArrayList<String> notificationStack = new ArrayList<>();
    private static final ArrayList<Integer> notificationsActive = new ArrayList<>();

    private static final Set<String> supportedPictureMimeTypes = new HashSet<String>() {{
        add("image/"); //for im.vector.app
        add("image/jpeg");
        add("image/png");
        add("image/gif");
        add("image/bmp");
        add("image/webp");
    }};

    private File notificationPictureCacheDirectory;

    private long activeCallPostTime;
    private int mLastCallCommand = CallSpec.CALL_UNDEFINED;

    private final Handler mHandler = new Handler();
    private Runnable mSetMusicInfoRunnable = null;
    private Runnable mSetMusicStateRunnable = null;

    private final GoogleMapsNotificationHandler googleMapsNotificationHandler = new GoogleMapsNotificationHandler();

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
                    Long ts = mNotificationHandleLookup.lookup(handle);
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
                            } catch (final PendingIntent.CanceledException e) {
                                LOG.error("Failed to open notification {}", sbn.getId());
                            }
                        }
                    }
                    break;
                }
                case ACTION_MUTE:
                    String packageName = mPackageLookup.lookup(handle);
                    if (packageName == null) {
                        LOG.info("could not lookup handle for mute action");
                        break;
                    }
                    LOG.info("going to mute {}", packageName);
                    if (GBApplication.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
                        GBApplication.addAppToNotifBlacklist(packageName);
                    } else {
                        GBApplication.removeFromAppsNotifBlacklist(packageName);
                    }
                    break;
                case ACTION_DISMISS: {
                    StatusBarNotification[] sbns = NotificationListener.this.getActiveNotifications();
                    Long ts = mNotificationHandleLookup.lookup(handle);
                    if (ts == null) {
                        LOG.info("could not lookup handle for dismiss action");
                        break;
                    }
                    for (StatusBarNotification sbn : sbns) {
                        if (sbn.getPostTime() == ts) {
                            String key = sbn.getKey();
                            NotificationListener.this.cancelNotification(key);
                        }
                    }
                    break;
                }
                case ACTION_DISMISS_ALL:
                    NotificationListener.this.cancelAllNotifications();
                    break;
                case ACTION_REPLY:
                    NotificationCompat.Action wearableAction = mActionLookup.lookup(handle);
                    String reply = intent.getStringExtra("reply");
                    if (wearableAction != null) {
                        PendingIntent actionIntent = wearableAction.getActionIntent();
                        if (actionIntent == null) {
                            LOG.warn("Action intent is null");
                            break;
                        }
                        Intent localIntent = new Intent();
                        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (wearableAction.getRemoteInputs() != null && wearableAction.getRemoteInputs().length > 0) {
                            RemoteInput[] remoteInputs = wearableAction.getRemoteInputs();
                            Bundle extras = new Bundle();
                            extras.putCharSequence(remoteInputs[0].getResultKey(), reply);
                            RemoteInput.addResultsToIntent(remoteInputs, localIntent, extras);
                        }
                        try {
                            LOG.info("will send exec intent to remote application");
                            actionIntent.send(context, 0, localIntent);
                            mActionLookup.remove(handle);
                        } catch (final PendingIntent.CanceledException e) {
                            LOG.warn("replyToLastNotification error", e);
                        }
                    } else {
                        LOG.warn("Received ACTION_REPLY but cannot find the corresponding wearableAction");
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
        createNotificationPictureCacheDirectory();
        cleanUpNotificationPictureProvider();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        notificationStack.clear();
        notificationsActive.clear();
        cleanUpNotificationPictureProvider();
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        onNotificationPosted(sbn, null);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        logNotification(sbn, true);

        notificationStack.remove(sbn.getPackageName());
        notificationStack.add(sbn.getPackageName());

        if (isServiceNotRunningAndShouldIgnoreNotifications()) return;

        final GBPrefs prefs = GBApplication.getPrefs();

        if (isOutsideNotificationTimes(prefs)) {
            return;
        }

        final boolean ignoreWorkProfile = prefs.getBoolean("notifications_ignore_work_profile", false);
        if (ignoreWorkProfile && isWorkProfile(sbn)) {
            LOG.debug("Ignoring notification from work profile");
            return;
        }

        final boolean mediaIgnoresAppList = prefs.getBoolean("notification_media_ignores_application_list", false);

        // If media notifications ignore app list, check them before
        if (mediaIgnoresAppList && handleMediaSessionNotification(sbn)) return;

        if (shouldIgnoreSource(sbn)) return;

        /* Check for navigation notifications and ignore if we're handling them */
        if (googleMapsNotificationHandler.handle(getApplicationContext(), sbn)) return;

        // If media notifications do NOT ignore app list, check them after
        if (!mediaIgnoresAppList && handleMediaSessionNotification(sbn)) return;

        int dndSuppressed = 0;
        if (rankingMap != null) {
            // Handle priority notifications for Do Not Disturb
            Ranking ranking = new Ranking();
            if (rankingMap.getRanking(sbn.getKey(), ranking)) {
                if (!ranking.matchesInterruptionFilter()) dndSuppressed = 1;
            }
        }

        if (prefs.getBoolean("notification_filter", false) && dndSuppressed == 1) {
            LOG.debug("Ignoring notification because of do not disturb");
            return;
        }

        if (NotificationCompat.CATEGORY_CALL.equals(sbn.getNotification().category)
                && prefs.getBoolean("notification_support_voip_calls", false)
                && (sbn.isOngoing() || shouldDisplayNonOngoingCallNotification(sbn))) {
            handleCallNotification(sbn);
            return;
        }

        if (shouldIgnoreNotification(sbn, false)) {
            if (!"com.sec.android.app.clockpackage".equals(sbn.getPackageName())) {  // workaround to allow phone alarm notification
                LOG.info("Ignoring notification: {}", sbn.getPackageName());          // need to fix
                return;
            }
        }

        String source = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        Long notificationOldRepeatPreventionValue = notificationOldRepeatPrevention.get(source);
        if (notificationOldRepeatPreventionValue != null
                && notification.when <= notificationOldRepeatPreventionValue
                && !shouldIgnoreRepeatPrevention(sbn)
        ) {
            LOG.info("NOT processing notification, already sent newer notifications from this source.");
            return;
        }

        // Ignore too frequent notifications, according to user preference
        long curTime = System.nanoTime();
        Long notificationBurstPreventionValue = notificationBurstPrevention.get(source);
        if (notificationBurstPreventionValue != null) {
            long diff = curTime - notificationBurstPreventionValue;
            if (diff < TimeUnit.SECONDS.toNanos(prefs.getInt("notifications_timeout", 0))) {
                LOG.info("Ignoring frequent notification, last one was {} ms ago", TimeUnit.NANOSECONDS.toMillis(diff));
                return;
            }
        }

        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.key = sbn.getKey();
        notificationSpec.when = notification.when;

        // determinate Source App Name ("Label")
        String name = NotificationUtils.getApplicationLabel(this, source);
        if (name != null) {
            notificationSpec.sourceName = name;
        }

        // Get the app ID that generated this notification. For now only used by pebble color, but may be more useful later.
        notificationSpec.sourceAppId = source;

        // Get the icon of the notification
        notificationSpec.iconId = notification.icon;

        notificationSpec.type = AppNotificationType.getInstance().get(source);

        //FIXME: some quirks lookup table would be the minor evil here
        if (source.startsWith("com.fsck.k9")) {
            if (NotificationCompat.isGroupSummary(notification)) {
                LOG.info("ignore K9 group summary");
                return;
            }
        }

        if (notificationSpec.type == null) {
            notificationSpec.type = NotificationType.UNKNOWN;
        }

        // Get color
        notificationSpec.pebbleColor = getPebbleColorForNotification(notificationSpec);

        LOG.info(
                "Processing notification {}, age: {}, source: {}, flags: {}",
                notificationSpec.getId(),
                (System.currentTimeMillis() - notification.when) ,
                source,
                notification.flags
        );

        boolean preferBigText = prefs.getBoolean("notification_prefer_long_text", true);

        dissectNotificationTo(notification, notificationSpec, preferBigText);

        if (notificationSpec.title != null || notificationSpec.body != null) {
            final String textToCheck = ensureNotNull(notificationSpec.title) + " " + ensureNotNull(notificationSpec.body);
            if (!checkNotificationContentForWhiteAndBlackList(sbn.getPackageName().toLowerCase(), textToCheck)) {
                return;
            }
        }

        // ignore Gadgetbridge's very own notifications, except for those from the debug screen
        if (getApplicationContext().getPackageName().equals(source)) {
            if (!getApplicationContext().getString(R.string.test_notification).equals(notificationSpec.title)) {
                return;
            }
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        // Some apps such as Telegram send both a group + normal notifications, which would get sent in duplicate to the devices
        // Others only send the group summary, so they need to be whitelisted
        if (actions.isEmpty() && NotificationCompat.isGroupSummary(notification)
                && !GROUP_SUMMARY_WHITELIST.contains(source)) { //this could cause #395 to come back
            LOG.info("Not forwarding notification, FLAG_GROUP_SUMMARY is set and no wearable action present. Notification flags: " + notification.flags);
            return;
        }

        notificationSpec.attachedActions = new ArrayList<>();
        notificationSpec.dndSuppressed = dndSuppressed;

        // DISMISS action
        NotificationSpec.Action dismissAction = new NotificationSpec.Action();
        dismissAction.title = "Dismiss";
        dismissAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS;
        notificationSpec.attachedActions.add(dismissAction);

        for (NotificationCompat.Action act : actions) {
            if (act != null) {
                NotificationSpec.Action wearableAction = new NotificationSpec.Action();
                wearableAction.title = act.getTitle().toString();
                if (act.getRemoteInputs() != null && act.getRemoteInputs().length > 0) {
                    wearableAction.type = NotificationSpec.Action.TYPE_WEARABLE_REPLY;
                } else {
                    wearableAction.type = NotificationSpec.Action.TYPE_WEARABLE_SIMPLE;
                }
                notificationSpec.attachedActions.add(wearableAction);
                wearableAction.handle = (notificationSpec.getId() << 4) + notificationSpec.attachedActions.size();
                mActionLookup.add((int)wearableAction.handle, act);
                LOG.info("Found wearable action: {} - {}  {}", notificationSpec.attachedActions.size(), act.getTitle(), sbn.getTag());
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

        notificationBurstPrevention.put(source, curTime);
        if (notification.when == 0) {
            LOG.info("This app might show old/duplicate notifications. notification.when is 0 for {}", source);
        } else if ((notification.when - System.currentTimeMillis()) > 30_000L) {
            // #4327 - Some apps such as outlook send reminder notifications in the future
            // If we add them to the oldRepeatPrevention, they never show up again
            LOG.info("This app might show old/duplicate notifications. notification.when is in the future for {}", source);
        } else {
            notificationOldRepeatPrevention.put(source, notification.when);
        }
        notificationsActive.add(notificationSpec.getId());
        // NOTE for future developers: this call goes to implementations of DeviceService.onNotification(NotificationSpec), like in GBDeviceService
        // this does NOT directly go to implementations of DeviceSupport.onNotification(NotificationSpec)!
        GBApplication.deviceService().onNotification(notificationSpec);
    }

    private static boolean isOutsideNotificationTimes(final GBPrefs prefs) {
        if (!prefs.getNotificationTimesEnabled()) {
            return false;
        }

        final LocalTime now = LocalTime.now();
        final LocalTime start = prefs.getNotificationTimesStart();
        final LocalTime end = prefs.getNotificationTimesEnd();
        final boolean shouldIgnore;
        if (start.isBefore(end)) {
            // eg. 06:00 -> 22:00
            shouldIgnore = now.isAfter(start) && now.isBefore(end);
        } else {
            // goes past midnight, eg. 22:00 -> 06:00
            shouldIgnore = now.isAfter(start) || now.isBefore(end);
        }

        if (shouldIgnore) {
            LOG.debug("Ignoring notification outside of notification times {}/{}", start, end);
        }

        return shouldIgnore;
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
        if (app.equals("com.android.dialer") || app.equals("com.android.incallui") || app.equals("com.google.android.dialer") || app.equals("com.asus.asusincallui") || app.equals("com.samsung.android.incallui")) {
            LOG.debug("Ignoring non-voip call");
            return;
        }
        Notification noti = sbn.getNotification();
        dumpExtras(noti.extras);
        boolean callStarted = false;
        if (noti.actions != null && noti.actions.length > 0) {
            for (Notification.Action action : noti.actions) {
                LOG.info("Found call action: " + action.title);
            }
            if (noti.actions.length == 1) {
                if (mLastCallCommand == CallSpec.CALL_INCOMING) {
                    LOG.info("There is only one call action and previous state was CALL_INCOMING, assuming call started");
                    callStarted = true;
                } else {
                    LOG.info("There is only one call action and previous state was not CALL_INCOMING, assuming outgoing call / duplicate notification and ignoring");
                    // FIXME: is there a way to detect transition CALL_OUTGOING -> CALL_START for more complete VoIP call state tracking?
                    return;
                }
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
        String appName = NotificationUtils.getApplicationLabel(this, app);
        if (noti.extras.containsKey(Notification.EXTRA_PEOPLE)) {
            number = noti.extras.getString(Notification.EXTRA_PEOPLE);
        } else if (noti.extras.containsKey(Notification.EXTRA_TITLE)) {
            number = noti.extras.getString(Notification.EXTRA_TITLE);
        } else {
            number = appName != null ? appName : app;
        }
        activeCallPostTime = sbn.getPostTime();
        CallSpec callSpec = new CallSpec();
        callSpec.number = number;
        callSpec.sourceAppId = app;
        if (appName != null) {
            callSpec.sourceName = appName;
        }
        callSpec.command = callStarted ? CallSpec.CALL_START : CallSpec.CALL_INCOMING;
        mLastCallCommand = callSpec.command;
        GBApplication.deviceService().onSetCallState(callSpec);
    }

    boolean shouldContinueAfterFilter(String body, @NonNull List<String> wordsList, @NonNull NotificationFilter notificationFilter) {
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

    // Strip Unicode control sequences: some apps like Telegram add a lot of them for unknown reasons.
    // Keep newline and whitespace characters
    private String sanitizeUnicode(String orig) {
        return orig.replaceAll("[\\p{C}&&\\S]", "");
    }

    private void dissectNotificationTo(Notification notification, NotificationSpec notificationSpec,
                                       boolean preferBigText) {

        Bundle extras = NotificationCompat.getExtras(notification);

        //dumpExtras(extras);
        if (extras == null) {
            return;
        }

        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title != null) {
            notificationSpec.title = sanitizeUnicode(title.toString());
        }

        CharSequence contentCS = null;
        final CharSequence bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT);
        if (preferBigText && !StringUtils.isBlank(bigText)) {
            contentCS = bigText;
        } else if (extras.containsKey(Notification.EXTRA_TEXT)) {
            contentCS = extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
        }
        if (contentCS != null) {
            notificationSpec.body = sanitizeUnicode(contentCS.toString());
        }

        NotificationCompat.MessagingStyle messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
        if (messagingStyle != null) {
            List<NotificationCompat.MessagingStyle.Message> messages = messagingStyle.getMessages();
            if (!messages.isEmpty()) {
                // Get the last message (assumed to be the most recent)
                NotificationCompat.MessagingStyle.Message lastMessage = messages.get(messages.size() - 1);

                if (supportedPictureMimeTypes.contains(lastMessage.getDataMimeType())) {
                    ContentResolver contentResolver = getContentResolver();
                    try (Cursor cursor = contentResolver.query(lastMessage.getDataUri(), null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                            notificationSpec.picturePath = cursor.getString(dataIndex);
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
        }

        if (extras.containsKey(NotificationCompat.EXTRA_PICTURE)) {
            final Bitmap bmp = (Bitmap) extras.get(NotificationCompat.EXTRA_PICTURE);
            File pictureFile = new File(this.notificationPictureCacheDirectory, String.valueOf(notificationSpec.getId()));

            try (FileOutputStream fos = new FileOutputStream(pictureFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                notificationSpec.picturePath = pictureFile.getAbsolutePath();
            } catch (IOException e) {
                LOG.error("Failed to save picture to notification cache: {}", e.getMessage());
            } finally {
                bmp.recycle();
            }
        }

        if (notificationSpec.type == NotificationType.COL_REMINDER
                && notificationSpec.body == null
                && notificationSpec.title != null) {
            notificationSpec.body = notificationSpec.title;
            notificationSpec.title = null;
        }
    }

    private boolean handleMediaSessionNotification(final StatusBarNotification sbn) {
        final MediaSession.Token token = sbn.getNotification().extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        return token != null && handleMediaSessionNotification(token);
    }

    /**
     * Try to handle media session notifications that tell info about the current play state.
     *
     * @param mediaSession The mediasession to handle.
     * @return true if notification was handled, false otherwise
     */
    public boolean handleMediaSessionNotification(MediaSession.Token mediaSession) {
        try {
            final MediaController c = new MediaController(getApplicationContext(), mediaSession);
            if (c.getMetadata() == null) {
                return false;
            }

            final MusicStateSpec stateSpec = MediaManager.extractMusicStateSpec(c.getPlaybackState());
            final MusicSpec musicSpec = MediaManager.extractMusicSpec(c.getMetadata());

            // finally, tell the device about it
            if (mSetMusicInfoRunnable != null) {
                mHandler.removeCallbacks(mSetMusicInfoRunnable);
            }
            mSetMusicInfoRunnable = new Runnable() {
                @Override
                public void run() {
                    GBApplication.deviceService().onSetMusicInfo(musicSpec);
                }
            };
            mHandler.postDelayed(mSetMusicInfoRunnable, 100);

            if (stateSpec != null) {
                if (mSetMusicStateRunnable != null) {
                    mHandler.removeCallbacks(mSetMusicStateRunnable);
                }
                mSetMusicStateRunnable = new Runnable() {
                    @Override
                    public void run() {
                        GBApplication.deviceService().onSetMusicState(stateSpec);
                    }
                };
            }
            mHandler.postDelayed(mSetMusicStateRunnable, 100);

            return true;
        } catch (final NullPointerException | SecurityException e) {
            LOG.error("Failed to handle media session notification", e);
            return false;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        logNotification(sbn, false);

        notificationStack.remove(sbn.getPackageName());

        if (isServiceNotRunningAndShouldIgnoreNotifications()) return;

        final GBPrefs prefs = GBApplication.getPrefs();

        if (isOutsideNotificationTimes(prefs)) {
            return;
        }

        final boolean ignoreWorkProfile = prefs.getBoolean("notifications_ignore_work_profile", false);
        if (ignoreWorkProfile && isWorkProfile(sbn)) {
            LOG.debug("Ignoring notification removal from work profile");
            return;
        }

        final boolean mediaIgnoresAppList = prefs.getBoolean("notification_media_ignores_application_list", false);

        // If media notifications ignore app list, check them before
        if (mediaIgnoresAppList && handleMediaSessionNotification(sbn)) return;

        if (shouldIgnoreSource(sbn)) return;

        googleMapsNotificationHandler.handleRemove(sbn);

        // If media notifications do NOT ignore app list, check them after
        if (!mediaIgnoresAppList && handleMediaSessionNotification(sbn)) return;

        if (Notification.CATEGORY_CALL.equals(sbn.getNotification().category)
                && activeCallPostTime == sbn.getPostTime()) {
            activeCallPostTime = 0;
            CallSpec callSpec = new CallSpec();
            callSpec.command = CallSpec.CALL_END;
            mLastCallCommand = callSpec.command;
            GBApplication.deviceService().onSetCallState(callSpec);
        }

        if (shouldIgnoreNotification(sbn, true)) return;

        // Build list of all currently active notifications
        ArrayList<Integer> activeNotificationsIds = new ArrayList<>();
        for (StatusBarNotification notification : getActiveNotifications()) {
            Integer id = mNotificationHandleLookup.lookupByValue(notification.getPostTime());
            if (id != null) {
                activeNotificationsIds.add(id);
            }
        }

        // Build list of notifications that aren't active anymore
        ArrayList<Integer> notificationsToRemove = new ArrayList<>();
        for (int notificationId : notificationsActive) {
            if (!activeNotificationsIds.contains(notificationId)) {
                notificationsToRemove.add(notificationId);
                deleteNotificationPicture(notificationId);
            }
        }

        // Clean up removed notifications from internal list
        notificationsActive.removeAll(notificationsToRemove);

        // Send notification remove request to device
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        for (GBDevice device : devices) {
            if (!device.isInitialized()) {
                continue;
            }

            Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
            if (devicePrefs.getBoolean("autoremove_notifications", true)) {
                for (int id : notificationsToRemove) {
                    LOG.info("Notification {} removed, deleting from {}", id, device.getAliasOrName());
                    GBApplication.deviceService(device).onDeleteNotification(id);
                }
            }
        }
    }

    private void deleteNotificationPicture(int notificationId) {
        File pictureFile = new File(this.notificationPictureCacheDirectory, String.valueOf(notificationId));
        if (pictureFile.exists())
            pictureFile.delete();
    }

    private void cleanUpNotificationPictureProvider() {
        File[] pictureFiles = this.notificationPictureCacheDirectory.listFiles();
        if (pictureFiles == null)
            return;

        for (File pictureFile : pictureFiles) {
            pictureFile.delete();
        }
    }

    private void createNotificationPictureCacheDirectory() {
        final File cacheDir = getApplicationContext().getExternalCacheDir();
        this.notificationPictureCacheDirectory = new File(cacheDir, "notification-pictures");
        this.notificationPictureCacheDirectory.mkdir();
    }
    private void logNotification(StatusBarNotification sbn, boolean posted) {
        LOG.debug(
                "Notification {} {}: packageName={}, when={}, priority={}, category={}",
                sbn.getId(),
                posted ? "posted" : "removed",
                sbn.getPackageName(),
                sbn.getNotification().when,
                sbn.getNotification().priority,
                sbn.getNotification().category
        );
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

    private boolean isServiceNotRunningAndShouldIgnoreNotifications() {
        /*
         * return early if DeviceCommunicationService is not running,
         * else the service would get started every time we get a notification.
         * unfortunately we cannot enable/disable NotificationListener at runtime like we do with
         * broadcast receivers because it seems to invalidate the permissions that are
         * necessary for NotificationListenerService
         */
        if (!DeviceCommunicationService.isRunning(this)) {
            LOG.trace("Service is not running, ignoring notification");
            return true;
        }
        return false;
    }

    private boolean shouldIgnoreSource(StatusBarNotification sbn) {
        String source = sbn.getPackageName();

        Prefs prefs = GBApplication.getPrefs();

        /* do not display messages from "android"
         * This includes keyboard selection message, usb connection messages, etc
         * Hope it does not filter out too much, we will see...
         */

        if (source.equals("android") ||
                source.equals("com.android.systemui") ||
                source.equals("com.android.dialer") ||
                source.equals("com.google.android.dialer") ||
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
                LOG.info("Ignoring notification, it's an sms notification");
                return true;
            }
        }

        if (GBApplication.getPrefs().getString("notification_list_is_blacklist", "true").equals("true")) {
            if (GBApplication.appIsNotifBlacklisted(source)) {
                LOG.info("Ignoring notification, application is blacklisted");
                return true;
            }
        } else {
            if (GBApplication.appIsNotifBlacklisted(source)) {
                LOG.info("Allowing notification, application is whitelisted");
                return false;
            } else {
                LOG.info("Ignoring notification, application is not whitelisted");
                return true;
            }
        }

        return false;
    }

    private boolean shouldIgnoreRepeatPrevention(StatusBarNotification sbn) {
        if (isFitnessApp(sbn)) {
            return true;
        }
        return false;
    }

    private boolean shouldIgnoreOngoing(StatusBarNotification sbn, NotificationType type) {
        if (isFitnessApp(sbn)) {
            return true;
        }
        if (type == NotificationType.COL_REMINDER) {
            return true;
        }
        return false;
    }

    private boolean isFitnessApp(StatusBarNotification sbn) {
        String source = sbn.getPackageName();
        if (source.equals("de.dennisguse.opentracks")
                || source.equals("de.dennisguse.opentracks.debug")
                || source.equals("de.dennisguse.opentracks.nightly")
                || source.equals("de.dennisguse.opentracks.playstore")
                || source.equals("de.tadris.fitness")
                || source.equals("de.tadris.fitness.debug")
        ) {
            return true;
        }

        return false;
    }

    private boolean isWorkProfile(StatusBarNotification sbn) {
        final UserHandle currentUser = Process.myUserHandle();
        return !sbn.getUser().equals(currentUser);
    }

    private boolean shouldDisplayNonOngoingCallNotification(StatusBarNotification sbn) {
        String source = sbn.getPackageName();
        NotificationType type = AppNotificationType.getInstance().get(source);

        if (type == NotificationType.TELEGRAM) {
            return true;
        }

        return false;
    }

    private boolean shouldIgnoreNotification(StatusBarNotification sbn, boolean remove) {
        Notification notification = sbn.getNotification();
        String source = sbn.getPackageName();

        NotificationType type = AppNotificationType.getInstance().get(source);
        //ignore notifications marked as LocalOnly https://developer.android.com/reference/android/app/Notification.html#FLAG_LOCAL_ONLY
        //some Apps always mark their notifcations as read-only
        if (NotificationCompat.getLocalOnly(notification) &&
                type != NotificationType.WECHAT &&
                type != NotificationType.TELEGRAM &&
                type != NotificationType.OUTLOOK &&
                type != NotificationType.COL_REMINDER &&
                type != NotificationType.SKYPE) { //see https://github.com/Freeyourgadget/Gadgetbridge/issues/1109
            LOG.info("Ignoring notification, local only");
            return true;
        }

        Prefs prefs = GBApplication.getPrefs();

        // Check for screen on when posting the notification; for removal, the screen
        // has to be on (obviously)
        if (!remove) {
            if (!prefs.getBoolean("notifications_generic_whenscreenon", false)) {
                PowerManager powermanager = (PowerManager) getSystemService(POWER_SERVICE);
                if (powermanager != null && powermanager.isScreenOn()) {
                    LOG.info("Not forwarding notification, screen seems to be on and settings do not allow this");
                    return true;
                }
            }
        }

        if (sbn.getNotification().priority < Notification.PRIORITY_DEFAULT) {
            if (prefs.getBoolean("notifications_ignore_low_priority", true)) {
                LOG.info("Ignoring low priority notification");
                return true;
            }
        }

        if (shouldIgnoreOngoing(sbn, type)) {
            return false;
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
            icon = NotificationUtils.getAppIcon(getApplicationContext(), appId);
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
