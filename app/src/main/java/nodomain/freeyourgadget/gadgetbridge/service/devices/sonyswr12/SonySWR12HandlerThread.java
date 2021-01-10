/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.sonyswr12.SonySWR12SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.SonySWR12Sample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.ActivityBase;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.ActivitySleep;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.ActivityWithData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventBase;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventCode;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventWithActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.EventWithValue;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;

public class SonySWR12HandlerThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SonySWR12HandlerThread.class);

    public SonySWR12HandlerThread(GBDevice gbDevice, Context context) {
        super(gbDevice, context);
    }

    public void process(EventBase event) {
        if (event instanceof EventWithValue) {
            if (event.getCode() == EventCode.HEART_RATE) {
                processRealTimeHeartRate((EventWithValue) event);
            }
        } else if (event instanceof EventWithActivity) {
            processWithActivity((EventWithActivity) event);
        }
    }

    private void processRealTimeHeartRate(EventWithValue event) {
        try {
            DBHandler dbHandler = GBApplication.acquireDB();
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            SonySWR12SampleProvider provider = new SonySWR12SampleProvider(getDevice(), dbHandler.getDaoSession());
            int timestamp = getTimestamp();
            SonySWR12Sample sample = new SonySWR12Sample(timestamp, deviceId, userId, (int) event.value, ActivitySample.NOT_MEASURED, 0, 1);
            provider.addGBActivitySample(sample);
            GBApplication.releaseDB();
            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, timestamp);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private int getTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private void processWithActivity(EventWithActivity event) {
        List<ActivityBase> payloadList = event.activityList;
        for (ActivityBase activity : payloadList) {
            switch (activity.getType()) {
                case WALK:
                case RUN:
                    addActivity((ActivityWithData) activity);
                    break;
                case SLEEP:
                    addSleep((ActivitySleep) activity);
                    break;
            }
        }
    }

    private void addActivity(ActivityWithData activity) {
        try {
            DBHandler dbHandler = GBApplication.acquireDB();
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            SonySWR12SampleProvider provider = new SonySWR12SampleProvider(getDevice(), dbHandler.getDaoSession());
            int kind = SonySWR12Constants.TYPE_ACTIVITY;
            SonySWR12Sample sample = new SonySWR12Sample(activity.getTimeStampSec(), deviceId, userId, ActivitySample.NOT_MEASURED, activity.data, kind, 1);
            provider.addGBActivitySample(sample);
            GBApplication.releaseDB();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void addSleep(ActivitySleep activity) {
        try {
            DBHandler dbHandler = GBApplication.acquireDB();
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            SonySWR12SampleProvider provider = new SonySWR12SampleProvider(getDevice(), dbHandler.getDaoSession());
            int kind;
            switch (activity.sleepLevel) {
                case LIGHT:
                    kind = SonySWR12Constants.TYPE_LIGHT;
                    break;
                case DEEP:
                    kind = SonySWR12Constants.TYPE_DEEP;
                    break;
                default:
                    kind = SonySWR12Constants.TYPE_ACTIVITY;
                    break;
            }
            if (kind == SonySWR12Constants.TYPE_LIGHT || kind == SonySWR12Constants.TYPE_DEEP) {
                //need so much samples because sleep has exact duration
                //so empty samples are for right representation of sleep on activity charts
                SonySWR12Sample sample = new SonySWR12Sample(activity.getTimeStampSec(), deviceId, userId, ActivitySample.NOT_MEASURED, 0, SonySWR12Constants.TYPE_NOT_WORN, 1);
                provider.addGBActivitySample(sample);
                sample = new SonySWR12Sample(activity.getTimeStampSec() + 2, deviceId, userId, ActivitySample.NOT_MEASURED, 0, kind, 1);
                provider.addGBActivitySample(sample);
                sample = new SonySWR12Sample(activity.getTimeStampSec() + activity.durationMin * 60 - 2, deviceId, userId, ActivitySample.NOT_MEASURED, 0, kind, 1);
                provider.addGBActivitySample(sample);
                sample = new SonySWR12Sample(activity.getTimeStampSec() + activity.durationMin * 60, deviceId, userId, ActivitySample.NOT_MEASURED, 0, SonySWR12Constants.TYPE_NOT_WORN, 1);
                provider.addGBActivitySample(sample);
            }
            GBApplication.releaseDB();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
