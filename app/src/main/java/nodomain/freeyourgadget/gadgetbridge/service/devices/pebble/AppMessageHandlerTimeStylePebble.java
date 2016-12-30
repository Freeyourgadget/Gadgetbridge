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


    private static final int ICON_CLEAR_DAY = 0;
    private static final int ICON_CLEAR_NIGHT = 1;
    private static final int ICON_CLOUDY_DAY = 2;
    private static final int ICON_HEAVY_RAIN = 3;
    private static final int ICON_HEAVY_SNOW = 4;
    private static final int ICON_LIGHT_RAIN = 5;
    private static final int ICON_LIGHT_SNOW = 6;
    private static final int ICON_PARTLY_CLOUDY_NIGHT = 7;
    private static final int ICON_PARTLY_CLOUDY = 8;
    private static final int ICON_RAINING_AND_SNOWING = 9;
    private static final int ICON_THUNDERSTORM = 10;
    private static final int ICON_WEATHER_GENERIC = 11;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerTimeStylePebble.class);

    public AppMessageHandlerTimeStylePebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    /*
     * converted to JAVA from original JS
     */
    private int getIconForConditionCode(int conditionCode, boolean isNight) {
        int generalCondition = conditionCode / 100;
        int iconToLoad;
        // determine the correct icon
        switch (generalCondition) {
            case 2: //thunderstorm
                iconToLoad = ICON_THUNDERSTORM;
                break;
            case 3: //drizzle
                iconToLoad = ICON_LIGHT_RAIN;
                break;
            case 5: //rain
                if (conditionCode == 500) {
                    iconToLoad = ICON_LIGHT_RAIN;
                } else if (conditionCode < 505) {
                    iconToLoad = ICON_HEAVY_RAIN;
                } else if (conditionCode == 511) {
                    iconToLoad = ICON_RAINING_AND_SNOWING;
                } else {
                    iconToLoad = ICON_LIGHT_RAIN;
                }
                break;
            case 6: //snow
                if (conditionCode == 600 || conditionCode == 620) {
                    iconToLoad = ICON_LIGHT_SNOW;
                } else if (conditionCode > 610 && conditionCode < 620) {
                    iconToLoad = ICON_RAINING_AND_SNOWING;
                } else {
                    iconToLoad = ICON_HEAVY_SNOW;
                }
                break;
            case 7: // fog, dust, etc
                iconToLoad = ICON_CLOUDY_DAY;
                break;
            case 8: // clouds
                if (conditionCode == 800) {
                    iconToLoad = (!isNight) ? ICON_CLEAR_DAY : ICON_CLEAR_NIGHT;
                } else if (conditionCode < 803) {
                    iconToLoad = (!isNight) ? ICON_PARTLY_CLOUDY : ICON_PARTLY_CLOUDY_NIGHT;
                } else {
                    iconToLoad = ICON_CLOUDY_DAY;
                }
                break;
            default:
                iconToLoad = ICON_WEATHER_GENERIC;
                break;
        }

        return iconToLoad;
    }

    private byte[] encodeTimeStylePebbleWeather() {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        ParcelableWeather2 weather = Weather.getInstance().getWeather2();

        boolean isNight = false;   //TODO: use the night icons when night
        if (weather != null) {
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherUseNightIcon, (Object) (isNight ? 1 : 0)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherTemperature, (Object) (weather.currentTemp - 273)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherCondition, (Object) (getIconForConditionCode(weather.currentConditionCode, isNight))));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastCondition, (Object) (getIconForConditionCode(weather.forecastConditionCode, isNight))));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastHighTemp, (Object) (weather.todayHighTemp - 273)));
            pairs.add(new Pair<>(MESSAGE_KEY_WeatherForecastLowTemp, (Object) (weather.todayLowTemp - 273)));
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