/*  Copyright (C) 2017 Andreas Shimokawa

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
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTrekVolle extends AppMessageHandler {
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE;
    private Integer MESSAGE_KEY_WEATHER_CONDITIONS;
    private Integer MESSAGE_KEY_WEATHER_ICON;
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE_MIN;
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE_MAX;
    private Integer MESSAGE_KEY_WEATHER_LOCATION;

    AppMessageHandlerTrekVolle(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            MESSAGE_KEY_WEATHER_TEMPERATURE = appKeys.getInt("WEATHER_TEMPERATURE");
            MESSAGE_KEY_WEATHER_CONDITIONS = appKeys.getInt("WEATHER_CONDITIONS");
            MESSAGE_KEY_WEATHER_ICON = appKeys.getInt("WEATHER_ICON");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MIN = appKeys.getInt("WEATHER_TEMPERATURE_MIN");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MAX = appKeys.getInt("WEATHER_TEMPERATURE_MAX");
            MESSAGE_KEY_WEATHER_LOCATION = appKeys.getInt("WEATHER_LOCATION");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the TrekVolle watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        /*
        case 1:  return RESOURCE_ID_IMAGE_WEATHER_CLEARNIGHT;
        case 2:  return RESOURCE_ID_IMAGE_WEATHER_CLEAR;
        case 3:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDYNIGHT;
        case 4:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDY;
        case 5:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDS;
        case 6:  return RESOURCE_ID_IMAGE_WEATHER_THICKCLOUDS;
        case 7:  return RESOURCE_ID_IMAGE_WEATHER_RAIN;
        case 8:  return RESOURCE_ID_IMAGE_WEATHER_RAINYNIGHT;
        case 9:  return RESOURCE_ID_IMAGE_WEATHER_RAINY;
        case 10: return RESOURCE_ID_IMAGE_WEATHER_LIGHTNING;
        case 11: return RESOURCE_ID_IMAGE_WEATHER_SNOW;
        case 12: return RESOURCE_ID_IMAGE_WEATHER_MIST;
        */
        return 2;
    }

    private byte[] encodeTrekVolleWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        boolean isNight = false; // FIXME
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE, (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_CONDITIONS, (Object) (weatherSpec.currentCondition)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode, isNight))));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MAX, (Object) (weatherSpec.todayMaxTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MIN, (Object) (weatherSpec.todayMinTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_LOCATION, (Object) weatherSpec.location));


        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs, null);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTrekVolleWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTrekVolleWeather(weatherSpec);
    }
}