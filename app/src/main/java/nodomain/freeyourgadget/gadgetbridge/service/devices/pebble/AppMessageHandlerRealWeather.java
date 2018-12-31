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

class AppMessageHandlerRealWeather extends AppMessageHandler {
    private static int CLEAR_DAY = 0;
    private static int CLEAR_NIGHT = 1;
    private static int WINDY = 2;
    private static int COLD = 3;
    private static int PARTLY_CLOUDY_DAY = 4;
    private static int PARTLY_CLOUDY_NIGHT = 5;
    private static int HAZE = 6;
    private static int CLOUD = 7;
    private static int RAIN = 8;
    private static int SNOW = 9;
    private static int HAIL = 10;
    private static int CLOUDY = 11;
    private static int STORM = 12;
    private static int NA = 13;

    private Integer KEY_WEATHER_ICON;
    private Integer KEY_WEATHER_TEMP;

    AppMessageHandlerRealWeather(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            KEY_WEATHER_TEMP = appKeys.getInt("temperature");
            KEY_WEATHER_ICON = appKeys.getInt("icon");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the YWeather watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        if (conditionCode == 800 || conditionCode == 951) {
            return isNight ? CLEAR_NIGHT : CLEAR_DAY;
        } else if (conditionCode == 801 || conditionCode == 802) {
            return isNight ? PARTLY_CLOUDY_NIGHT : PARTLY_CLOUDY_DAY;
        } else if (conditionCode >= 300 && conditionCode < 313) {
            return RAIN;
        } else if (conditionCode >= 313 && conditionCode < 400) {
            return RAIN;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
        } else if (conditionCode >= 700 && conditionCode < 732) {
            return CLOUDY;
        } else if (conditionCode == 741 || conditionCode == 751 || conditionCode == 761 || conditionCode == 762 ) {
            return HAZE;
        } else if (conditionCode == 771) {
            return WINDY;
        } else if (conditionCode == 781) {
            return STORM;
        } else if (conditionCode >= 200 && conditionCode < 300) {
            return STORM;
        } else if (conditionCode == 600 || conditionCode == 601 || conditionCode == 602 ) {
            return SNOW;
        } else if (conditionCode == 611 || conditionCode == 612) {
            return HAIL;
        } else if (conditionCode == 615 || conditionCode == 616 || conditionCode == 620 || conditionCode == 621 || conditionCode == 622) {
            return SNOW;
        } else if (conditionCode == 906) {
            return SNOW;
        } else if (conditionCode == 803 || conditionCode == 804) {
            return CLOUD;
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
        }
        return 0;
    }

    private byte[] encodeRealWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }
        boolean isNight = false; // TODO
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_WEATHER_TEMP, (Object) (String.format(Locale.ENGLISH, "%.0f°", weatherSpec.currentTemp - 273.15))));
        pairs.add(new Pair<>(KEY_WEATHER_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode, isNight))));
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
        sendBytes.encodedBytes = encodeRealWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeRealWeatherMessage(weatherSpec);
    }
}
