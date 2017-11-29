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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import ru.gelin.android.weather.notification.ParcelableWeather2;


public class WeatherNotificationReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherNotificationReceiver.class);


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().contains("WEATHER_UPDATE_2")) {
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
            Weather.getInstance().setWeather2(weather);
            LOG.info("weather in " + weather.location + " is " + weather.currentCondition + " (" + (weather.currentTemp - 273) + "°C)");

            WeatherSpec weatherSpec = new WeatherSpec();
            weatherSpec.timestamp = (int) (weather.queryTime / 1000);
            weatherSpec.location = weather.location;
            weatherSpec.currentTemp = weather.currentTemp;
            weatherSpec.currentCondition = weather.currentCondition;
            weatherSpec.currentConditionCode = weather.currentConditionCode;
            weatherSpec.todayMaxTemp = weather.todayHighTemp;
            weatherSpec.todayMinTemp = weather.todayLowTemp;
            weatherSpec.forecasts = weather.forecasts;
            Weather.getInstance().setWeatherSpec(weatherSpec);
            GBApplication.deviceService().onSendWeather(weatherSpec);
        }
    }
}