package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WeatherNotificationReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherNotificationReceiver.class);

    static class Weather implements Parcelable {
        // getters and setters suck ;)

        public long time = 0;
        public long queryTime = 0;
        public int version = 0;
        public String location = "";
        int currentTemp = 0;

        private Weather(Parcel in) {
            int version = in.readInt();
            if (version != 2) {
                LOG.info("wrong version" + version);
                return;
            }
            Bundle bundle = in.readBundle();
            location = bundle.getString("weather_location");
            time = bundle.getLong("weather_time");
            queryTime = bundle.getLong("weather_query_time");
            int conditions = bundle.getInt("weather_conditions");
            if (conditions > 0) {
                currentTemp = bundle.getInt("weather_current_temp");
            }
        }

        public static final Creator<Weather> CREATOR = new Creator<Weather>() {
            @Override
            public Weather createFromParcel(Parcel in) {
                return new Weather(in);
            }

            @Override
            public Weather[] newArray(int size) {
                return new Weather[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // we do not really want to use this at all
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().contains("WEATHER_UPDATE_2")) {
            LOG.info("Wrong action");
            return;
        }
        Bundle bundle = intent.getExtras();

        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            LOG.info(String.format("%s %s (%s)", key,
                    value.toString(), value.getClass().getName()));
        }

        if (!intent.hasExtra("ru.gelin.android.weather.notification.EXTRA_WEATHER")) {
            LOG.info("no weather extra");
            return;
        }

        Weather weather = intent.getParcelableExtra("ru.gelin.android.weather.notification.EXTRA_WEATHER");
        if (weather != null) {
            LOG.info("weather in " + weather.location + " is " + weather.currentTemp);
        }
    }
}