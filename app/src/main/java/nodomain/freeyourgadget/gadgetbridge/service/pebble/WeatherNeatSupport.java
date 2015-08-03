package nodomain.freeyourgadget.gadgetbridge.service.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

public class WeatherNeatSupport {

    public static final int KEY_REQUEST = 0;
    public static final int KEY_CITY = 1;
    public static final int KEY_TEMPERATUR = 2;
    public static final int KEY_CONDITION = 3;
    public static final int KEY_LIGHT_TIME = 5;

    public static final UUID uuid = UUID.fromString("3684003b-a685-45f9-a713-abc6364ba051");
    private final PebbleProtocol mPebbleProtocol;

    private static final Logger LOG = LoggerFactory.getLogger(WeatherNeatSupport.class);

    public WeatherNeatSupport(PebbleProtocol pebbleProtocol) {
        mPebbleProtocol = pebbleProtocol;
    }

    private byte[] encodeWeatherNeatMessage(String city, String temperature, String condition, int light_time) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(4);
        pairs.add(new Pair<>(1, (Object) city));
        pairs.add(new Pair<>(2, (Object) temperature));
        pairs.add(new Pair<>(3, (Object) condition));
        pairs.add(new Pair<>(5, (Object) light_time)); // seconds for backlight on shake

        byte[] ackMessage = mPebbleProtocol.encodeApplicationMessageAck(uuid, mPebbleProtocol.last_id);
        byte[] testMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs);

        ByteBuffer buf = ByteBuffer.allocate(ackMessage.length + testMessage.length);

        // encode ack and put in front of push message (hack for acknowledging the last message)
        buf.put(ackMessage);
        buf.put(testMessage);

        return buf.array();
    }

    public GBDeviceEvent handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeWeatherNeatMessage("Berlin", "22 C", "cloudy", 0);
        return sendBytes;
    }
}
