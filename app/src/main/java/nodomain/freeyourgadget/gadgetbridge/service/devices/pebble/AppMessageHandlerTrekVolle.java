package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTrekVolle extends AppMessageHandler {
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE;
    private Integer MESSAGE_KEY_WEATHER_CONDITIONS;
    private Integer MESSAGE_KEY_WEATHER_ICON;
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE_MIN;
    private Integer MESSAGE_KEY_WEATHER_TEMPERATURE_MAX;
    private Integer MESSAGE_KEY_WEATHER_LOCATION;

    AppMessageHandlerTrekVolle(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            MESSAGE_KEY_WEATHER_TEMPERATURE = appKeys.getInt("WEATHER_TEMPERATURE");
            MESSAGE_KEY_WEATHER_CONDITIONS = appKeys.getInt("WEATHER_CONDITIONS");
            MESSAGE_KEY_WEATHER_ICON = appKeys.getInt("WEATHER_ICON");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MIN = appKeys.getInt("WEATHER_TEMPERATURE_MIN");
            MESSAGE_KEY_WEATHER_TEMPERATURE_MAX = appKeys.getInt("WEATHER_TEMPERATURE_MAX");
            MESSAGE_KEY_WEATHER_LOCATION = appKeys.getInt("WEATHER_LOCATION");
        } catch (IOException | JSONException e) {
            GB.toast("There was an error accessing the watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        }
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