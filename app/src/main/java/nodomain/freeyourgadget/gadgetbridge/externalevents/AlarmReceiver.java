package nodomain.freeyourgadget.gadgetbridge.externalevents;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AlarmReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);

    public AlarmReceiver() {
        Context context = GBApplication.getContext();
        Intent intent = new Intent("DAILY_ALARM");
        intent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("DAILY_ALARM"), 0);
        AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 10000, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("will resend sunrise and sunset events");

        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNRISE, 1);
        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNRISE, 2);
        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNSET, 1);
        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNSET, 2);

        Prefs prefs = GBApplication.getPrefs();

        float latitude = prefs.getFloat("location_latitude", 0);
        float longitude = prefs.getFloat("location_longitude", 0);
        final GregorianCalendar dateTimeToday = new GregorianCalendar();
        final GregorianCalendar dateTimeTomorrow = new GregorianCalendar();
        dateTimeTomorrow.add(GregorianCalendar.DAY_OF_MONTH, 1);

        GregorianCalendar[] sunriseTransitSetToday = SPA.calculateSunriseTransitSet(dateTimeToday, latitude, longitude, DeltaT.estimate(dateTimeToday));
        GregorianCalendar[] sunriseTransitSetTomorrow = SPA.calculateSunriseTransitSet(dateTimeTomorrow, latitude, longitude, DeltaT.estimate(dateTimeToday));

        CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
        calendarEventSpec.durationInSeconds = 0;
        calendarEventSpec.description = null;

        calendarEventSpec.type = CalendarEventSpec.TYPE_SUNRISE;
        calendarEventSpec.title = "Sunrise";
        if (sunriseTransitSetToday[0] != null) {
            calendarEventSpec.id = 1;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetToday[0].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }
        if (sunriseTransitSetTomorrow[0] != null) {
            calendarEventSpec.id = 2;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetTomorrow[0].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }

        calendarEventSpec.type = CalendarEventSpec.TYPE_SUNSET;
        calendarEventSpec.title = "Sunset";
        if (sunriseTransitSetToday[2] != null) {
            calendarEventSpec.id = 1;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetToday[2].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }
        if (sunriseTransitSetTomorrow[2] != null) {
            calendarEventSpec.id = 2;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetTomorrow[2].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }
    }
}
