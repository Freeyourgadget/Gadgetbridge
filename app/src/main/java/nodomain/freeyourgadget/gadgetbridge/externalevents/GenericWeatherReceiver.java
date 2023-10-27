/*  Copyright (C) 2020 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericWeatherReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GenericWeatherReceiver.class);

    public final static String ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    public final static String EXTRA_WEATHER_JSON = "WeatherJson";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_GENERIC_WEATHER.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null && bundle.containsKey(EXTRA_WEATHER_JSON)) {
                try {
                    JSONObject weatherJson = new JSONObject(bundle.getString(EXTRA_WEATHER_JSON));

                    WeatherSpec weatherSpec = new WeatherSpec();

                    weatherSpec.timestamp = safelyGet(weatherJson, Integer.class, "timestamp", (int) (System.currentTimeMillis() / 1000));
                    weatherSpec.location = safelyGet(weatherJson, String.class, "location", "");
                    weatherSpec.currentTemp = safelyGet(weatherJson, Integer.class, "currentTemp", 0);
                    weatherSpec.todayMinTemp = safelyGet(weatherJson, Integer.class, "todayMinTemp", 0);
                    weatherSpec.todayMaxTemp = safelyGet(weatherJson, Integer.class, "todayMaxTemp", 0);
                    weatherSpec.currentCondition = safelyGet(weatherJson, String.class, "currentCondition", "");
                    weatherSpec.currentConditionCode = safelyGet(weatherJson, Integer.class, "currentConditionCode", 0);
                    weatherSpec.currentHumidity = safelyGet(weatherJson, Integer.class, "currentHumidity", 0);
                    weatherSpec.windSpeed = safelyGet(weatherJson, Number.class, "windSpeed", 0d).floatValue();
                    weatherSpec.windDirection = safelyGet(weatherJson, Integer.class, "windDirection", 0);
                    weatherSpec.uvIndex = safelyGet(weatherJson, Number.class, "uvIndex", 0d).floatValue();
                    weatherSpec.precipProbability = safelyGet(weatherJson, Integer.class, "precipProbability", 0);
                    weatherSpec.dewPoint = safelyGet(weatherJson, Integer.class, "dewPoint", 0);
                    weatherSpec.pressure = safelyGet(weatherJson, Number.class, "pressure", 0).floatValue();
                    weatherSpec.cloudCover = safelyGet(weatherJson, Integer.class, "cloudCover", 0);
                    weatherSpec.visibility = safelyGet(weatherJson, Number.class, "visibility", 0).floatValue();
                    weatherSpec.sunRise = safelyGet(weatherJson, Integer.class, "sunRise", 0);
                    weatherSpec.sunSet = safelyGet(weatherJson, Integer.class, "sunSet", 0);
                    weatherSpec.moonRise = safelyGet(weatherJson, Integer.class, "moonRise", 0);
                    weatherSpec.moonSet = safelyGet(weatherJson, Integer.class, "moonSet", 0);
                    weatherSpec.moonPhase = safelyGet(weatherJson, Integer.class, "moonPhase", 0);
                    weatherSpec.latitude = safelyGet(weatherJson, Number.class, "latitude", 0).floatValue();
                    weatherSpec.longitude = safelyGet(weatherJson, Number.class, "longitude", 0).floatValue();
                    weatherSpec.feelsLikeTemp = safelyGet(weatherJson, Integer.class, "feelsLikeTemp", 0);
                    weatherSpec.isCurrentLocation = safelyGet(weatherJson, Integer.class, "isCurrentLocation", -1);

                    if (weatherJson.has("airQuality")) {
                        weatherSpec.airQuality = toAirQuality(weatherJson.getJSONObject("airQuality"));
                    }

                    if (weatherJson.has("forecasts")) {
                        JSONArray forecastArray = weatherJson.getJSONArray("forecasts");
                        weatherSpec.forecasts = new ArrayList<>();

                        for (int i = 0, l = forecastArray.length(); i < l; i++) {
                            JSONObject forecastJson = forecastArray.getJSONObject(i);

                            WeatherSpec.Daily forecast = new WeatherSpec.Daily();

                            forecast.conditionCode = safelyGet(forecastJson, Integer.class, "conditionCode", 0);
                            forecast.humidity = safelyGet(forecastJson, Integer.class, "humidity", 0);
                            forecast.maxTemp = safelyGet(forecastJson, Integer.class, "maxTemp", 0);
                            forecast.minTemp = safelyGet(forecastJson, Integer.class, "minTemp", 0);
                            forecast.windSpeed = safelyGet(forecastJson, Number.class, "windSpeed", 0).floatValue();
                            forecast.windDirection = safelyGet(forecastJson, Integer.class, "windDirection", 0);
                            forecast.uvIndex = safelyGet(forecastJson, Number.class, "uvIndex", 0d).floatValue();
                            forecast.precipProbability = safelyGet(forecastJson, Integer.class, "precipProbability", 0);
                            forecast.sunRise = safelyGet(forecastJson, Integer.class, "sunRise", 0);
                            forecast.sunSet = safelyGet(forecastJson, Integer.class, "sunSet", 0);
                            forecast.moonRise = safelyGet(forecastJson, Integer.class, "moonRise", 0);
                            forecast.moonSet = safelyGet(forecastJson, Integer.class, "moonSet", 0);
                            forecast.moonPhase = safelyGet(forecastJson, Integer.class, "moonPhase", 0);

                            if (forecastJson.has("airQuality")) {
                                forecast.airQuality = toAirQuality(forecastJson.getJSONObject("airQuality"));
                            }

                            weatherSpec.forecasts.add(forecast);
                        }
                    }

                    if (weatherJson.has("hourly")) {
                        JSONArray forecastArray = weatherJson.getJSONArray("hourly");
                        weatherSpec.hourly = new ArrayList<>();

                        for (int i = 0, l = forecastArray.length(); i < l; i++) {
                            JSONObject forecastJson = forecastArray.getJSONObject(i);

                            WeatherSpec.Hourly forecast = new WeatherSpec.Hourly();

                            forecast.timestamp = safelyGet(forecastJson, Integer.class, "timestamp", 0);
                            forecast.temp = safelyGet(forecastJson, Integer.class, "temp", 0);
                            forecast.conditionCode = safelyGet(forecastJson, Integer.class, "conditionCode", 0);
                            forecast.humidity = safelyGet(forecastJson, Integer.class, "humidity", 0);
                            forecast.windSpeed = safelyGet(forecastJson, Number.class, "windSpeed", 0).floatValue();
                            forecast.windDirection = safelyGet(forecastJson, Integer.class, "windDirection", 0);
                            forecast.uvIndex = safelyGet(forecastJson, Number.class, "uvIndex", 0d).floatValue();
                            forecast.precipProbability = safelyGet(forecastJson, Integer.class, "precipProbability", 0);

                            weatherSpec.hourly.add(forecast);
                        }
                    }

                    LOG.info("Got generic weather for {}", weatherSpec.location);

                    Weather.getInstance().setWeatherSpec(weatherSpec);
                    GBApplication.deviceService().onSendWeather(weatherSpec);
                } catch (Exception e) {
                    GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);
                }
            }
        }
    }

    private WeatherSpec.AirQuality toAirQuality(final JSONObject jsonObject) {
        final WeatherSpec.AirQuality airQuality = new WeatherSpec.AirQuality();
        airQuality.aqi = safelyGet(jsonObject, Integer.class, "aqi", -1);
        airQuality.co = safelyGet(jsonObject, Number.class, "co", -1).floatValue();
        airQuality.no2 = safelyGet(jsonObject, Number.class, "no2", -1).floatValue();
        airQuality.o3 = safelyGet(jsonObject, Number.class, "o3", -1).floatValue();
        airQuality.pm10 = safelyGet(jsonObject, Number.class, "pm10", -1).floatValue();
        airQuality.pm25 = safelyGet(jsonObject, Number.class, "pm25", -1).floatValue();
        airQuality.so2 = safelyGet(jsonObject, Number.class, "so2", -1).floatValue();
        airQuality.coAqi = safelyGet(jsonObject, Integer.class, "coAqi", -1);
        airQuality.no2Aqi = safelyGet(jsonObject, Integer.class, "no2Aqi", -1);
        airQuality.o3Aqi = safelyGet(jsonObject, Integer.class, "o3Aqi", -1);
        airQuality.pm10Aqi = safelyGet(jsonObject, Integer.class, "pm10Aqi", -1);
        airQuality.pm25Aqi = safelyGet(jsonObject, Integer.class, "pm25Aqi", -1);
        airQuality.so2Aqi = safelyGet(jsonObject, Integer.class, "so2Aqi", -1);

        return airQuality;
    }

    private <T> T safelyGet(JSONObject jsonObject, Class<T> tClass, String name, T defaultValue) {
        try {
            if (jsonObject.has(name)) {
                Object value = jsonObject.get(name);

                if (tClass.isInstance(value)) {
                    return (T) value;
                }
            }
        } catch (Exception e) {
            //
        }
        return defaultValue;
    }
}