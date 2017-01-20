package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class AppMessageHandlerTimeStylePebble extends AppMessageHandler {

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

    AppMessageHandlerTimeStylePebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
        messageKeys = new HashMap<>();
        try {
            JSONObject appKeys = getAppKeys();
            Iterator<String> appKeysIterator = appKeys.keys();
            while (appKeysIterator.hasNext()) {
                String current = appKeysIterator.next();
                switch (current) {
                    case "WeatherCondition":
                    case "WeatherForecastCondition":
                    case "WeatherForecastHighTemp":
                    case "WeatherForecastLowTemp":
                    case "WeatherTemperature":
                    case "SettingUseMetric":
                    case "WeatherUseNightIcon":
                        messageKeys.put(current, appKeys.getInt(current));
                        break;
                }
            }
        } catch (IOException | JSONException e) {
            GB.toast("There was an error accessing the watchface configuration.", Toast.LENGTH_LONG, GB.ERROR);
        }
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

    private byte[] encodeTimeStylePebbleWeather(WeatherSpec weatherSpec) {

        if (weatherSpec == null) {
            return null;
        }

        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        boolean isNight = false;   //TODO: use the night icons when night
        pairs.add(new Pair<>(messageKeys.get("SettingUseMetric"), (Object) 1)); //celsius
        pairs.add(new Pair<>(messageKeys.get("WeatherUseNightIcon"), (Object) (isNight ? 1 : 0)));
        pairs.add(new Pair<>(messageKeys.get("WeatherTemperature"), (Object) (weatherSpec.currentTemp - 273)));
        pairs.add(new Pair<>(messageKeys.get("WeatherCondition"), (Object) (getIconForConditionCode(weatherSpec.currentConditionCode, isNight))));
        pairs.add(new Pair<>(messageKeys.get("WeatherForecastCondition"), (Object) (getIconForConditionCode(weatherSpec.tomorrowConditionCode, isNight))));
        pairs.add(new Pair<>(messageKeys.get("WeatherForecastHighTemp"), (Object) (weatherSpec.todayMaxTemp - 273)));
        pairs.add(new Pair<>(messageKeys.get("WeatherForecastLowTemp"), (Object) (weatherSpec.todayMinTemp - 273)));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);
    }

    @Override
    public GBDeviceEvent[] onAppStart() {
        WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();
        if (weatherSpec == null) {
            return new GBDeviceEvent[]{null};
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeTimeStylePebbleWeather(weatherSpec);
        return new GBDeviceEvent[]{sendBytes};
    }

    @Override
    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return encodeTimeStylePebbleWeather(weatherSpec);
    }
}