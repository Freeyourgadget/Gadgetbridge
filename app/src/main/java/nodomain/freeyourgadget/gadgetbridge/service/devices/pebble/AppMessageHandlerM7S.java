/*  Copyright (C) 2018 Johann C. Rode, Sergio Lopez

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

class AppMessageHandlerM7S extends AppMessageHandler {
    private static final int CLEAR = 73;
    private static final int CLOUDY = 34;
    private static final int FOG = 60;
    private static final int DRIZZLE = 45;
    private static final int LIGHT_RAIN = 36;
    private static final int RAIN = 36;
    private static final int THUNDERSTORM = 70;
    private static final int SNOW = 57;
    private static final int HAIL = 51;
    private static final int WIND = 66;
    private static final int EXTREME_WIND = 66;
    private static final int TORNADO = 88;
    private static final int HURRICANE = 88;
    private static final int EXTREME_COLD = 90;
    private static final int EXTREME_HEAT = 93;
    private static final int HAZE = 63;

    private Integer KEY_LOCATION_NAME;
    private Integer KEY_WEATHER_TEMP;
    private Integer KEY_WEATHER_STRING_1;
    private Integer KEY_WEATHER_STRING_2;
    private Integer KEY_WEATHER_ICON;
    private Integer KEY_WEATHER_DATA_TIME;

    AppMessageHandlerM7S(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            KEY_LOCATION_NAME = appKeys.getInt("KEY_LOCATION_NAME");
            KEY_WEATHER_TEMP = appKeys.getInt("KEY_WEATHER_TEMP");
            KEY_WEATHER_STRING_1 = appKeys.getInt("KEY_WEATHER_STRING_1");
            KEY_WEATHER_STRING_2 = appKeys.getInt("KEY_WEATHER_STRING_2");
            KEY_WEATHER_ICON = appKeys.getInt("KEY_WEATHER_ICON");
            KEY_WEATHER_DATA_TIME = appKeys.getInt("KEY_WEATHER_DATA_TIME");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the M7S watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode) {
        if (conditionCode == 800 || conditionCode == 951) {
            return CLEAR;
        } else if (conditionCode > 800 && conditionCode < 900) {
            return CLOUDY;
        } else if (conditionCode >= 300 && conditionCode < 313) {
            return DRIZZLE;
        } else if (conditionCode >= 313 && conditionCode < 400) {
            return LIGHT_RAIN;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return HAZE;
        } else if (conditionCode == 741) {
            return FOG;
        } else if (conditionCode == 751 || conditionCode == 761 || conditionCode == 762 ) {
            return HAZE;
        } else if (conditionCode == 771) {
            return WIND;
        } else if (conditionCode == 781) {
            return TORNADO;
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return THUNDERSTORM;
        } else if (conditionCode >= 600 && conditionCode < 700) {
            return SNOW;
        } else if (conditionCode == 906) {
            return HAIL;
        } else if (conditionCode >= 907 && conditionCode < 957) {
            return WIND;
        } else if (conditionCode == 905 || (conditionCode >= 957 && conditionCode < 900)) {
            return EXTREME_WIND;
        } else if (conditionCode == 900) {
            return TORNADO;
        } else if (conditionCode == 901 || conditionCode == 902 || conditionCode == 962) {
            return HURRICANE;
        } else if (conditionCode == 903) {
            return EXTREME_COLD;
        } else if (conditionCode == 904) {
            return EXTREME_HEAT;
        }

        return 0;
    }

    private byte[] encodeM7SWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        String wString1 = String.format(Locale.ENGLISH, "%.0f / %.0f__C \n%.0f %s", (weatherSpec.todayMaxTemp-273.15), (weatherSpec.todayMinTemp-273.15), weatherSpec.windSpeed, "km/h");
        String wString2 = String.format(Locale.ENGLISH, "%d %%", weatherSpec.currentHumidity);

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_LOCATION_NAME, (Object) (weatherSpec.location)));
        pairs.add(new Pair<>(KEY_WEATHER_TEMP, (Object) ((int) Math.round(weatherSpec.currentTemp - 273.15))));
        pairs.add(new Pair<>(KEY_WEATHER_DATA_TIME, (Object) (weatherSpec.timestamp)));
        pairs.add(new Pair<>(KEY_WEATHER_STRING_1, (Object) (wString1)));
        pairs.add(new Pair<>(KEY_WEATHER_STRING_2, (Object) (wString2)));
        pairs.add(new Pair<>(KEY_WEATHER_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode))));
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
        sendBytes.encodedBytes = encodeM7SWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeM7SWeatherMessage(weatherSpec);
    }
}
