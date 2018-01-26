/*  Copyright (C) 2017 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Process;
import android.support.v4.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.isRunningMarshmallowOrLater;

/**
 * Original source by xinghui - see https://gist.github.com/xinghui/b2ddd8cffe55c4b62f5d8846d5545bf9
 * NB: no license specified in the source code as of 2017-04-19
 */
public class NotificationCollectorMonitorService extends Service {
    public static final String NOTIFICATION_CHANNEL_ID = "gadgetbridge";
    private static final Logger LOG = LoggerFactory.getLogger(NotificationCollectorMonitorService.class);

    @Override
    @SuppressLint("NewApi")
    public void onCreate() {
        super.onCreate();
        if (isRunningMarshmallowOrLater()) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);

            // Create notification channel required
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            // Placeholder notification
            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Gadgetbridge")
                    .setContentText("")
                    .build();

            startForeground(1, notification);
        }

        ensureCollectorRunning();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void ensureCollectorRunning() {
        ComponentName collectorComponent = new ComponentName(this, NotificationListener.class);
        LOG.info("ensureCollectorRunning collectorComponent: " + collectorComponent);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            LOG.info("ensureCollectorRunning() runningServices is NULL");
            return;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                LOG.warn("ensureCollectorRunning service - pid: " + service.pid + ", currentPID: " + Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " + service.clientCount
                        + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    collectorRunning = true;
                }
            }
        }
        if (collectorRunning) {
            LOG.debug("ensureCollectorRunning: collector is running");
            return;
        }
        LOG.debug("ensureCollectorRunning: collector not running, reviving...");
        toggleNotificationListenerService();
    }

    private void toggleNotificationListenerService() {
        LOG.debug("toggleNotificationListenerService() called");
        ComponentName thisComponent = new ComponentName(this, NotificationListener.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}