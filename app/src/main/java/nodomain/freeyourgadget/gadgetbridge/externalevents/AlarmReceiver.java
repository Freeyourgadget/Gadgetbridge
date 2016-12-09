package nodomain.freeyourgadget.gadgetbridge.externalevents;


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AlarmReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmReceiver.class);

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
        if (!GBApplication.getPrefs().getBoolean("send_sunrise_sunset", false)) {
            LOG.info("won't send sunrise and sunset events (disabled in preferences)");
            return;
        }
        LOG.info("will resend sunrise and sunset events");

        final GregorianCalendar dateTimeTomorrow = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        dateTimeTomorrow.set(Calendar.HOUR, 0);
        dateTimeTomorrow.set(Calendar.MINUTE, 0);
        dateTimeTomorrow.set(Calendar.SECOND, 0);
        dateTimeTomorrow.set(Calendar.MILLISECOND, 0);
        dateTimeTomorrow.add(GregorianCalendar.DAY_OF_MONTH, 1);

        /*
         * rotate ids ud reuse the id from two days ago for tomorrow, this way we will have
         * sunrise /sunset for 3 days while sending only sunrise/sunset per day
         */
        byte id_tomorrow = (byte) ((dateTimeTomorrow.getTimeInMillis() / (1000L * 60L * 60L * 24L)) % 3);

        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNRISE, id_tomorrow);
        GBApplication.deviceService().onDeleteCalendarEvent(CalendarEventSpec.TYPE_SUNSET, id_tomorrow);

        Prefs prefs = GBApplication.getPrefs();

        float latitude = prefs.getFloat("location_latitude", 0);
        float longitude = prefs.getFloat("location_longitude", 0);
        LOG.info("got longitude/latitude from preferences: " + latitude + "/" + longitude);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                prefs.getBoolean("use_updated_location_if_available", false)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            if (provider != null) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    latitude = (float) lastKnownLocation.getLatitude();
                    longitude = (float) lastKnownLocation.getLongitude();
                    LOG.info("got longitude/latitude from last known location: " + latitude + "/" + longitude);
                }
            }
        }
        GregorianCalendar[] sunriseTransitSetTomorrow = SPA.calculateSunriseTransitSet(dateTimeTomorrow, latitude, longitude, DeltaT.estimate(dateTimeTomorrow));

        CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
        calendarEventSpec.durationInSeconds = 0;
        calendarEventSpec.description = null;

        calendarEventSpec.type = CalendarEventSpec.TYPE_SUNRISE;
        calendarEventSpec.title = "Sunrise";
        if (sunriseTransitSetTomorrow[0] != null) {
            calendarEventSpec.id = id_tomorrow;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetTomorrow[0].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }

        calendarEventSpec.type = CalendarEventSpec.TYPE_SUNSET;
        calendarEventSpec.title = "Sunset";
        if (sunriseTransitSetTomorrow[2] != null) {
            calendarEventSpec.id = id_tomorrow;
            calendarEventSpec.timestamp = (int) (sunriseTransitSetTomorrow[2].getTimeInMillis() / 1000);
            GBApplication.deviceService().onAddCalendarEvent(calendarEventSpec);
        }
    }
}
