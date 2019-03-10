/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Taavi Eomäe

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
package ru.gelin.android.weather.notification;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class ParcelableWeather2 implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(ParcelableWeather2.class);

    // getters and setters suck ;)
    public WeatherSpec weatherSpec = new WeatherSpec();

    public JSONObject reconstructedOWMForecast = null;

    private ParcelableWeather2(Parcel in) {
        int version = in.readInt();
        if (version != 2) {
            return;
        }
        Bundle bundle = in.readBundle(getClass().getClassLoader());

        weatherSpec.location = bundle.getString("weather_location");
        long time = bundle.getLong("weather_time");
        long queryTime = bundle.getLong("weather_query_time");
        weatherSpec.timestamp = (int) (queryTime / 1000);
        bundle.getString("weather_forecast_url");
        int conditions = bundle.getInt("weather_conditions");
        if (conditions > 0) {
            Bundle conditionBundle = in.readBundle(getClass().getClassLoader());
            weatherSpec.currentCondition = conditionBundle.getString("weather_condition_text");
            conditionBundle.getStringArray("weather_condition_types");
            weatherSpec.currentTemp = conditionBundle.getInt("weather_current_temp");

            weatherSpec.windDirection = mapDirToDeg(conditionBundle.getString("weather_wind_direction"));
            weatherSpec.windSpeed = getSpeedInKMH(conditionBundle.getInt("weather_wind_speed"),
                    conditionBundle.getString("weather_wind_speed_unit"));

            String[] currentConditionType = conditionBundle.getStringArray("weather_condition_types");
            if (currentConditionType != null) {
                weatherSpec.currentConditionCode = weatherConditionTypesToOpenWeatherMapIds(currentConditionType[0]);
            }
            weatherSpec.todayMinTemp = conditionBundle.getInt("weather_low_temp");
            weatherSpec.todayMaxTemp = conditionBundle.getInt("weather_high_temp");
            weatherSpec.currentHumidity = conditionBundle.getInt("weather_humidity_value");

            //fetch forecasts
            int timeOffset = 0;

            JSONArray list = new JSONArray();
            JSONObject city = new JSONObject();
            while (--conditions > 0) {
                timeOffset += 86400000; //manually determined
                JSONObject item = new JSONObject();
                JSONObject condition = new JSONObject();
                JSONObject main = new JSONObject();
                JSONArray weather = new JSONArray();
                Bundle forecastBundle = in.readBundle(getClass().getClassLoader());
                String[] forecastConditionType = forecastBundle.getStringArray("weather_condition_types");
                int forecastConditionCode = 0;
                if (forecastConditionType != null) {
                    forecastConditionCode = weatherConditionTypesToOpenWeatherMapIds(forecastConditionType[0]);
                }
                int forecastLowTemp = forecastBundle.getInt("weather_low_temp");
                int forecastHighTemp = forecastBundle.getInt("weather_high_temp");
                int forecastHumidity = forecastBundle.getInt("weather_humidity_value");
                weatherSpec.forecasts.add(new WeatherSpec.Forecast(forecastLowTemp, forecastHighTemp, forecastConditionCode, forecastHumidity));
                try {
                    condition.put("id", forecastConditionCode);
                    condition.put("main", forecastBundle.getString("weather_condition_text"));
                    condition.put("description", forecastBundle.getString("weather_condition_text"));
                    condition.put("icon", Weather.mapToOpenWeatherMapIcon(forecastConditionCode));
                    weather.put(condition);

                    main.put("temp", forecastBundle.getInt("weather_current_temp"));
                    main.put("humidity", forecastHumidity);
                    main.put("temp_min", forecastLowTemp);
                    main.put("temp_max", forecastHighTemp);

                    //forecast

                    item.put("dt", (time / 1000) + timeOffset);
                    item.put("main", main);
                    item.put("weather", weather);
                    list.put(item);
                } catch (JSONException e) {
                    LOG.error("error while construction JSON", e);
                }
            }
            try {
                //"city":{"id":3181913,"name":"Bolzano","coord":{"lat":46.4927,"lon":11.3336},"country":"IT"}
                city.put("name", weatherSpec.location);
                city.put("country", "World");

                reconstructedOWMForecast = new JSONObject();
                reconstructedOWMForecast.put("city", city);
                reconstructedOWMForecast.put("cnt", list.length());
                reconstructedOWMForecast.put("list", list);

            } catch (JSONException e) {
                LOG.error("error while construction JSON", e);
            }
            LOG.debug("Forecast JSON for Webview: " + reconstructedOWMForecast);
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

    private int weatherConditionTypesToOpenWeatherMapIds(String weather_condition_type) {
        switch (weather_condition_type) {
            case "THUNDERSTORM_RAIN_LIGHT":
                return 200;
            case "THUNDERSTORM_RAIN":
                return 201;
            case "THUNDERSTORM_RAIN_HEAVY":
                return 202;
            case "THUNDERSTORM_LIGHT":
                return 210;
            case "THUNDERSTORM":
                return 211;
            case "THUNDERSTORM_HEAVY":
                return 212;
            case "THUNDERSTORM_RAGGED":
                return 221;
            case "THUNDERSTORM_DRIZZLE_LIGHT":
                return 230;
            case "THUNDERSTORM_DRIZZLE":
                return 231;
            case "THUNDERSTORM_DRIZZLE_HEAVY":
                return 232;

            case "DRIZZLE_LIGHT":
                return 300;
            case "DRIZZLE":
                return 301;
            case "DRIZZLE_HEAVY":
                return 302;
            case "DRIZZLE_RAIN_LIGHT":
                return 310;
            case "DRIZZLE_RAIN":
                return 311;
            case "DRIZZLE_RAIN_HEAVY":
                return 312;
            case "DRIZZLE_SHOWER":
                return 321;

            case "RAIN_LIGHT":
                return 500;
            case "RAIN":
                return 501;
            case "RAIN_HEAVY":
                return 502;
            case "RAIN_VERY_HEAVY":
                return 503;
            case "RAIN_EXTREME":
                return 504;
            case "RAIN_FREEZING":
                return 511;
            case "RAIN_SHOWER_LIGHT":
                return 520;
            case "RAIN_SHOWER":
                return 521;
            case "RAIN_SHOWER_HEAVY":
                return 522;

            case "SNOW_LIGHT":
                return 600;
            case "SNOW":
                return 601;
            case "SNOW_HEAVY":
                return 602;
            case "SLEET":
                return 611;
            case "SNOW_SHOWER":
                return 621;

            case "MIST":
                return 701;
            case "SMOKE":
                return 711;
            case "HAZE":
                return 721;
            case "SAND_WHIRLS":
                return 731;
            case "FOG":
                return 741;

            case "CLOUDS_CLEAR":
                return 800;
            case "CLOUDS_FEW":
                return 801;
            case "CLOUDS_SCATTERED":
                return 802;
            case "CLOUDS_BROKEN":
                return 803;
            case "CLOUDS_OVERCAST":
                return 804;

            case "TORNADO":
                return 900;
            case "TROPICAL_STORM":
                return 901;
            case "HURRICANE":
                return 902;
            case "COLD":
                return 903;
            case "HOT":
                return 904;
            case "WINDY":
                return 905;
            case "HAIL":
                return 906;
        }
        return 3200;
    }

    private int getSpeedInKMH(int speed, String incomingUnit) {
        float kmhSpeed = 0;
        switch (incomingUnit) {
            case "MPS":
                kmhSpeed = speed * 3.6f;
                break;
            case "MPH":
                kmhSpeed = speed * 1.6093f;
                break;
            case "KPH":
                kmhSpeed = speed;
                break;
        }
        return Math.round(kmhSpeed);
    }

    private int mapDirToDeg(String dir) {
        return Math.round(WindDirection.valueOf(dir).ordinal() * 22.5f);
    }

    private enum WindDirection { // see upstream code, we can't be more precise than getting the quadrant
        N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW
    }

}