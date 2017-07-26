/*  Copyright (C) 2015-2017 Andreas Shimokawa, Daniele Gobbetti

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

public class ParcelableWeather2 implements Parcelable {
    private static final Logger LOG = LoggerFactory.getLogger(ParcelableWeather2.class);

    // getters and setters suck ;)

    public long time = 0;
    public long queryTime = 0;
    public int version = 0;
    public String location = "";
    public int currentTemp = 0;
    public String currentCondition = "";

    private String[] currentConditionType = null;
    public int currentConditionCode = 3200;
    private String[] forecastConditionType = null;
    public int forecastConditionCode = 3200;
    public int todayLowTemp = 0;
    public int todayHighTemp = 0;
    public int forecastLowTemp = 0;
    public int forecastHighTemp = 0;

    public JSONObject reconstructedWeather = null;
    public JSONObject reconstructedForecast = null;

    private ParcelableWeather2(Parcel in) {
        int version = in.readInt();
        if (version != 2) {
            return;
        }
        Bundle bundle = in.readBundle();

        location = bundle.getString("weather_location");
        time = bundle.getLong("weather_time");
        queryTime = bundle.getLong("weather_query_time");
        bundle.getString("weather_forecast_url");
        int conditions = bundle.getInt("weather_conditions");
        if (conditions > 0) {
            Bundle conditionBundle = in.readBundle();
            reconstructedWeather = new JSONObject();
            JSONArray weather = new JSONArray();
            JSONObject condition = new JSONObject();
            JSONObject main = new JSONObject();

            currentCondition = conditionBundle.getString("weather_condition_text");
            conditionBundle.getStringArray("weather_condition_types");
            currentTemp = conditionBundle.getInt("weather_current_temp");

            currentConditionType = conditionBundle.getStringArray("weather_condition_types");
            currentConditionCode = weatherConditionTypesToOpenWeatherMapIds(currentConditionType[0]);
            todayLowTemp = conditionBundle.getInt("weather_low_temp");
            todayHighTemp = conditionBundle.getInt("weather_high_temp");
            try {
                condition.put("id", currentConditionCode);
                condition.put("main", currentCondition);
                condition.put("icon", Weather.mapToOpenWeatherMapIcon(currentConditionCode));
                weather.put(condition);

                main.put("temp", currentTemp);
                main.put("humidity", conditionBundle.getInt("weather_humidity_value"));
                main.put("temp_min", todayLowTemp);
                main.put("temp_max", todayHighTemp);
                main.put("name", location);

                reconstructedWeather.put("weather", weather);
                reconstructedWeather.put("main", main);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            LOG.debug("Weather JSON for WEBVIEW: " + reconstructedWeather.toString());
            //fetch immediate next forecast
            if (--conditions > 0) {
                int timeOffset = 86400000; //manually determined
                reconstructedForecast = new JSONObject();
                JSONArray list = new JSONArray();
                JSONObject city = new JSONObject();
                JSONObject item = new JSONObject();
                condition = new JSONObject();
                main = new JSONObject();
                weather = new JSONArray();
                Bundle forecastBundle = in.readBundle();
                forecastConditionType = forecastBundle.getStringArray("weather_condition_types");
                forecastConditionCode = weatherConditionTypesToOpenWeatherMapIds(forecastConditionType[0]);
                forecastLowTemp = forecastBundle.getInt("weather_low_temp");
                forecastHighTemp = forecastBundle.getInt("weather_high_temp");
                try {
                    condition.put("id", forecastConditionCode);
                    condition.put("main", forecastBundle.getString("weather_condition_text"));
                    condition.put("icon", Weather.mapToOpenWeatherMapIcon(forecastConditionCode));
                    weather.put(condition);

                    main.put("temp", forecastBundle.getInt("weather_current_temp"));
                    main.put("humidity", forecastBundle.getInt("weather_humidity_value"));
                    main.put("temp_min", forecastLowTemp);
                    main.put("temp_max", forecastHighTemp);

                    //forecast
                    //"city":{"id":3181913,"name":"Bolzano","coord":{"lat":46.4927,"lon":11.3336},"country":"IT"}
                    city.put("name", location);
                    city.put("country", "World");
                    reconstructedForecast.put("city", city);

                    item.put("dt", (time / 1000) + timeOffset);
                    item.put("main", main);
                    item.put("weather", weather);
                    list.put(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // get the rest
                while (--conditions > 0) {
                    conditionBundle = in.readBundle();
                    conditionBundle.getString("weather_condition_text");
                    weatherConditionTypesToOpenWeatherMapIds(conditionBundle.getStringArray("weather_condition_types")[0]);
                    conditionBundle.getInt("weather_current_temp");
                    item = new JSONObject();
                    condition = new JSONObject();
                    main = new JSONObject();
                    weather = new JSONArray();
                    timeOffset += 86400000;
                    try {
                        condition.put("id", weatherConditionTypesToOpenWeatherMapIds(conditionBundle.getStringArray("weather_condition_types")[0]));
                        condition.put("main", conditionBundle.getString("weather_condition_text"));
                        condition.put("icon", Weather.mapToOpenWeatherMapIcon(weatherConditionTypesToOpenWeatherMapIds(conditionBundle.getStringArray("weather_condition_types")[0])));
                        weather.put(condition);

                        main.put("temp", conditionBundle.getInt("weather_current_temp"));
                        main.put("humidity", conditionBundle.getInt("weather_humidity_value"));
                        main.put("temp_min", conditionBundle.getInt("weather_low_temp"));
                        main.put("temp_max", conditionBundle.getInt("weather_high_temp"));

                        item.put("dt", (time / 1000) + timeOffset);
                        item.put("main", main);
                        item.put("weather", weather);
                        list.put(item);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    reconstructedForecast.put("cnt", list.length());
                    reconstructedForecast.put("list", list);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LOG.debug("Forecast JSON for WEBVIEW: " + reconstructedForecast.toString());
            }
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
}
