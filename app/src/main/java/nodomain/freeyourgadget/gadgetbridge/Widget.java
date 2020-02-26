/*  Copyright (C) 2019-2020 Andreas Shimokawa, vanous

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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.WidgetAlarmsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class Widget extends AppWidgetProvider {
    public static final String WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.WidgetClick";
    public static final String APPWIDGET_DELETED = "nodomain.freeyourgadget.gadgetbridge.APPWIDGET_DELETED";
    private static final Logger LOG = LoggerFactory.getLogger(Widget.class);
    static BroadcastReceiver broadcastReceiver = null;

    private GBDevice getSelectedDevice() {

        Context context = GBApplication.getContext();

        if (!(context instanceof GBApplication)) {
            return null;
        }

        GBApplication gbApp = (GBApplication) context;

        return gbApp.getDeviceManager().getSelectedDevice();
    }

    private long[] getSteps() {
        Context context = GBApplication.getContext();
        Calendar day = GregorianCalendar.getInstance();

        if (!(context instanceof GBApplication)) {
            return new long[]{0, 0, 0};
        }
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForAllDevices(day);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {

        GBDevice device = getSelectedDevice();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        //onclick refresh
        Intent intent = new Intent(context, Widget.class);
        intent.setAction(WIDGET_CLICK);
        PendingIntent refreshDataIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //views.setOnClickPendingIntent(R.id.todaywidget_bottom_layout, refreshDataIntent);
        views.setOnClickPendingIntent(R.id.todaywidget_header_bar, refreshDataIntent);

        //open GB main window
        Intent startMainIntent = new Intent(context, ControlCenterv2.class);
        PendingIntent startMainPIntent = PendingIntent.getActivity(context, 0, startMainIntent, 0);
        views.setOnClickPendingIntent(R.id.todaywidget_header_icon, startMainPIntent);

        //alarms popup menu
        Intent startAlarmListIntent = new Intent(context, WidgetAlarmsActivity.class);
        PendingIntent startAlarmListPIntent = PendingIntent.getActivity(context, 0, startAlarmListIntent, 0);
        views.setOnClickPendingIntent(R.id.todaywidget_header_plus, startAlarmListPIntent);

        //charts, requires device
        if (device != null) {
            Intent startChartsIntent = new Intent(context, ChartsActivity.class);
            startChartsIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            PendingIntent startChartsPIntent = PendingIntent.getActivity(context, 0, startChartsIntent, 0);
            views.setOnClickPendingIntent(R.id.todaywidget_bottom_layout, startChartsPIntent);
        }


        long[] dailyTotals = getSteps();

        views.setTextViewText(R.id.todaywidget_steps, context.getString(R.string.widget_steps_label,  dailyTotals[0]));
        views.setTextViewText(R.id.todaywidget_sleep, context.getString(R.string.widget_sleep_label, getHM(dailyTotals[1])));

        if (device != null) {
            String status = String.format("%1s", device.getStateString());
            if (device.isConnected()) {
                if (device.getBatteryLevel() > 1) {
                    status = String.format("Battery %1s%%", device.getBatteryLevel());
                }
            }

            views.setTextViewText(R.id.todaywidget_device_status, status);
            views.setTextViewText(R.id.todaywidget_device_name, device.getName());

        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public void refreshData() {
        Context context = GBApplication.getContext();
        GBDevice device = getSelectedDevice();

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
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

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
        if (broadcastReceiver == null) {
            LOG.debug("gbwidget BROADCAST receiver initialized.");
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LOG.debug("gbwidget BROADCAST, action" + intent.getAction());
                    updateWidget();
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GBApplication.ACTION_NEW_DATA);
            intentFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    @Override
    public void onDisabled(Context context) {

        if (broadcastReceiver != null) {
            AndroidUtils.safeUnregisterBroadcastReceiver(context,broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        LOG.debug("gbwidget LOCAL onReceive, action: " + intent.getAction());
        //this handles widget re-connection after apk updates
        if (WIDGET_CLICK.equals(intent.getAction())) {
            if (broadcastReceiver == null) {
                onEnabled(context);
            }
            refreshData();
            //updateWidget();
        } else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);
        }
    }

}

