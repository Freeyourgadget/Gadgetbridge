/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo

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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class TimeChangeReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(TimeChangeReceiver.class);

    public static final String ACTION_DST_CHANGED_OR_PERIODIC_SYNC = "nodomain.freeyourgadget.gadgetbridge.DST_CHANGED_OR_PERIODIC_SYNC";
    public static final long PERIODIC_SYNC_INTERVAL_MS = 158003000; // 43:53:23.000
    public static final long PERIODIC_SYNC_INTERVAL_MAX_MS = 172800000; // 48 hours

    @Override
    public void onReceive(Context context, Intent intent) {
        final Prefs prefs = GBApplication.getPrefs();
        final String action = intent.getAction();
        if (action == null) {
            LOG.warn("Null action");
            return;
        }

        if (!prefs.getBoolean("datetime_synconconnect", true)) {
            LOG.warn("Ignoring time change for {}, time sync is disabled", action);
            return;
        }

        switch (action) {
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case ACTION_DST_CHANGED_OR_PERIODIC_SYNC:
                // Continue after the switch
                break;
            default:
                LOG.warn("Unknown action {}", action);
                return;
        }

        // acquire wake lock, otherwise device might enter deep sleep immediately after returning from onReceive()
        AndroidUtils.acquirePartialWakeLock(context, "TimeSyncWakeLock", 10100);

        final Date newTime = GregorianCalendar.getInstance().getTime();
        LOG.info("Time/Timezone changed or periodic sync, syncing with device: {} ({}), {}", DateTimeUtils.formatDate(newTime), newTime.toGMTString(), intent.getAction());
        GBApplication.deviceService().onSetTime();

        // Reschedule the next DST change (since the timezone may have changed) or periodic sync
        scheduleNextDstChangeOrPeriodicSync(context);
    }

    /**
     * Schedule an alarm to trigger on the next DST change, since ACTION_TIMEZONE_CHANGED is not broadcast otherwise
     * or schedule an alarm to trigger after PERIODIC_SYNC_INTERVAL_MS (whichever is earlier).
     *
     * @param context the context
     */
    public static void scheduleNextDstChangeOrPeriodicSync(final Context context) {
        final ZoneId zoneId = ZoneId.systemDefault();
        final ZoneRules zoneRules = zoneId.getRules();
        final Instant now = Instant.now();
        final ZoneOffsetTransition transition = zoneRules.nextTransition(now);

        final Intent i = new Intent(ACTION_DST_CHANGED_OR_PERIODIC_SYNC);
        i.setPackage(BuildConfig.APPLICATION_ID);
        final PendingIntent pi = PendingIntentUtils.getBroadcast(context, 0, i, 0, false);

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        boolean exactAlarm = false;
        long delayMillis = PERIODIC_SYNC_INTERVAL_MS;

        if (transition != null) {
            final long nextDstMillis = transition.getInstant().toEpochMilli();
            final long dstDelayMillis = nextDstMillis - now.toEpochMilli() + 5000L;
            if (dstDelayMillis < PERIODIC_SYNC_INTERVAL_MAX_MS) {
                exactAlarm = canScheduleExactAlarms(context, am);
                delayMillis = dstDelayMillis;
                LOG.info("Scheduling next DST change: {} (in {} millis) (exact = {})", nextDstMillis, delayMillis, exactAlarm);
            }
        } else {
            LOG.warn("No DST transition found for {}", zoneId);
        }

        if (delayMillis == PERIODIC_SYNC_INTERVAL_MS) {
            LOG.info("Scheduling next periodic time sync in {} millis (exact = {})", delayMillis, exactAlarm);
        }

        am.cancel(pi);

        boolean scheduledExact = false;
        if (exactAlarm) {
            try {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pi);
                scheduledExact = true;
            } catch (final Exception e) {
                LOG.error("Failed to schedule exact alarm for next DST change or periodic time sync", e);
            }
        }

        // Fallback to inexact alarm if the exact one failed
        if (!scheduledExact) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pi);
                } else {
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pi);
                }
            } catch (final Exception e) {
                LOG.error("Failed to schedule inexact alarm for next DST change or periodic time sync", e);
            }
        }
    }

    public static void ifEnabledScheduleNextDstChangeOrPeriodicSync(final Context context) {
        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            scheduleNextDstChangeOrPeriodicSync(context);
        }
    }

    private static boolean canScheduleExactAlarms(final Context context, final AlarmManager am) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return am.canScheduleExactAlarms();
        } else {
            return GB.checkPermission(context, "android.permission.SCHEDULE_EXACT_ALARM");
        }
    }
}
