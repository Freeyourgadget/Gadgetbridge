package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gelin.android.weather.notification.ParcelableWeather2;


public class WeatherNotificationReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherNotificationReceiver.class);


    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().contains("WEATHER_UPDATE_2")) {
            LOG.info("Wrong action");
            return;
        }
        ParcelableWeather2 weather = null;
        try {
            weather = intent.getParcelableExtra("ru.gelin.android.weather.notification.EXTRA_WEATHER");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        if (weather != null) {
            LOG.info("weather in " + weather.location + " is " + weather.currentCondition + " (" + (weather.currentTemp - 273) + "°C)");

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = sharedPrefs.edit();
            edit.putString("weather_location", weather.location);
            edit.putString("weather_current_condition", weather.currentCondition);
            edit.putInt("weather_current_temp", weather.currentTemp);
            edit.apply();
        }
    }
}