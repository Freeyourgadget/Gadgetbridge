/*  Copyright (C) 2017-2018 Daniele Gobbetti

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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;


public class OmniJawsObserver extends ContentObserver {

    private static final Logger LOG = LoggerFactory.getLogger(OmniJawsObserver.class);

    private static final String SERVICE_PACKAGE = "org.omnirom.omnijaws";
    public static final Uri WEATHER_URI = Uri.parse("content://org.omnirom.omnijaws.provider/weather");
    private static final Uri SETTINGS_URI = Uri.parse("content://org.omnirom.omnijaws.provider/settings");

    private Context mContext;
    private boolean mInstalled;
    private boolean mEnabled = false;
    private boolean mMetric = true;


    private final String[] WEATHER_PROJECTION = new String[]{
            "city",
            "condition", //unused, see below
            "condition_code",
            "temperature",
            "humidity",
            "forecast_low",
            "forecast_high",
            "forecast_condition",
            "forecast_condition_code",
            "time_stamp",
            "forecast_date",
            "wind_speed",
            "wind_direction"
    };

    private final String[] SETTINGS_PROJECTION = new String[]{
            "enabled",
            "units"
    };

    public OmniJawsObserver(Handler handler) throws NameNotFoundException {
        super(handler);
        mContext = GBApplication.getContext();
        mInstalled = isOmniJawsServiceAvailable();
        LOG.info("OmniJaws installation status: " + mInstalled);
        checkSettings();
        LOG.info("OmniJaws is enabled: " + mEnabled);
        if (!mEnabled) {
            throw new NameNotFoundException();
        }

        updateWeather();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        LOG.info("Weather update received");
        checkSettings();
        if (!mEnabled) {
            LOG.info("Provider was disabled, ignoring.");
            return;
        }
        queryWeather();
    }

    private void queryWeather() {
        Cursor c = mContext.getContentResolver().query(WEATHER_URI, WEATHER_PROJECTION, null, null, null);
        if (c != null) {
            try {

                WeatherSpec weatherSpec = new WeatherSpec();
                weatherSpec.forecasts = new ArrayList<>();

                int count = c.getCount();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        c.moveToPosition(i);
                        if (i == 0) {

                            weatherSpec.location = c.getString(0);
                            weatherSpec.currentConditionCode = Weather.mapToOpenWeatherMapCondition(c.getInt(2));
                            weatherSpec.currentCondition = Weather.getConditionString(weatherSpec.currentConditionCode);
                            //alternatively the following would also be possible
                            //weatherSpec.currentCondition = c.getString(1);

                            weatherSpec.currentTemp = toKelvin(c.getFloat(3));
                            weatherSpec.currentHumidity = (int) c.getFloat(4);

                            weatherSpec.windSpeed = toKmh(c.getFloat(11));
                            weatherSpec.windDirection = c.getInt(12);
                            weatherSpec.timestamp = (int) (Long.valueOf(c.getString(9)) / 1000);
                        } else if (i == 1) {
                            weatherSpec.todayMinTemp = toKelvin(c.getFloat(5));
                            weatherSpec.todayMaxTemp = toKelvin(c.getFloat(6));
                        } else {

                            WeatherSpec.Forecast gbForecast = new WeatherSpec.Forecast();
                            gbForecast.minTemp = toKelvin(c.getFloat(5));
                            gbForecast.maxTemp = toKelvin(c.getFloat(6));
                            gbForecast.conditionCode = Weather.mapToOpenWeatherMapCondition(c.getInt(8));
                            weatherSpec.forecasts.add(gbForecast);
                        }
                    }
                }

                Weather.getInstance().setWeatherSpec(weatherSpec);
                GBApplication.deviceService().onSendWeather(weatherSpec);

            } finally {
                c.close();
            }
        }

    }

    private void updateWeather() {
        Intent updateIntent = new Intent(Intent.ACTION_MAIN)
                .setClassName(SERVICE_PACKAGE, SERVICE_PACKAGE + ".WeatherService");
        updateIntent.setAction(SERVICE_PACKAGE + ".ACTION_UPDATE");
        mContext.startService(updateIntent);
    }

    private boolean isOmniJawsServiceAvailable() throws NameNotFoundException {
        final PackageManager pm = mContext.getPackageManager();
        pm.getPackageInfo("org.omnirom.omnijaws", 0);
        return true;
    }

    private void checkSettings() {
        if (!mInstalled) {
            return;
        }
        final Cursor c = mContext.getContentResolver().query(SETTINGS_URI, SETTINGS_PROJECTION,
                null, null, null);
        if (c != null) {
            int count = c.getCount();
            if (count == 1) {
                c.moveToPosition(0);
                mEnabled = c.getInt(0) == 1 ? true : false;
                mMetric = c.getInt(1) == 0 ? true : false;
            }
        }
    }

    private int toKelvin(double temperature) {
        if (mMetric) {
            return (int) (temperature + 273.15);
        }
        return (int) ((temperature - 32) * 0.5555555555555556D + 273.15);
    }

    private float toKmh(float speed) {
        if (mMetric) {
            return speed;
        }
        return (speed * 1.61f);
    }
}