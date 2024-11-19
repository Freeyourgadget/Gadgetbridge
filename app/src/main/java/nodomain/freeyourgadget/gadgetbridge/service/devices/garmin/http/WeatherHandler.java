package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SunriseTransitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;

public class WeatherHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherHandler.class);

    private static final Gson GSON = new GsonBuilder()
            //.serializeNulls()
            .create();

    // These get requested on connection at most every 5 minutes
    public static GarminHttpResponse handleWeatherRequest(final GarminHttpRequest request) {
        final String path = request.getPath();
        final Map<String, String> query = request.getQuery();

        final WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) {
            LOG.warn("No weather in weather instance");
            return null;
        }

        final Object weatherData;
        switch (path) {
            case "/weather/v2/forecast/day": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final int duration = getQueryNum(query, "duration", 5);
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String provider = getQueryString(query, "provider", "dci");
                final String speedUnit = getQueryString(query, "speedUnit", "KILOMETERS_PER_HOUR");
                final List<WeatherForecastDay> ret = new ArrayList<>(duration);
                final GregorianCalendar date = new GregorianCalendar();
                date.setTime(new Date(weatherSpec.timestamp * 1000L));
                ret.add(new WeatherForecastDay(date, weatherSpec.todayAsDaily(), tempUnit, speedUnit));
                for (int i = 0; i < Math.min(duration, weatherSpec.forecasts.size()) - 1; i++) {
                    date.add(Calendar.DAY_OF_MONTH, 1);
                    ret.add(new WeatherForecastDay(date, weatherSpec.forecasts.get(i), tempUnit, speedUnit));
                }
                weatherData = ret;
                break;
            }
            case "/weather/v2/forecast/hour": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final int duration = getQueryNum(query, "duration", 13);
                final String speedUnit = getQueryString(query, "speedUnit", "METERS_PER_SECOND");
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String provider = getQueryString(query, "provider", "dci");
                final String timesOfInterest = getQueryString(query, "timesOfInterest", "");
                final List<WeatherForecastHour> ret = new ArrayList<>(duration);
                for (int i = 0; i < Math.min(duration, weatherSpec.hourly.size()); i++) {
                    ret.add(new WeatherForecastHour(weatherSpec.hourly.get(i), tempUnit, speedUnit));
                }
                weatherData = ret;
                break;
            }
            case "/weather/v2/current": {
                final int lat = getQueryNum(query, "lat", 0);
                final int lon = getQueryNum(query, "lon", 0);
                final String tempUnit = getQueryString(query, "tempUnit", "CELSIUS");
                final String speedUnit = getQueryString(query, "speedUnit", "METERS_PER_SECOND");
                final String provider = getQueryString(query, "provider", "dci");
                weatherData = new WeatherForecastCurrent(weatherSpec, tempUnit, speedUnit);
                break;
            }
            default:
                LOG.warn("Unknown weather path {}", path);
                return null;
        }

        final String json = GSON.toJson(weatherData);
        LOG.debug("Weather response: {}", json);

        final GarminHttpResponse response = new GarminHttpResponse();
        response.setStatus(200);
        response.setBody(json.getBytes(StandardCharsets.UTF_8));
        response.getHeaders().put("Content-Type", "application/json");
        return response;
    }

    private static int getQueryNum(final Map<String, String> query, final String key, final int defaultValue) {
        final String str = query.get(key);
        if (str != null) {
            return Integer.parseInt(str);
        } else {
            return defaultValue;
        }
    }

    private static String getQueryString(final Map<String, String> query, final String key, final String defaultValue) {
        final String str = query.get(key);
        if (str != null) {
            return str;
        } else {
            return defaultValue;
        }
    }

    public static class WeatherForecastDay {
        public int dayOfWeek; // 1 monday .. 7 sunday
        public String description;
        public String summary;
        public WeatherValue high;
        public WeatherValue low;
        public Integer precipProb;
        public Integer icon;
        public Integer epochSunrise;
        public Integer epochSunset;
        public Wind wind;
        public Integer humidity;

        public WeatherForecastDay(final GregorianCalendar date, final WeatherSpec.Daily dailyForecast, final String tempUnit, final String speedUnit) {
            dayOfWeek = BLETypeConversions.dayOfWeekToRawBytes(date);
            description = "Unknown"; // TODO from conditionCode
            summary = "Unknown"; // TODO from conditionCode
            high = getTemperature(dailyForecast.maxTemp, tempUnit);
            low = getTemperature(dailyForecast.minTemp, tempUnit);
            precipProb = dailyForecast.precipProbability;
            icon = mapToGarminCondition(dailyForecast.conditionCode);

            if (dailyForecast.sunRise != 0 && dailyForecast.sunSet != 0) {
                epochSunrise = dailyForecast.sunRise;
                epochSunset = dailyForecast.sunSet;
            } else {
                final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();

                final SunriseTransitSet sunriseTransitSet = SPA.calculateSunriseTransitSet(
                        date.toZonedDateTime(),
                        lastKnownLocation.getLatitude(),
                        lastKnownLocation.getLongitude(),
                        DeltaT.estimate(date.toZonedDateTime().toLocalDate())
                );

                if (sunriseTransitSet.getSunrise() != null) {
                    epochSunrise = (int) (sunriseTransitSet.getSunrise().toInstant().getEpochSecond());
                }
                if (sunriseTransitSet.getSunset() != null) {
                    epochSunset = (int) (sunriseTransitSet.getSunset().toInstant().getEpochSecond());
                }
            }

            wind = new Wind(getSpeed(dailyForecast.windSpeed, speedUnit), dailyForecast.windDirection);
            humidity = dailyForecast.humidity;
        }
    }

    public static class WeatherForecastHour {
        public int epochSeconds;
        public String description;
        public WeatherValue temp;
        public Integer precipProb;
        public Wind wind;
        public Integer icon;
        public WeatherValue dewPoint;
        public Float uvIndex;
        public Integer relativeHumidity;
        public WeatherValue feelsLikeTemperature;
        public WeatherValue visibility;
        public WeatherValue pressure;
        public Object airQuality;
        public Integer cloudCover;

        public WeatherForecastHour(final WeatherSpec.Hourly hourlyForecast, final String tempUnit, final String speedUnit) {
            epochSeconds = hourlyForecast.timestamp;
            description = "Unknown"; // TODO from conditionCode
            temp = getTemperature(hourlyForecast.temp, tempUnit);
            precipProb = hourlyForecast.precipProbability;
            wind = new Wind(getSpeed(hourlyForecast.windSpeed, speedUnit), hourlyForecast.windDirection);
            icon = mapToGarminCondition(hourlyForecast.conditionCode);
            //dewPoint = new WeatherValue(hourlyForecast.temp - 273f, "CELSIUS"); // TODO dewPoint
            uvIndex = hourlyForecast.uvIndex;
            relativeHumidity = hourlyForecast.humidity;
            //feelsLikeTemperature = new WeatherValue(hourlyForecast.temp - 273f, "CELSIUS"); // TODO feelsLikeTemperature
            //visibility = new WeatherValue(0, "METER"); // TODO visibility
            //pressure = new WeatherValue(0f, "INCHES_OF_MERCURY"); // TODO pressure
            //airQuality = null; // TODO airQuality
            //cloudCover = 0; // TODO cloudCover
        }
    }

    public static class WeatherForecastCurrent {
        public Integer epochSeconds;
        public WeatherValue temperature;
        public String description;
        public Integer icon;
        public WeatherValue feelsLikeTemperature;
        public WeatherValue dewPoint;
        public Integer relativeHumidity;
        public Wind wind;
        public String locationName;
        public WeatherValue visibility;
        public WeatherValue pressure;
        public WeatherValue pressureChange;

        public WeatherForecastCurrent(final WeatherSpec weatherSpec, final String tempUnit, final String speedUnit) {
            epochSeconds = weatherSpec.timestamp;
            temperature = getTemperature(weatherSpec.currentTemp, tempUnit);
            description = weatherSpec.currentCondition;
            icon = mapToGarminCondition(weatherSpec.currentConditionCode);
            feelsLikeTemperature = getTemperature(weatherSpec.currentTemp, tempUnit);
            dewPoint = getTemperature(weatherSpec.dewPoint, tempUnit);
            relativeHumidity = weatherSpec.currentHumidity;
            wind = new Wind(getSpeed(weatherSpec.windSpeed, speedUnit), weatherSpec.windDirection);
            locationName = weatherSpec.location;
            visibility = new WeatherValue(weatherSpec.visibility, "METER");
            pressure = new WeatherValue(weatherSpec.pressure * 0.02953, "INCHES_OF_MERCURY");
            pressureChange = new WeatherValue(0f, "INCHES_OF_MERCURY");
        }
    }

    public static class WeatherValue {
        public Number value;
        public String units;

        public WeatherValue(final Number value, final String units) {
            this.value = value;
            this.units = units;
        }
    }

    public static class Wind {
        public WeatherValue speed;
        public String directionString;
        public Integer direction;

        public Wind(final WeatherValue speed, final int direction) {
            this.speed = speed;
            this.directionString = getWindDirection(direction);
            this.direction = direction;
        }
    }

    public static String getWindDirection(int degrees) {
        degrees = (degrees % 360 + 360) % 360;

        final String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        final int index = (int) Math.round(((double) degrees % 360) / 45);

        return directions[index % 8];
    }

    private static WeatherValue getTemperature(final int kelvin, final String unit) {
        switch (unit) {
            case "FAHRENHEIT":
                return new WeatherValue(WeatherUtils.celsiusToFahrenheit(kelvin - 273.15), "FAHRENHEIT");
            case "KELVIN":
                return new WeatherValue(kelvin, "KELVIN");
            case "CELSIUS":
            default:
                // #4313 - We do a "wrong" conversion to celsius on purpose
                return new WeatherValue(kelvin - 273, "CELSIUS");
        }
    }

    private static WeatherValue getSpeed(final float kmph, final String unit) {
        switch (unit) {
            case "METERS_PER_SECOND":
                return new WeatherValue(kmph / 3.6, "METERS_PER_SECOND");
            case "KILOMETERS_PER_HOUR":
            default:
                return new WeatherValue(kmph, "KILOMETERS_PER_HOUR");
        }
    }

    public static int mapToGarminCondition(final int openWeatherMapCondition) {
        // Icons mapped from a Venu 3:
        // 0 1 2 unk
        // 3 4 5 6 sunny
        // 7 8 9 10 sun cloudy
        // 11 12 cloudy with dashes below
        // 13 14 sun cloud 2 clouds
        // 15 16 clouds
        // 17 rain
        // 18 19 20 21 rain with sun (or night at night?)
        // 22 rain
        // 23 24 unk
        // 25 26 thunder with rain and sun behind
        // 27 thunder with rain
        // 28 29 rain
        // 30 31 32 33 34 snow with clouds
        // 35 36 37 snowflake
        // 38 snow with clouds, with big flake
        // 39 snow with rain
        // 40 41 snow with rain
        // 42 43 44 rain with snow
        // 45 rain with snow
        // 46 wind
        // 47 48 foggy (dashes?)
        // 49 50 51 unk

        switch (openWeatherMapCondition) {
        //Group 2xx: Thunderstorm
            case 210:  //light thunderstorm::  //11d
            case 200:  //thunderstorm with light rain:  //11d
            case 201:  //thunderstorm with rain:  //11d
            case 202:  //thunderstorm with heavy rain:  //11d
            case 230:  //thunderstorm with light drizzle:  //11d
            case 231:  //thunderstorm with drizzle:  //11d
            case 232:  //thunderstorm with heavy drizzle:  //11d
            case 211:  //thunderstorm:  //11d
            case 212:  //heavy thunderstorm:  //11d
            case 221:  //ragged thunderstorm:  //11d
                return 27;

        //Group 90x: Extreme
            case 901:  //tropical storm
        //Group 7xx: Atmosphere
            case 781:  //tornado:  //[[file:50d.png]]
        //Group 90x: Extreme
            case 900:  //tornado
        // Group 7xx: Atmosphere
            case 771:  //squalls:  //[[file:50d.png]]
        //Group 9xx: Additional
            case 960:  //storm
            case 961:  //violent storm
            case 902:  //hurricane
            case 962:  //hurricane
                return 46;

        //Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 301:  //drizzle:  //09d
            case 302:  //heavy intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 311:  //drizzle rain:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
            case 313:  //shower rain and drizzle:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
            case 321:  //shower drizzle:  //09d
        //Group 5xx: Rain
            case 500:  //light rain:  //10d
            case 501:  //moderate rain:  //10d
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
            case 522:  //heavy intensity shower rain:  //09d
            case 531:  //ragged shower rain:  //09d
                return 17;

        //Group 90x: Extreme
            case 906:  //hail
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
            case 511:  //freezing rain:  //13d
                return 40;

        //Group 6xx: Snow
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
            case 601:  //snow:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 602:  //heavy snow:  //[[file:13d.png]]
        //Group 6xx: Snow
            case 620:  //light shower snow:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return 38;


        //Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
            case 721:  //haze:  //[[file:50d.png]]
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
            case 741:  //fog:  //[[file:50d.png]]
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
            case 762:  //volcanic ash:  //[[file:50d.png]]
                return 47;

        //Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return 5;

        //Group 90x: Extreme
            case 904:  //hot
                return 5;

        //Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                return 8;
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return 15;

        //Group 80x: Clouds
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return 15;

        //Group 9xx: Additional
            case 905:  //windy
            case 951:  //calm
            case 952:  //light breeze
            case 953:  //gentle breeze
            case 954:  //moderate breeze
            case 955:  //fresh breeze
            case 956:  //strong breeze
            case 957:  //high windcase  near gale
            case 958:  //gale
            case 959:  //severe gale
                return 46;

            default:
        //Group 90x: Extreme
            case 903:  //cold
                return 35;
        }
    }
}
