package ru.gelin.android.weather.notification;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParcelableWeather2 implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(ParcelableWeather2.class);

    // getters and setters suck ;)

    public long time = 0;
    public long queryTime = 0;
    public int version = 0;
    public String location = "";
    public int currentTemp = 0;

    private ParcelableWeather2(Parcel in) {
        int version = in.readInt();
        if (version != 2) {
            return;
        }
        Bundle bundle = in.readBundle(this.getClass().getClassLoader());
        location = bundle.getString("weather_location");
        time = bundle.getLong("weather_time");
        queryTime = bundle.getLong("weather_query_time");
        bundle.getString("weather_forecast_url");
        int conditions = bundle.getInt("weather_conditions");
        if (conditions > 0) {
            Bundle conditionBundle = in.readBundle(this.getClass().getClassLoader());
            conditionBundle.getString("weather_condition_text");
            conditionBundle.getStringArray("weather_condition_types");
            currentTemp = conditionBundle.getInt("weather_current_temp");

        }
    }

    public static final Creator<ParcelableWeather2> CREATOR = new Creator<ParcelableWeather2>() {
        @Override
        public ParcelableWeather2 createFromParcel(Parcel in) {
            return new ParcelableWeather2(in);
        }

        @Override
        public ParcelableWeather2[] newArray(int size) {
            return new ParcelableWeather2[size];
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
