/*  Copyright (C) 2019-2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class AutoConnectIntervalReceiver extends BroadcastReceiver {

    final DeviceCommunicationService service;
    static int mDelay = 4;
    private static final Logger LOG = LoggerFactory.getLogger(AutoConnectIntervalReceiver.class);

    public AutoConnectIntervalReceiver(DeviceCommunicationService service) {
        this.service = service;
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(DeviceManager.ACTION_DEVICES_CHANGED);
        LocalBroadcastManager.getInstance(service).registerReceiver(this, filterLocal);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        GBDevice gbDevice = service.getGBDevice();
        if (gbDevice == null) {
            return;
        }

        if (action.equals(DeviceManager.ACTION_DEVICES_CHANGED)) {
            if (gbDevice.isInitialized()) {
                LOG.info("will reset connection delay, device is initialized!");
                mDelay = 4;
            }
            else if (gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                scheduleReconnect();
            }
        }
        else if (action.equals("GB_RECONNECT")) {
            if (gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                LOG.info("Will re-connect to " + gbDevice.getAddress() + "(" + gbDevice.getName() + ")");
                GBApplication.deviceService().connect();
            }
        }
    }

    public void scheduleReconnect() {
        mDelay*=2;
        if (mDelay > 64) {
            mDelay = 64;
        }
        scheduleReconnect(mDelay);
    }

    public void scheduleReconnect(int delay) {
        LOG.info("scheduling reconnect in " + delay + " seconds");
        AlarmManager am = (AlarmManager) (GBApplication.getContext().getSystemService(Context.ALARM_SERVICE));
        Intent intent = new Intent("GB_RECONNECT");
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(GBApplication.getContext(), 0, intent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Calendar.getInstance().
                    getTimeInMillis() + delay * 1000, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().
                    getTimeInMillis() + delay * 1000, pendingIntent);
        }
    }

    public void destroy() {
        LocalBroadcastManager.getInstance(service).unregisterReceiver(this);
    }

}
