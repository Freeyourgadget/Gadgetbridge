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
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DailySteps;
import android.content.ComponentName;


public class StepsTodayWidget extends AppWidgetProvider {

    public static String getData(Context appContext, GBDevice device){
        Logger LOG = LoggerFactory.getLogger(StepsTodayWidget.class);
        LOG.info("PETR get data device: "+ device);

        DailySteps ds = new DailySteps();
        LOG.info("PETR " + String.valueOf(ds));
        String kroky = ds.loadItems(device);
        LOG.info("PETR  →" + kroky);
        return kroky;



    };

    public static final String ACTION =
            "nodomain.freeyourgadget.gadgetbridge.STEPS_TODAY_CLICK";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Logger LOG = LoggerFactory.getLogger(StepsTodayWidget.class);

        LOG.info("PETR running updateWidget");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.steps_today_widget);

        // Add our own click intent
        Intent intent = new Intent(context, StepsTodayWidget.class);
        intent.setAction(ACTION);
        PendingIntent clickPI = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.stepstodaywidget_text, clickPI);



        Context appContext = context.getApplicationContext();

        if (appContext instanceof GBApplication) {
            GBApplication gbApp = (GBApplication) appContext;
            GBDevice selectedDevice = gbApp.getDeviceManager().getDevices().get(0);
            views.setTextViewText(R.id.stepstodaywidget_text,getData(appContext,selectedDevice));
        }




        //views.setTextViewText(R.id.stepstodaywidget_text, String.valueOf(GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal)));

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
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION.equals(intent.getAction())) {
            Logger LOG = LoggerFactory.getLogger(StepsTodayWidget.class);
            LOG.info("PETR onReceive, click");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), StepsTodayWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

            onUpdate(context, appWidgetManager, appWidgetIds);


        }
    }


    //Calendar day = GregorianCalendar.getInstance();
    //GBDevice device = null;
    //DBHandler db = null;




}

