/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti

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

class AppMessageHandlerSquare extends AppMessageHandler {
    private int CfgKeyCelsiusTemperature;
    private int CfgKeyConditions;
    private int CfgKeyWeatherMode;
    private int CfgKeyUseCelsius;
    private int CfgKeyWeatherLocation;

    AppMessageHandlerSquare(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            CfgKeyCelsiusTemperature = appKeys.getInt("CfgKeyCelsiusTemperature");
            CfgKeyConditions = appKeys.getInt("CfgKeyConditions");
            CfgKeyWeatherMode = appKeys.getInt("CfgKeyWeatherMode");
            CfgKeyUseCelsius = appKeys.getInt("CfgKeyUseCelsius");
            CfgKeyWeatherLocation = appKeys.getInt("CfgKeyWeatherLocation");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the Square watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
    }

    private byte[] encodeSquareWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(CfgKeyWeatherMode, (Object) 1));
        pairs.add(new Pair<>(CfgKeyConditions, (Object) weatherSpec.currentCondition));
        pairs.add(new Pair<>(CfgKeyUseCelsius, (Object) 1));
        pairs.add(new Pair<>(CfgKeyCelsiusTemperature, (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(CfgKeyWeatherLocation, (Object) (weatherSpec.location)));
        byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

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
        sendBytes.encodedBytes = encodeSquareWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeSquareWeatherMessage(weatherSpec);
    }
}
