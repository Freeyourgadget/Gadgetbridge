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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViews;

import nodomain.freeyourgadget.gadgetbridge.activities.GBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailySteps;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import android.content.ComponentName;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;


public class StepsTodayWidget extends AppWidgetProvider {
    private static final Logger LOG = LoggerFactory.getLogger(StepsTodayWidget.class);
    private static final int UPDATE_INTERVAL_MILLIS = 1200000;
    private static final String STEPS_TODAY_WIDGET_ALARM_UPDATE = "nodomain.freeyourgadget.gadgetbridge.STEPS_TODAY_WIDGET_ALARM_UPDATE";
    public static final String STEPS_TODAY_WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.STEPS_TODAY_WIDGET_CLICK";
    public static final String NEW_DATA_ACTION = "nodomain.freeyourgadget.gadgetbridge.NEW_DATA_ACTION";
    public static final String APPWIDGET_DELETED = "nodomain.freeyourgadget.gadgetbridge.APPWIDGET_DELETED";

    public BroadcastReceiver broadcastReceiver;


    public GBDevice getSelectedDevice() {
        Context context = GBApplication.getContext();
        if (!(context instanceof GBApplication)) {
            return null;
        }

        GBApplication gbApp = (GBApplication) context;
        return gbApp.getDeviceManager().getSelectedDevice();
    }

    public static int getSteps() {
        Context context = GBApplication.getContext();
        Calendar day = GregorianCalendar.getInstance();

        if (!(context instanceof GBApplication)) {
            return 0;
        }
        DailySteps ds = new DailySteps();
        return ds.getDailyStepsForAllDevices(day);
    }

    public void refreshData() {
        Context context = GBApplication.getContext();
        GBDevice selectedDevice = getSelectedDevice();
        if (selectedDevice == null || !selectedDevice.isInitialized()) {
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
                context.getString(R.string.device_fetching_data),
                Toast.LENGTH_SHORT, GB.INFO);

        GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
    }


    public void updateSteps() {
        Context context = GBApplication.getContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), StepsTodayWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.steps_today_widget);
        //views.setTextViewText(R.id.stepstodaywidget_text, context.getString(R.string.appwidget_steps_today_text, getSteps()));
        onUpdate(context, appWidgetManager, appWidgetIds);

    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.steps_today_widget);

        // Add our own click intent
        Intent intent = new Intent(context, StepsTodayWidget.class);
        intent.setAction(STEPS_TODAY_WIDGET_CLICK);

        PendingIntent clickPI = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.stepstodaywidget_text, clickPI);

        views.setTextViewText(R.id.stepstodaywidget_text, context.getString(R.string.appwidget_steps_today_text,  getSteps()));

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

        //getting broadcast from
        //nodomain/freeyourgadget/gadgetbridge/service/devices/huami/operations/AbstractFetchOperation.java
        //handleActivityFetchFinish
        //not perfect yet, but better then polling or alarm

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LOG.info("gbwidget received new data " + intent.getAction());
                updateSteps();
            }
        };
        GBApplication.getContext().registerReceiver(broadcastReceiver, new IntentFilter(NEW_DATA_ACTION));
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
        LOG.debug("gbwidget onReceive, intent: " + intent.getAction());

        if (STEPS_TODAY_WIDGET_CLICK.equals(intent.getAction())) {
            refreshData();
        }else if (STEPS_TODAY_WIDGET_ALARM_UPDATE.equals(intent.getAction())) {
            updateSteps();
        }else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);

        } else {
            super.onReceive(context, intent);
        }
    }

}

