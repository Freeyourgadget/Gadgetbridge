package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

public class AppMessageHandlerWeatherNeat extends AppMessageHandler {

    public static final int KEY_REQUEST = 0;
    public static final int KEY_CITY = 1;
    public static final int KEY_TEMPERATUR = 2;
    public static final int KEY_CONDITION = 3;
    public static final int KEY_LIGHT_TIME = 5;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerWeatherNeat.class);

    public AppMessageHandlerWeatherNeat(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeWeatherNeatMessage(String city, String temperature, String condition, int light_time) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(4);
        pairs.add(new Pair<>(1, (Object) city));
        pairs.add(new Pair<>(2, (Object) temperature));
        pairs.add(new Pair<>(3, (Object) condition));
        pairs.add(new Pair<>(5, (Object) light_time)); // seconds for backlight on shake

        byte[] ackMessage = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);
        byte[] testMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

        ByteBuffer buf = ByteBuffer.allocate(ackMessage.length + testMessage.length);

        // encode ack and put in front of push message (hack for acknowledging the last message)
        buf.put(ackMessage);
        buf.put(testMessage);

        return buf.array();
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeWeatherNeatMessage("Berlin", "22 C", "cloudy", 0);
        return new GBDeviceEvent[]{sendBytes};
    }
}
