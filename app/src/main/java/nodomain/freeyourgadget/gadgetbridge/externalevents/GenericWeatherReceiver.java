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

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GenericWeatherReceiver extends BroadcastReceiver {
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
                    weatherSpec.windSpeed = safelyGet(weatherJson, Float.class, "windSpeed", 0f);
                    weatherSpec.windDirection = safelyGet(weatherJson, Integer.class, "windDirection", 0);

                    if (weatherJson.has("forecasts")) {
                        JSONArray forecastArray = weatherJson.getJSONArray("forecasts");
                        weatherSpec.forecasts = new ArrayList<>();

                        for (int i = 0, l = forecastArray.length(); i < l; i++) {
                            JSONObject forecastJson = forecastArray.getJSONObject(i);

                            WeatherSpec.Forecast forecast = new WeatherSpec.Forecast();
                            
                            forecast.conditionCode = safelyGet(forecastJson, Integer.class, "conditionCode", 0);
                            forecast.humidity = safelyGet(forecastJson, Integer.class, "humidity", 0);
                            forecast.maxTemp = safelyGet(forecastJson, Integer.class, "maxTemp", 0);
                            forecast.minTemp = safelyGet(forecastJson, Integer.class, "minTemp", 0);

                            weatherSpec.forecasts.add(forecast);
                        }
                    }

                    Weather.getInstance().setWeatherSpec(weatherSpec);
                    GBApplication.deviceService().onSendWeather(weatherSpec);
                } catch (Exception e) {
                    GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);
                }
            }
        }
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