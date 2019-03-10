/*  Copyright (C) 2018-2019 Johann C. Rode

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerYWeather extends AppMessageHandler {
    private static int CLEAR_DAY = 0;
    private static int CLEAR_NIGHT = 1;
    private static int WINDY = 2;
    private static int COLD = 3;
    private static int HOT = 4;
    private static int PARTLY_CLOUDY_DAY = 5;
    private static int PARTLY_CLOUDY_NIGHT = 6;
    private static int FOG = 7;
    private static int RAIN = 8;
    private static int SNOW = 9;
    private static int SLEET = 10;
    private static int SNOW_SLEET = 11;
    private static int RAIN_SLEET = 12;
    private static int RAIN_SNOW = 13;
    private static int CLOUDY = 14;
    private static int STORM = 15;
    private static int NA = 16;
    private static int DRIZZLE = 17;

    private Integer KEY_WEATHER_ICON;
    private Integer KEY_WEATHER_TEMP;
    private Integer KEY_LOCATION_NAME;
    private Integer KEY_WEATHER_WIND_SPEED;
    private Integer KEY_WEATHER_WIND_DIRECTION;
    private Integer KEY_WEATHER_TODAY_MINTEMP;
    private Integer KEY_WEATHER_TODAY_MAXTEMP;
    private Integer KEY_WEATHER_D1_ICON;
    private Integer KEY_WEATHER_D1_MINTEMP;
    private Integer KEY_WEATHER_D1_MAXTEMP;
    private Integer KEY_WEATHER_D2_ICON;
    private Integer KEY_WEATHER_D2_MINTEMP;
    private Integer KEY_WEATHER_D2_MAXTEMP;
    private Integer KEY_WEATHER_D3_ICON;
    private Integer KEY_WEATHER_D3_MINTEMP;
    private Integer KEY_WEATHER_D3_MAXTEMP;

    AppMessageHandlerYWeather(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            KEY_LOCATION_NAME = appKeys.getInt("city");
            KEY_WEATHER_TEMP = appKeys.getInt("temperature");
            KEY_WEATHER_ICON = appKeys.getInt("icon");
            KEY_WEATHER_WIND_SPEED = appKeys.getInt("wind");
            KEY_WEATHER_WIND_DIRECTION = appKeys.getInt("wdirection");
            KEY_WEATHER_TODAY_MINTEMP = appKeys.getInt("low");
            KEY_WEATHER_TODAY_MAXTEMP = appKeys.getInt("high");
            KEY_WEATHER_D1_ICON = appKeys.getInt("code1");
            KEY_WEATHER_D1_MINTEMP = appKeys.getInt("low1");
            KEY_WEATHER_D1_MAXTEMP = appKeys.getInt("high1");
            KEY_WEATHER_D2_ICON = appKeys.getInt("code2");
            KEY_WEATHER_D2_MINTEMP = appKeys.getInt("low2");
            KEY_WEATHER_D2_MAXTEMP = appKeys.getInt("high2");
            KEY_WEATHER_D3_ICON = appKeys.getInt("code3");
            KEY_WEATHER_D3_MINTEMP = appKeys.getInt("low3");
            KEY_WEATHER_D3_MAXTEMP = appKeys.getInt("high3");

        } catch (JSONException e) {
            GB.toast("There was an error accessing the YWeather watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? CLEAR_NIGHT : CLEAR_DAY;
        } else if (conditionCode > 800 && conditionCode < 900) {
            return isNight ? PARTLY_CLOUDY_NIGHT : PARTLY_CLOUDY_DAY;
        } else if (conditionCode >= 300 && conditionCode < 313) {
            return DRIZZLE;
        } else if (conditionCode >= 313 && conditionCode < 400) {
            return DRIZZLE;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return CLOUDY;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762 ) {
            return FOG;
        } else if (conditionCode == 771) {
            return WINDY;
        } else if (conditionCode == 781) {
            return STORM;
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return STORM;
        } else if (conditionCode == 600 || conditionCode == 601 || conditionCode == 602 ) {
            return SNOW;
        } else if (conditionCode == 611 || conditionCode == 612) {
            return SLEET;
        } else if (conditionCode == 615 || conditionCode == 616 || conditionCode == 620 || conditionCode == 621 || conditionCode == 622) {
            return RAIN_SNOW;
        } else if (conditionCode == 906) {
            return SLEET;
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return STORM;
        } else if (conditionCode == 905 || (conditionCode >= 957 && conditionCode < 900)) {
            return STORM;
        } else if (conditionCode == 900) {
            return STORM;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return STORM;
        } else if (conditionCode == 903) {
            return COLD;
        } else if (conditionCode == 904) {
            return HOT;
        }

        return 0;
    }

    private String formatWindDirection(float wdirection) {
        if (Float.isNaN(wdirection)) {
            return "n/a";
        }
        if (wdirection >= 348.75 || wdirection <= 11.25) {
            return "N";
        } else if ( wdirection > 11.25 && wdirection <= 33.75 ) {
            return "NNE";
        } else if ( wdirection > 33.75 && wdirection <= 56.25 ) {
            return "NE";
        } else if ( wdirection > 56.25 && wdirection <= 78.75 ) {
            return "ENE";
        } else if ( wdirection > 78.75 && wdirection <= 101.25 ) {
            return "E";
        } else if ( wdirection > 101.25 && wdirection <= 123.75 ) {
            return "ESE";
        } else if ( wdirection > 123.75 && wdirection <= 146.25 ) {
            return "SE";
        } else if ( wdirection > 146.25 && wdirection <= 168.75 ) {
            return "SSE";
        } else if ( wdirection > 168.75 && wdirection <= 191.25 ) {
            return "S";
        } else if ( wdirection > 191.25 && wdirection <= 213.75 ) {
            return "SSW";
        } else if ( wdirection > 213.75 && wdirection <= 236.25 ) {
            return "SW";
        } else if ( wdirection > 236.25 && wdirection <= 258.75 ) {
            return "WSW";
        } else if ( wdirection > 258.75 && wdirection <= 281.25 ) {
            return "W";
        } else if ( wdirection > 281.25 && wdirection <= 303.75 ) {
            return "WNW";
        } else if ( wdirection > 303.75 && wdirection <= 326.25 ) {
            return "NW";
        } else if ( wdirection > 326.25 && wdirection <= 348.75 ) {
            return "NNW";
        } else {
            return "n/a";
        }
    }

    private byte[] encodeYWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }
        boolean isNight = false; // TODO
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_LOCATION_NAME, (Object) (weatherSpec.location)));
        pairs.add(new Pair<>(KEY_WEATHER_TEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°", weatherSpec.currentTemp - 273.15))));
        pairs.add(new Pair<>(KEY_WEATHER_TODAY_MINTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", weatherSpec.todayMinTemp - 273.15))));
        pairs.add(new Pair<>(KEY_WEATHER_TODAY_MAXTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", weatherSpec.todayMaxTemp - 273.15))));
        pairs.add(new Pair<>(KEY_WEATHER_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode, isNight))));
        pairs.add(new Pair<>(KEY_WEATHER_WIND_SPEED, (Object) (String.format(Locale.ENGLISH, "%.0f", weatherSpec.windSpeed))));
        pairs.add(new Pair<>(KEY_WEATHER_WIND_DIRECTION, (Object) (formatWindDirection(weatherSpec.windDirection))));
        if (weatherSpec.forecasts.size() > 0) {
            WeatherSpec.Forecast day1 = weatherSpec.forecasts.get(0);
            pairs.add(new Pair<>(KEY_WEATHER_D1_ICON, (Object) (getIconForConditionCode(day1.conditionCode, false))));
            pairs.add(new Pair<>(KEY_WEATHER_D1_MINTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day1.minTemp - 273.15))));
            pairs.add(new Pair<>(KEY_WEATHER_D1_MAXTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day1.maxTemp - 273.15))));
        }
        if (weatherSpec.forecasts.size() > 1) {
            WeatherSpec.Forecast day2 = weatherSpec.forecasts.get(1);
            pairs.add(new Pair<>(KEY_WEATHER_D2_ICON, (Object) (getIconForConditionCode(day2.conditionCode, false))));
            pairs.add(new Pair<>(KEY_WEATHER_D2_MINTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day2.minTemp - 273.15))));
            pairs.add(new Pair<>(KEY_WEATHER_D2_MAXTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day2.maxTemp - 273.15))));
        }
        if (weatherSpec.forecasts.size() > 2) {
            WeatherSpec.Forecast day3 = weatherSpec.forecasts.get(2);
            pairs.add(new Pair<>(KEY_WEATHER_D3_ICON, (Object) (getIconForConditionCode(day3.conditionCode, false))));
            pairs.add(new Pair<>(KEY_WEATHER_D3_MINTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day3.minTemp - 273.15))));
            pairs.add(new Pair<>(KEY_WEATHER_D3_MAXTEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°C", day3.maxTemp - 273.15))));
        }
        byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);

        ByteBuffer buf = ByteBuffer.allocate(weatherMessage.length);

        buf.put(weatherMessage);

        return buf.array();
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeYWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeYWeatherMessage(weatherSpec);
    }
}
