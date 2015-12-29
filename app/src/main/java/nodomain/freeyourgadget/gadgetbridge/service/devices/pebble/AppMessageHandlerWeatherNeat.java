package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

public class AppMessageHandlerWeatherNeat extends AppMessageHandler {

    public static final int KEY_REQUEST = 0;
    public static final int KEY_CITY = 1;
    public static final int KEY_TEMPERATURE = 2;
    public static final int KEY_CONDITION = 3;
    public static final int KEY_LIGHT_TIME = 5;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerWeatherNeat.class);

    public AppMessageHandlerWeatherNeat(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeWeatherNeatMessage(String city, String temperature, String condition, int light_time) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(4);
        pairs.add(new Pair<>(KEY_CITY, (Object) city));
        pairs.add(new Pair<>(KEY_TEMPERATURE, (Object) temperature));
        pairs.add(new Pair<>(KEY_CONDITION, (Object) condition));
        pairs.add(new Pair<>(KEY_LIGHT_TIME, (Object) light_time)); // seconds for backlight on shake

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
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        String currentTemp = (sharedPrefs.getInt("weather_current_temp", 0) - 273) + "°C";
        String location = sharedPrefs.getString("weather_location", "unknown");
        String condition = sharedPrefs.getString("weather_current_condition", "unknown");
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeWeatherNeatMessage(location, currentTemp, condition, 3);
        return new GBDeviceEvent[]{sendBytes};
    }
}
