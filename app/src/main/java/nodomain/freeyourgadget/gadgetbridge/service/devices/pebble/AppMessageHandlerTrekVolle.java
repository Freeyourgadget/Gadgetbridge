package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

class AppMessageHandlerTrekVolle extends AppMessageHandler {
    private static final int MESSAGE_KEY_WEATHER_TEMPERATURE = 10000;
    private static final int MESSAGE_KEY_WEATHER_CONDITIONS = 10001;
    private static final int MESSAGE_KEY_WEATHER_ICON = 10002;
    private static final int MESSAGE_KEY_WEATHER_TEMPERATURE_MIN = 10024;
    private static final int MESSAGE_KEY_WEATHER_TEMPERATURE_MAX = 10025;
    private static final int MESSAGE_KEY_WEATHER_LOCATION = 10030;

    AppMessageHandlerTrekVolle(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        /*
        case 1:  return RESOURCE_ID_IMAGE_WEATHER_CLEARNIGHT;
        case 2:  return RESOURCE_ID_IMAGE_WEATHER_CLEAR;
        case 3:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDYNIGHT;
        case 4:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDY;
        case 5:  return RESOURCE_ID_IMAGE_WEATHER_CLOUDS;
        case 6:  return RESOURCE_ID_IMAGE_WEATHER_THICKCLOUDS;
        case 7:  return RESOURCE_ID_IMAGE_WEATHER_RAIN;
        case 8:  return RESOURCE_ID_IMAGE_WEATHER_RAINYNIGHT;
        case 9:  return RESOURCE_ID_IMAGE_WEATHER_RAINY;
        case 10: return RESOURCE_ID_IMAGE_WEATHER_LIGHTNING;
        case 11: return RESOURCE_ID_IMAGE_WEATHER_SNOW;
        case 12: return RESOURCE_ID_IMAGE_WEATHER_MIST;
        */
        return 2;
    }

    private byte[] encodeTrekVolleWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        boolean isNight = false; // FIXME
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE, (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_CONDITIONS, (Object) (weatherSpec.currentCondition)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_ICON, (Object) (getIconForConditionCode(weatherSpec.currentConditionCode, isNight))));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MAX, (Object) (weatherSpec.todayMaxTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_TEMPERATURE_MIN, (Object) (weatherSpec.todayMinTemp - 273)));
        pairs.add(new Pair<>(MESSAGE_KEY_WEATHER_LOCATION, (Object) weatherSpec.location));


        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTrekVolleWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTrekVolleWeather(weatherSpec);
    }
}