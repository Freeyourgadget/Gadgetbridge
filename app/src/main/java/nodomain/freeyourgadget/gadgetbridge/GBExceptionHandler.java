/*  Copyright (C) 2015-2024 Carsten Pfeiffer, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge;


import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_ID_ERROR;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import ch.qos.logback.classic.LoggerContext;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;

/**
 * Catches otherwise uncaught exceptions, logs them and terminates the app.
 */
public class GBExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GBExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler mDelegate;
    private final boolean mNotifyOnCrash;

    public GBExceptionHandler(Thread.UncaughtExceptionHandler delegate, final boolean notifyOnCrash) {
        mDelegate = delegate;
        mNotifyOnCrash = notifyOnCrash;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        LOG.error("Uncaught exception", ex);
        // flush the log buffers and stop logging
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();

        if (mNotifyOnCrash) {
            showNotification(ex);
        }

        if (mDelegate != null) {
            mDelegate.uncaughtException(thread, ex);
        } else {
            System.exit(1);
        }
    }

    private void showNotification(final Throwable e) {
        final Context context = GBApplication.getContext();

        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, Log.getStackTraceString(e));
        shareIntent.setType("text/plain");

        final PendingIntent pendingShareIntent = PendingIntentUtils.getActivity(
                context,
                0,
                Intent.createChooser(shareIntent, context.getString(R.string.app_crash_share_stacktrace)),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
        );

        final NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_share, context.getString(R.string.share), pendingShareIntent).build();

        final Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_HIGH_PRIORITY_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(
                        R.string.app_crash_notification_title,
                        context.getString(R.string.app_name)
                ))
                .setContentText(e.getLocalizedMessage())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(shareAction)
                .build();

        GB.notify(NOTIFICATION_ID_ERROR, notification, context);
    }
}
