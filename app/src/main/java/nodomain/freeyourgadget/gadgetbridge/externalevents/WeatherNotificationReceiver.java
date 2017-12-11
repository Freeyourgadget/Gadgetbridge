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
        ParcelableWeather2 parcelableWeather2 = null;
        try {
            parcelableWeather2 = intent.getParcelableExtra("ru.gelin.android.weather.notification.EXTRA_WEATHER");
        } catch (RuntimeException e) {
            LOG.error("cannot get ParcelableWeather2", e);
        }

        if (parcelableWeather2 != null) {
            Weather weather = Weather.getInstance();
            weather.setReconstructedOWMWeather(parcelableWeather2.reconstructedOWMWeather);
            weather.setReconstructedOWMForecast(parcelableWeather2.reconstructedOWMForecast);

            WeatherSpec weatherSpec = parcelableWeather2.weatherSpec;
            LOG.info("weather in " + weatherSpec.location + " is " + weatherSpec.currentCondition + " (" + (weatherSpec.currentTemp - 273) + "°C)");

            Weather.getInstance().setWeatherSpec(weatherSpec);
            GBApplication.deviceService().onSendWeather(weatherSpec);
        }
    }
}