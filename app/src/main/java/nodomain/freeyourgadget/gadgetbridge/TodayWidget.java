/*  Copyright (C) 2016-2019 0nse, Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViews;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import android.content.ComponentName;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;


public class TodayWidget extends AppWidgetProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TodayWidget.class);
    private static final int UPDATE_INTERVAL_MILLIS = 1200000;
    private static final String TODAY_WIDGET_ALARM_UPDATE = "nodomain.freeyourgadget.gadgetbridge.TODAY_WIDGET_ALARM_UPDATE";
    public static final String TODAY_WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.TODAY_WIDGET_CLICK";
    public static final String NEW_DATA_ACTION = "nodomain.freeyourgadget.gadgetbridge.NEW_DATA_ACTION";
    public static final String APPWIDGET_DELETED = "nodomain.freeyourgadget.gadgetbridge.APPWIDGET_DELETED";
    public static final String ACTION_DEVICE_CHANGED = "nodomain.freeyourgadget.gadgetbridge.gbdevice.action.device_changed";
    public static GBDevice device;
    public BroadcastReceiver broadcastReceiver;

    public TodayWidget(){
        device=getSelectedDevice();
    }

    public static void setDevice()
    {
        device=getSelectedDevice();
        LOG.info("gbwidget setDevice device: " + device);
    }

    public static GBDevice getSelectedDevice() {
        Context context = GBApplication.getContext();
        if (!(context instanceof GBApplication)) {
            LOG.info("gbwidget getSelectedDevice no context");
            return null;
        }

        GBApplication gbApp = (GBApplication) context;

        return gbApp.getDeviceManager().getSelectedDevice();
    }

    public static float[] getSteps() {
        Context context = GBApplication.getContext();
        Calendar day = GregorianCalendar.getInstance();

        if (!(context instanceof GBApplication)) {
            return new float[]{0,0,0};
        }
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForAllDevices(day);
    }

    public void refreshData() {
        Context context = GBApplication.getContext();
        if (device == null || !device.isInitialized()) {
            GB.toast(context,
                    context.getString(R.string.device_not_connected),
                    Toast.LENGTH_SHORT, GB.ERROR);
            GBApplication.deviceService().connect();
            GB.toast(context,
                    context.getString(R.string.connecting),
                    Toast.LENGTH_SHORT, GB.INFO);

            return;
        }
        GB.toast(context,
                context.getString(R.string.busy_task_fetch_activity_data),
                Toast.LENGTH_SHORT, GB.INFO);

        GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
    }


    public void updateWidget() {
        Context context = GBApplication.getContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), TodayWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

    }

    private static String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        setDevice();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.today_widget);

        // Add our own click intent
        Intent intent = new Intent(context, TodayWidget.class);
        intent.setAction(TODAY_WIDGET_CLICK);

        PendingIntent clickPI = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.top_layout, clickPI);


        float[] DailyTotals = getSteps();


        views.setTextViewText(R.id.todaywidget_steps, context.getString(R.string.appwidget_today_steps,  (int)DailyTotals[0]));
        views.setTextViewText(R.id.todaywidget_sleep, context.getString(R.string.appwidget_today_sleep,  getHM((long)DailyTotals[1])));

        if (device !=null){
            String status = String.format("%1s" , device.getStateString());
            if (device.isConnected()) {
                if (device.getBatteryLevel() > 1) {
                    status = String.format("Battery: %1s%%, %1s", device.getBatteryLevel(), device.getStateString());
                }
            }

            views.setTextViewText(R.id.todaywidget_device_status, status);
            views.setTextViewText(R.id.todaywidget_device_name, device.getName());

        }


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onEnabled(Context context) {

        /*
        getting broadcasts from:
        nodomain/freeyourgadget/gadgetbridge/service/devices/huami/operations/AbstractFetchOperation.java
        handleActivityFetchFinish
        and
        nodomain/freeyourgadget/gadgetbridge/impl/GBDevice.java
        sendDeviceUpdateIntent

       better then polling or alarm
        */

        broadcastReceiver = new BroadcastReceiver() {
            @Override   
            public void onReceive(Context context, Intent intent) {
                LOG.info("gbwidget received new data " + intent.getAction());
                LOG.info("gbwidget extra data: "+ intent.getExtras());
                if (ACTION_DEVICE_CHANGED.equals(intent.getAction())){
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                        LOG.info("gbwidget device status: " + dev.getStateString());
                }
                updateWidget();
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NEW_DATA_ACTION);
        intentFilter.addAction(ACTION_DEVICE_CHANGED);
        GBApplication.getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onDisabled(Context context) {
        LOG.info("gbwidget onDisabled " + broadcastReceiver);
        if (broadcastReceiver != null) {
            GBApplication.getContext().unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context,intent);
        LOG.debug("gbwidget onReceive, intent: " + intent.getAction());

        if (TODAY_WIDGET_CLICK.equals(intent.getAction())) {
            refreshData();
        }else if (TODAY_WIDGET_ALARM_UPDATE.equals(intent.getAction())) {
            updateWidget();
        }else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);

        } else {
            super.onReceive(context, intent);
        }
    }

}

