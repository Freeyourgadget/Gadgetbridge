package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

class AppMessageHandlerSquare extends AppMessageHandler {
    // "CfgKeyCelsiusTemperature":10001,
    // CfgKeyConditions":10002,
    //"CfgKeyWeatherError":10003,
    // "CfgKeyWeatherMode":10004,
    // "CfgKeyUseCelsius":10005,"
    // CfgKeyWeatherLocation":10006,"
    // "CfgKeyTemperature":10000,
    //
    //
    private static final int KEY_TEMP = 10001; //celsius
    private static final int KEY_WEATHER = 10002;
    private static final int KEY_WEATHER_MODE = 10004;
    private static final int KEY_USE_CELSIUS = 10005; //celsius
    private static final int KEY_LOCATION = 10006;
    private static final int KEY_TEMP_F = 10000; //fahrenheit

    AppMessageHandlerSquare(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeSquareWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_WEATHER_MODE, (Object) 1));
        pairs.add(new Pair<>(KEY_WEATHER, (Object) weatherSpec.currentCondition));
        pairs.add(new Pair<>(KEY_USE_CELSIUS, (Object) 1));
        pairs.add(new Pair<>(KEY_TEMP, (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(KEY_LOCATION, (Object) (weatherSpec.location)));
        byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

        ByteBuffer buf = ByteBuffer.allocate(weatherMessage.length);

        buf.put(weatherMessage);

        return buf.array();
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        // Just ACK
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);
        return new GBDeviceEvent[]{sendBytesAck};
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
