/*  Copyright (C) 2015-2018 Andreas Shimokawa, Daniele Gobbetti

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerObsidian extends AppMessageHandler {

    /*
      "appKeys": {
    "CONFIG_WEATHER_REFRESH": 35,
    "CONFIG_WEATHER_UNIT_LOCAL": 31,
    "MSG_KEY_WEATHER_TEMP": 100,

    "CONFIG_WEATHER_EXPIRATION": 36,
    "MSG_KEY_FETCH_WEATHER": 102,
    "MSG_KEY_WEATHER_ICON": 101,
    "MSG_KEY_WEATHER_FAILED": 104,
    "CONFIG_WEATHER_MODE_LOCAL": 30,
    "CONFIG_WEATHER_APIKEY_LOCAL": 33,
    "CONFIG_WEATHER_LOCAL": 28,
    "CONFIG_COLOR_WEATHER": 29,
    "CONFIG_WEATHER_LOCATION_LOCAL": 34,
    "CONFIG_WEATHER_SOURCE_LOCAL": 32
  }
     */


    private static final String ICON_01d = "a"; //night icons are just uppercase
    private static final String ICON_02d = "b";
    private static final String ICON_03d = "c";
    private static final String ICON_04d = "d";
    private static final String ICON_09d = "e";
    private static final String ICON_10d = "f";
    private static final String ICON_11d = "g";
    private static final String ICON_13d = "h";
    private static final String ICON_50d = "i";


    AppMessageHandlerObsidian(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
        messageKeys = new HashMap<>();
        try {
            JSONObject appKeys = getAppKeys();
            Iterator<String> appKeysIterator = appKeys.keys();
            while (appKeysIterator.hasNext()) {
                String current = appKeysIterator.next();
                switch (current) {
                    case "CONFIG_WEATHER_REFRESH":
                    case "CONFIG_WEATHER_UNIT_LOCAL":
                    case "MSG_KEY_WEATHER_TEMP":
                    case "MSG_KEY_WEATHER_ICON":
                        messageKeys.put(current, appKeys.getInt(current));
                        break;
                }
            }
        } catch (JSONException e) {
            GB.toast("There was an error accessing the timestyle watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private String getIconForConditionCode(int conditionCode, boolean isNight) {

        int generalCondition = conditionCode / 100;
        String iconToLoad;
        // determine the correct icon
        switch (generalCondition) {
            case 2: //thunderstorm
                iconToLoad = ICON_11d;
                break;
            case 3: //drizzle
                iconToLoad = ICON_09d;
                break;
            case 5: //rain
                if (conditionCode == 500) {
                    iconToLoad = ICON_09d;
                } else if (conditionCode < 505) {
                    iconToLoad = ICON_10d;
                } else if (conditionCode == 511) {
                    iconToLoad = ICON_10d;
                } else {
                    iconToLoad = ICON_09d;
                }
                break;
            case 6: //snow
                if (conditionCode == 600 || conditionCode == 620) {
                    iconToLoad = ICON_13d;
                } else if (conditionCode > 610 && conditionCode < 620) {
                    iconToLoad = ICON_13d;
                } else {
                    iconToLoad = ICON_13d;
                }
                break;
            case 7: // fog, dust, etc
                iconToLoad = ICON_03d;
                break;
            case 8: // clouds
                if (conditionCode == 800) {
                    iconToLoad = ICON_01d;
                } else if (conditionCode < 803) {
                    iconToLoad = ICON_02d;
                } else {
                    iconToLoad = ICON_04d;
                }
                break;
            default:
                iconToLoad = ICON_02d;
                break;
        }

        return (!isNight) ? iconToLoad : iconToLoad.toUpperCase();
    }

    private byte[] encodeObisdianWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        boolean isNight = false;   //TODO: use the night icons when night
        pairs.add(new Pair<>(messageKeys.get("CONFIG_WEATHER_REFRESH"), (Object) 60));
        pairs.add(new Pair<>(messageKeys.get("CONFIG_WEATHER_UNIT_LOCAL"), (Object) 1)); //celsius
        pairs.add(new Pair<>(messageKeys.get("MSG_KEY_WEATHER_ICON"), (Object) getIconForConditionCode(weatherSpec.currentConditionCode, isNight))); //celsius
        pairs.add(new Pair<>(messageKeys.get("MSG_KEY_WEATHER_TEMP"), (Object) (weatherSpec.currentTemp - 273)));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeObisdianWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeObisdianWeather(weatherSpec);
    }
}