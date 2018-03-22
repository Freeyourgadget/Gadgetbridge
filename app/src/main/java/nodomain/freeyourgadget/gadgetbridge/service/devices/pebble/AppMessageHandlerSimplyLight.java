/*  Copyright (C) 2016-2018 Andreas Shimokawa, Sergio Lopez

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
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerSimplyLight extends AppMessageHandler {
    private static final int CLEAR = 0;
    private static final int CLOUDY = 1;
    private static final int FOG = 2;
    private static final int LIGHT_RAIN = 3;
    private static final int RAIN = 4;
    private static final int THUNDERSTORM = 5;
    private static final int SNOW = 6;
    private static final int HAIL = 7;
    private static final int WIND = 8;
    private static final int EXTREME_WIND = 9;
    private static final int TORNADO = 10;
    private static final int HURRICANE = 11;
    private static final int EXTREME_COLD = 12;
    private static final int EXTREME_HEAT = 13;
    private static final int SNOW_THUNDERSTORM = 14;

    private Integer KEY_TEMPERATURE;
    private Integer KEY_CONDITION;
    private Integer KEY_ERR;

    AppMessageHandlerSimplyLight(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            KEY_TEMPERATURE = appKeys.getInt("temperature");
            KEY_CONDITION = appKeys.getInt("condition");
            KEY_ERR = appKeys.getInt("err");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the Simply Light watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }


private int getConditionForConditionCode(int conditionCode) {
        if (conditionCode == 800 || conditionCode == 951) {
            return CLEAR;
        } else if (conditionCode > 800 && conditionCode < 900) {
            return CLOUDY;
        } else if (conditionCode >= 700 && conditionCode < 800) {
            return FOG;
        } else if (conditionCode >= 300 && conditionCode < 400) {
            return LIGHT_RAIN;
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return RAIN;
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

    private byte[] encodeSimplyLightWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_TEMPERATURE, (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(KEY_CONDITION, (Object) (getConditionForConditionCode(weatherSpec.currentConditionCode))));
        pairs.add(new Pair<>(KEY_ERR, (Object) 0));
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
        sendBytes.encodedBytes = encodeSimplyLightWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeSimplyLightWeatherMessage(weatherSpec);
    }
}
