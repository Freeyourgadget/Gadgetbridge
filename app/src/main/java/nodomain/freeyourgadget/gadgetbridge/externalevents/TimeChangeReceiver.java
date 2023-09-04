/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer

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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.zone.ZoneOffsetTransition;
import org.threeten.bp.zone.ZoneRules;

import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class TimeChangeReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(TimeChangeReceiver.class);

    public static final String ACTION_DST_CHANGED = "nodomain.freeyourgadget.gadgetbridge.DST_CHANGED";

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
            case ACTION_DST_CHANGED:
                // Continue after the switch
                break;
            default:
                LOG.warn("Unknown action {}", action);
                return;
        }

        final Date newTime = GregorianCalendar.getInstance().getTime();
        LOG.info("Time or Timezone changed, syncing with device: {} ({}), {}", DateTimeUtils.formatDate(newTime), newTime.toGMTString(), intent.getAction());
        GBApplication.deviceService().onSetTime();

        // Reschedule the next DST change, since the timezone may have changed
        scheduleNextDstChange(context);
    }

    /**
     * Schedule an alarm to trigger on the next DST change, since ACTION_TIMEZONE_CHANGED is not broadcast otherwise.
     *
     * @param context the context
     */
    public static void scheduleNextDstChange(final Context context) {
        final ZoneId zoneId = ZoneId.systemDefault();
        final ZoneRules zoneRules = zoneId.getRules();
        final Instant now = Instant.now();
        final ZoneOffsetTransition transition = zoneRules.nextTransition(now);
        if (transition == null) {
            LOG.warn("No DST transition found for {}", zoneId);
            return;
        }

        final long nextDstMillis = transition.getInstant().toEpochMilli();
        final long delayMillis = nextDstMillis - now.toEpochMilli() + 5000L;

        final Intent i = new Intent(ACTION_DST_CHANGED);
        final PendingIntent pi = PendingIntentUtils.getBroadcast(context, 0, i, 0, false);

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final boolean exactAlarm = canScheduleExactAlarms(context, am);

        LOG.info("Scheduling next DST change: {} (in {} millis) (exact = {})", nextDstMillis, delayMillis, exactAlarm);

        am.cancel(pi);

        boolean scheduledExact = false;
        if (exactAlarm) {
            try {
                am.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMillis, pi);
                scheduledExact = true;
            } catch (final Exception e) {
                LOG.error("Failed to schedule exact alarm for next DST change", e);
            }
        }

        // Fallback to inexact alarm if the exact one failed
        if (!scheduledExact) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMillis, pi);
                } else {
                    am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMillis, pi);
                }
            } catch (final Exception e) {
                LOG.error("Failed to schedule inexact alarm next DST change", e);
            }
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
