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
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailySteps;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import android.content.ComponentName;
import android.widget.Toast;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class StepsTodayWidget extends AppWidgetProvider {

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


    public void setSteps() {
        Context context = GBApplication.getContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), StepsTodayWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.steps_today_widget);
        views.setTextViewText(R.id.stepstodaywidget_text, context.getString(R.string.appwidget_steps_today_text, getSteps()));
    }

    public static final String ACTION =
            "nodomain.freeyourgadget.gadgetbridge.STEPS_TODAY_CLICK";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.steps_today_widget);

        // Add our own click intent
        Intent intent = new Intent(context, StepsTodayWidget.class);
        intent.setAction(ACTION);

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
    }

    @Override
    public void onDisabled(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (ACTION.equals(intent.getAction())) {
            refreshData();
            setSteps();
        }
    }

}

