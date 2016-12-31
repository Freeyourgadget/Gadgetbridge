package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import ru.gelin.android.weather.notification.ParcelableWeather2;

class AppMessageHandlerMarioTime extends AppMessageHandler {

    private static final int KEY_WEATHER_ICON_ID = 10;
    private static final int KEY_WEATHER_TEMPERATURE = 11;
    private static final int KEY_WEATHER_REQUEST = 12;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerMarioTime.class);

    AppMessageHandlerMarioTime(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeWeatherMessage(int temperature, int condition) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_WEATHER_ICON_ID, (Object) (byte) condition));
        pairs.add(new Pair<>(KEY_WEATHER_TEMPERATURE, (Object) (byte) temperature));
        byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

        ByteBuffer buf = ByteBuffer.allocate(weatherMessage.length);

        buf.put(weatherMessage);

        return buf.array();
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        boolean weatherRequested = false;
        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_WEATHER_REQUEST:
                    LOG.info("got weather request");
                    weatherRequested = true;
                    break;
                default:
                    LOG.info("unknown key " + pair.first);
            }
        }
        if (!weatherRequested) {
            return new GBDeviceEvent[]{null};
        }
        return onAppStart();
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        ParcelableWeather2 weather = Weather.getInstance().getWeather2();

        if (weather == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeWeatherMessage(weather.currentTemp - 273, 1); // FIXME: condition code

        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        return new GBDeviceEvent[]{sendBytesAck, sendBytes};

    }
}
