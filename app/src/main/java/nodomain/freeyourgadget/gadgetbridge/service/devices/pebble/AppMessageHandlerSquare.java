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
        } catch (IOException | JSONException e) {
            GB.toast("There was an error accessing the watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
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
