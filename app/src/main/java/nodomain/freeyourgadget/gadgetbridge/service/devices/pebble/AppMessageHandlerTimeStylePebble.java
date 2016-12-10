package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import ru.gelin.android.weather.notification.ParcelableWeather2;

public class AppMessageHandlerTimeStylePebble extends AppMessageHandler {
    private static final int MESSAGE_KEY_WeatherCondition = 10000;
    private static final int MESSAGE_KEY_WeatherForecastCondition = 10002;
    private static final int MESSAGE_KEY_WeatherForecastHighTemp = 10003;
    private static final int MESSAGE_KEY_WeatherForecastLowTemp = 10004;
    private static final int MESSAGE_KEY_WeatherTemperature = 10001;
    private static final int MESSAGE_KEY_WeatherUseNightIcon = 10025;


    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerTimeStylePebble.class);

    public AppMessageHandlerTimeStylePebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeTimeStylePebbleWeather() {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        ParcelableWeather2 weather = Weather.getInstance().getWeather2();

        if (weather != null) {
            //TODO: use the night icons when night
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherUseNightIcon, (Object) 1));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherTemperature, (Object) (weather.currentTemp - 273)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherCondition, (Object) (weather.currentConditionCode)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastCondition, (Object) (weather.forecastConditionCode)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastHighTemp, (Object) (weather.highTemp - 273)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastLowTemp, (Object) (weather.lowTemp - 273)));
        }
        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        return pushMessage();
    }

    @Override
    public GBDeviceEvent[] pushMessage() {
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTimeStylePebbleWeather();
        return new GBDeviceEvent[]{sendBytesAck, sendBytes};
    }
}