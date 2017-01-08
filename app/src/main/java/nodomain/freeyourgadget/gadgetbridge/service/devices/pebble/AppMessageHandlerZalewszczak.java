package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

class AppMessageHandlerZalewszczak extends AppMessageHandler {
    private static final int KEY_ICON = 0;
    private static final int KEY_TEMP = 1; //celsius

    AppMessageHandlerZalewszczak(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    /*
 * converted to JAVA from original JS
 */
    private int getIconForConditionCode(int conditionCode) {
        if (conditionCode < 300) {
            return 7;
        } else if (conditionCode < 400) {
            return 6;
        } else if (conditionCode == 511) {
            return 8;
        } else if (conditionCode < 600) {
            return 6;
        } else if (conditionCode < 700) {
            return 8;
        } else if (conditionCode < 800) {
            return 10;
        } else if (conditionCode == 800) {
            return 1;
        } else if (conditionCode == 801) {
            return 2;
        } else if (conditionCode < 900) {
            return 5;
        } else {
            return 0;
        }
    }


    private byte[] encodeWeatherMessage(WeatherSpec weatherSpec) {
        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>(2);
        pairs.add(new Pair<>(KEY_TEMP, (Object) (Math.round(weatherSpec.currentTemp - 273) + "C")));
        pairs.add(new Pair<>(KEY_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode))));
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
        sendBytes.encodedBytes = encodeWeatherMessage(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeWeatherMessage(weatherSpec);
    }
}
