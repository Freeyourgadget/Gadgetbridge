package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.graphics.Color;
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

public class AppMessageHandlerTimeStylePebble extends AppMessageHandler {
    public static final int KEY_SETTING_SIDEBAR_LEFT = 9;
    public static final int KEY_CONDITION_CODE = 4;
    public static final int KEY_FORECAST_CONDITION = 25;
    public static final int KEY_FORECAST_TEMP_HIGH = 26;
    public static final int KEY_FORECAST_TEMP_LOW = 27;
    public static final int KEY_SETTING_ALTCLOCK_NAME = 28;
    public static final int KEY_SETTING_ALTCLOCK_OFFSET = 29;
    public static final int KEY_SETTING_BT_VIBE = 11;
    public static final int KEY_SETTING_CLOCK_FONT_ID = 18;
    public static final int KEY_SETTING_COLOR_BG = 7;
    public static final int KEY_SETTING_COLOR_SIDEBAR = 8;
    public static final int KEY_SETTING_COLOR_TIME = 6;
    public static final int KEY_SETTING_DISABLE_WEATHER = 17;
    public static final int KEY_SETTING_HOURLY_VIBE = 19;
    public static final int KEY_SETTING_LANGUAGE_ID = 13;
    public static final int KEY_SETTING_ONLY_SHOW_BATTERY_WHEN_LOW = 20;
    public static final int KEY_SETTING_SHOW_BATTERY_PCT = 16;
    public static final int KEY_SETTING_SHOW_LEADING_ZERO = 15;
    public static final int KEY_SETTING_SIDEBAR_TEXT_COLOR = 12;
    public static final int KEY_SETTING_USE_LARGE_FONTS = 21;
    public static final int KEY_SETTING_USE_METRIC = 10;
    public static final int KEY_TEMPERATURE = 3;
    public static final int KEY_USE_NIGHT_ICON = 5;
    public static final int KEY_WIDGET_0_ID = 22;
    public static final int KEY_WIDGET_1_ID = 23;
    public static final int KEY_WIDGET_2_ID = 24;


    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerTimeStylePebble.class);

    public AppMessageHandlerTimeStylePebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeTimeStylePebbleConfig() {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        //settings that give good legibility on pebble time
        pairs.add(new Pair<>(KEY_SETTING_SIDEBAR_LEFT, (Object) 1));
        pairs.add(new Pair<>(KEY_SETTING_CLOCK_FONT_ID, (Object) 1));
        pairs.add(new Pair<>(KEY_SETTING_COLOR_BG, (Object) Color.parseColor("#ffffff")));
        pairs.add(new Pair<>(KEY_SETTING_COLOR_SIDEBAR, (Object) Color.parseColor("#00aaff")));
        pairs.add(new Pair<>(KEY_SETTING_COLOR_TIME, (Object) Color.parseColor("#000000")));
        pairs.add(new Pair<>(KEY_SETTING_SHOW_LEADING_ZERO, (Object) 1));
        pairs.add(new Pair<>(KEY_SETTING_LANGUAGE_ID, (Object) 2)); //2 = Deutsch
        pairs.add(new Pair<>(KEY_SETTING_USE_METRIC, (Object) 1));
        pairs.add(new Pair<>(KEY_SETTING_SHOW_BATTERY_PCT, (Object) 1));

        pairs.add(new Pair<>(KEY_WIDGET_0_ID, (Object) 7)); //7 = current weather
        pairs.add(new Pair<>(KEY_WIDGET_1_ID, (Object) 2)); //2 = battery
        pairs.add(new Pair<>(KEY_WIDGET_2_ID, (Object) 4)); //4 = Date

        byte[] ackMessage = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);
        byte[] testMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);

        byte[] weatherMessage=encodeTimeStylePebbleWeather();
        ByteBuffer buf = ByteBuffer.allocate(ackMessage.length + testMessage.length + weatherMessage.length);

        // encode ack and put in front of push message (hack for acknowledging the last message)
        buf.put(ackMessage);
        buf.put(testMessage);
        buf.put(weatherMessage);

        return buf.array();
    }

    private byte[] encodeTimeStylePebbleWeather() {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        ParcelableWeather2 weather = Weather.getInstance().getWeather2();

        if (weather != null) {
            //TODO: use the night icons when night
            pairs.add(new Pair<>(KEY_USE_NIGHT_ICON, (Object) 0));
            pairs.add(new Pair<>(KEY_TEMPERATURE, (Object) (weather.currentTemp - 273)));
            pairs.add(new Pair<>(KEY_CONDITION_CODE, (Object) Weather.mapToYahooCondition(weather.currentConditionCode)));
            pairs.add(new Pair<>(KEY_FORECAST_CONDITION, (Object) Weather.mapToYahooCondition(weather.forecastConditionCode)));
            pairs.add(new Pair<>(KEY_FORECAST_TEMP_HIGH, (Object) (weather.highTemp - 273)));
            pairs.add(new Pair<>(KEY_FORECAST_TEMP_LOW, (Object) (weather.lowTemp - 273)));
        }
        byte[] weatherMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);
        return weatherMessage;

    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        return null;
        /*
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTimeStylePebbleConfig();
        return new GBDeviceEvent[]{sendBytes};
        */
    }
}
