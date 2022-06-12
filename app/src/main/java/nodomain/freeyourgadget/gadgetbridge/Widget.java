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
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
import android.os.Bundle;
import android.view.View;
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
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class Widget extends AppWidgetProvider {
    public static final String WIDGET_CLICK = "nodomain.freeyourgadget.gadgetbridge.WidgetClick";
    public static final String APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";

    private static final Logger LOG = LoggerFactory.getLogger(Widget.class);
    static BroadcastReceiver broadcastReceiver = null;


    private long[] getSteps(GBDevice gbDevice) {
        Context context = GBApplication.getContext();
        Calendar day = GregorianCalendar.getInstance();

        if (!(context instanceof GBApplication)) {
            return new long[]{0, 0, 0};
        }
        DailyTotals ds = new DailyTotals();
        return ds.getDailyTotalsForDevice(gbDevice, day);
        //return ds.getDailyTotalsForAllDevices(day);
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {

        GBDevice deviceForWidget = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);
        if (deviceForWidget == null) {
            LOG.debug("Widget: no device, bailing out");
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        //onclick refresh
        Intent intent = new Intent(context, Widget.class);
        intent.setAction(WIDGET_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshDataIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.todaywidget_header_container, refreshDataIntent);

        //open GB main window
        Intent startMainIntent = new Intent(context, ControlCenterv2.class);
        PendingIntent startMainPIntent = PendingIntent.getActivity(context, 0, startMainIntent, 0);
        views.setOnClickPendingIntent(R.id.todaywidget_header_icon, startMainPIntent);

        //alarms popup menu
        Intent startAlarmListIntent = new Intent(context, WidgetAlarmsActivity.class);
        startAlarmListIntent.putExtra(GBDevice.EXTRA_DEVICE, deviceForWidget);
        PendingIntent startAlarmListPIntent = PendingIntent.getActivity(context, appWidgetId, startAlarmListIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.todaywidget_header_alarm_icon, startAlarmListPIntent);

        //charts
        Intent startChartsIntent = new Intent(context, ChartsActivity.class);
        startChartsIntent.putExtra(GBDevice.EXTRA_DEVICE, deviceForWidget);
        PendingIntent startChartsPIntent = PendingIntent.getActivity(context, appWidgetId, startChartsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.todaywidget_bottom_layout, startChartsPIntent);

        long[] dailyTotals = getSteps(deviceForWidget);
        int steps = (int) dailyTotals[0];
        int sleep = (int) dailyTotals[1];
        ActivityUser activityUser = new ActivityUser();
        int stepGoal = activityUser.getStepsGoal();
        int sleepGoal = activityUser.getSleepDurationGoal();
        int sleepGoalMinutes = sleepGoal * 60;
        int distanceGoal = activityUser.getDistanceGoalMeters() * 100;
        int stepLength = activityUser.getStepLengthCm();
        double distanceMeters = dailyTotals[0] * stepLength * 0.01;
        String distanceFormatted = FormatUtils.getFormattedDistanceLabel(distanceMeters);

        if (sleep < 1) {
            views.setViewVisibility(R.id.todaywidget_sleep_layout, View.GONE);
        } else {
            views.setViewVisibility(R.id.todaywidget_sleep_layout, View.VISIBLE);
        }

        views.setTextViewText(R.id.todaywidget_steps, String.format("%1s", steps));
        views.setTextViewText(R.id.todaywidget_sleep, String.format("%1s", getHM(sleep)));
        views.setTextViewText(R.id.todaywidget_distance, distanceFormatted);
        views.setProgressBar(R.id.todaywidget_steps_progress, stepGoal, steps, false);
        views.setProgressBar(R.id.todaywidget_sleep_progress, sleepGoalMinutes, sleep, false);
        views.setProgressBar(R.id.todaywidget_distance_progress, distanceGoal, steps * stepLength, false);
        views.setViewVisibility(R.id.todaywidget_battery_icon, View.GONE);
        if (deviceForWidget != null) {
            String status = String.format("%1s", deviceForWidget.getStateString());
            if (deviceForWidget.isConnected()) {
                if (deviceForWidget.getBatteryLevel() > 1) {
                    views.setViewVisibility(R.id.todaywidget_battery_icon, View.VISIBLE);

                    status = String.format("%1s%%", deviceForWidget.getBatteryLevel());
                }
            }

            String deviceName = deviceForWidget.getAlias() != null ? deviceForWidget.getAlias() : deviceForWidget.getName();
            views.setTextViewText(R.id.todaywidget_device_status, status);
            views.setTextViewText(R.id.todaywidget_device_name, deviceName);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public void refreshData(int appWidgetId) {
        Context context = GBApplication.getContext();
        GBDevice deviceForWidget = new WidgetPreferenceStorage().getDeviceForWidget(appWidgetId);

        if (deviceForWidget == null || !deviceForWidget.isInitialized()) {
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

    public void removeWidget(Context context, int appWidgetId) {
        WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
        widgetPreferenceStorage.removeWidgetById(context, appWidgetId);
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
            AndroidUtils.safeUnregisterBroadcastReceiver(context, broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        LOG.debug("gbwidget LOCAL onReceive, action: " + intent.getAction() + intent);
        Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        //this handles widget re-connection after apk updates
        if (WIDGET_CLICK.equals(intent.getAction())) {
            if (broadcastReceiver == null) {
                onEnabled(context);
            }
                refreshData(appWidgetId);
            //updateWidget();
        } else if (APPWIDGET_DELETED.equals(intent.getAction())) {
            onDisabled(context);
            removeWidget(context, appWidgetId);
        }
    }

}

