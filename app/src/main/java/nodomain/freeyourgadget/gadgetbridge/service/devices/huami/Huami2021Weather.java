/*  Copyright (C) 2022-2023 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;

/**
 * The weather models that the bands expect as an http response to weather requests. Base URL usually
 * is https://api-mifit.huami.com.
 */
public class Huami2021Weather {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021Weather.class);

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") // for pubTimes
            .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
            // These serializers keep the fields in order. Not sure if this is needed, but the debug
            // logs show some errors when deserializing the json payloads.
            .registerTypeAdapter(ForecastResponse.class, new ForecastResponse.Serializer())
            .registerTypeAdapter(MoonRiseSet.class, new MoonRiseSet.Serializer())
            .registerTypeAdapter(Range.class, new Range.Serializer())
            .registerTypeAdapter(IndexResponse.class, new IndexResponse.Serializer())
            .registerTypeAdapter(IndexEntry.class, new IndexEntry.Serializer())
            .registerTypeAdapter(CurrentResponse.class, new CurrentResponse.Serializer())
            .registerTypeAdapter(CurrentWeatherModel.class, new CurrentWeatherModel.Serializer())
            .registerTypeAdapter(AqiModel.class, new AqiModel.Serializer())
            .registerTypeAdapter(UnitValue.class, new UnitValue.Serializer())
            .registerTypeAdapter(HourlyResponse.class, new HourlyResponse.Serializer())
            .registerTypeAdapter(AlertsResponse.class, new AlertsResponse.Serializer())
            .registerTypeAdapter(TideResponse.class, new TideResponse.Serializer())
            .registerTypeAdapter(TideDataEntry.class, new TideDataEntry.Serializer())
            .registerTypeAdapter(TideTableEntry.class, new TideTableEntry.Serializer())
            .registerTypeAdapter(TideHourlyEntry.class, new TideHourlyEntry.Serializer())
            .create();

    public static Response handleHttpRequest(final String path, final Map<String, String> query) {
        final WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) {
            LOG.error("No weather in weather instance");
            return new Huami2021Weather.ErrorResponse(404, -2001, "Not found");
        }

        switch (path) {
            case "/weather/v2/forecast":
                final int forecastDays = getQueryNum(query, "days", 10);
                return new ForecastResponse(weatherSpec, forecastDays);
            case "/weather/index":
                final int indexDays = getQueryNum(query, "days", 3);
                return new IndexResponse(weatherSpec, indexDays);
            case "/weather/current":
                return new CurrentResponse(weatherSpec);
            case "/weather/forecast/hourly":
                final int hours = getQueryNum(query, "hours", 72);
                return new HourlyResponse(weatherSpec, hours);
            case "/weather/alerts":
                return new AlertsResponse(weatherSpec);
            case "/weather/tide":
                final int tideDays = getQueryNum(query, "days", 10);
                return new TideResponse(weatherSpec, tideDays);
        }

        LOG.error("Unknown weather path {}", path);
        return new Huami2021Weather.ErrorResponse(404, -2001, "Not found");
    }

    private static int getQueryNum(final Map<String, String> query, final String key, final int defaultValue) {
        final String daysStr = query.get(key);
        if (daysStr != null) {
            return Integer.parseInt(daysStr);
        } else {
            return defaultValue;
        }
    }

    private static class RawJsonStringResponse extends Response {
        private final String content;

        public RawJsonStringResponse(final String content) {
            this.content = content;
        }

        public String toJson() {
            return content;
        }
    }

    public static class ErrorResponse extends Response {
        private final int httpStatusCode;
        private final int errorCode;
        private final String message;

        public ErrorResponse(final int httpStatusCode, final int errorCode, final String message) {
            this.httpStatusCode = httpStatusCode;
            this.errorCode = errorCode;
            this.message = message;
        }

        @Override
        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

    public static abstract class Response {
        public int getHttpStatusCode() {
            return 200;
        }

        public String toJson() {
            return GSON.toJson(this);
        }
    }

    // /weather/v2/forecast
    //
    // locale=zh_CN
    // deviceSource=11
    // days=10
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class ForecastResponse extends Response {
        public Date pubTime;
        public List<String> humidity = new ArrayList<>(); // int
        public List<Range> temperature = new ArrayList<>(); // ints
        public List<Range> weather = new ArrayList<>();
        public List<Range> windDirection = new ArrayList<>();
        public List<Range> sunRiseSet = new ArrayList<>();
        public List<Range> windSpeed = new ArrayList<>();
        public MoonRiseSet moonRiseSet = new MoonRiseSet();
        public List<Object> airQualities = new ArrayList<>();

        public ForecastResponse(final WeatherSpec weatherSpec, final int days) {
            final int actualDays = Math.min(weatherSpec.forecasts.size(), days - 1); // leave one slot for the first day

            pubTime = new Date(weatherSpec.timestamp * 1000L);

            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(pubTime);

            final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();
            final GregorianCalendar sunriseDate = new GregorianCalendar();
            sunriseDate.setTime(calendar.getTime());

            // First one is for the current day
            temperature.add(new Range(weatherSpec.todayMinTemp - 273, weatherSpec.todayMaxTemp - 273));
            final String currentWeatherCode = String.valueOf(mapToZeppOsWeatherCode(weatherSpec.currentConditionCode));
            weather.add(new Range(currentWeatherCode, currentWeatherCode));
            if (weatherSpec.sunRise != 0 && weatherSpec.sunSet != 0) {
                sunRiseSet.add(getSunriseSunset(new Date(weatherSpec.sunRise * 1000L), new Date(weatherSpec.sunSet * 1000L)));
            } else if (weatherSpec.getLocation() != null) {
                sunRiseSet.add(getSunriseSunset(sunriseDate, weatherSpec.getLocation()));
            } else {
                sunRiseSet.add(getSunriseSunset(sunriseDate, lastKnownLocation));
            }
            sunriseDate.add(Calendar.DAY_OF_MONTH, 1);
            windDirection.add(new Range(weatherSpec.windDirection, weatherSpec.windDirection));
            windSpeed.add(new Range(Math.round(weatherSpec.windSpeed), Math.round(weatherSpec.windSpeed)));

            moonRiseSet.add(weatherSpec.moonRise, weatherSpec.moonSet, weatherSpec.moonPhase);

            for (int i = 0; i < actualDays; i++) {
                final WeatherSpec.Daily forecast = weatherSpec.forecasts.get(i);
                temperature.add(new Range(forecast.minTemp - 273, forecast.maxTemp - 273));
                final String weatherCode = String.valueOf(mapToZeppOsWeatherCode(forecast.conditionCode));
                weather.add(new Range(weatherCode, weatherCode));

                if (forecast.sunRise != 0 && forecast.sunSet != 0) {
                    sunRiseSet.add(getSunriseSunset(new Date(forecast.sunRise * 1000L), new Date(forecast.sunSet * 1000L)));
                } else {
                    sunRiseSet.add(getSunriseSunset(sunriseDate, lastKnownLocation));
                }
                sunriseDate.add(Calendar.DAY_OF_MONTH, 1);

                if (forecast.windDirection != -1) {
                    windDirection.add(new Range(forecast.windDirection, forecast.windDirection));
                } else {
                    windDirection.add(new Range(0, 0));
                }

                if (forecast.windSpeed != -1) {
                    windSpeed.add(new Range(Math.round(forecast.windSpeed), Math.round(forecast.windSpeed)));
                } else {
                    windSpeed.add(new Range(0, 0));
                }

                moonRiseSet.add(forecast.moonRise, forecast.moonSet, forecast.moonPhase);
            }
        }

        private Range getSunriseSunset(final GregorianCalendar date, final Location location) {
            final GregorianCalendar[] sunriseTransitSet = SPA.calculateSunriseTransitSet(
                    date,
                    location.getLatitude(),
                    location.getLongitude(),
                    DeltaT.estimate(date)
            );

            return getSunriseSunset(sunriseTransitSet[0].getTime(), sunriseTransitSet[2].getTime());
        }

        private Range getSunriseSunset(final Date sunRise, final Date sunSet) {
            final SimpleDateFormat sunRiseSetSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            sunRiseSetSdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            final String from = sunRiseSetSdf.format(sunRise);
            final String to = sunRiseSetSdf.format(sunSet);

            return new Range(from, to);
        }

        public static class Serializer implements JsonSerializer<ForecastResponse> {
            @Override
            public JsonElement serialize(final ForecastResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("humidity", context.serialize(obj.humidity));
                object.add("temperature", context.serialize(obj.temperature));
                object.add("weather", context.serialize(obj.weather));
                object.add("windDirection", context.serialize(obj.windDirection));
                object.add("sunRiseSet", context.serialize(obj.sunRiseSet));
                object.add("windSpeed", context.serialize(obj.windSpeed));
                object.add("moonRiseSet", context.serialize(obj.moonRiseSet));
                object.add("airQualities", context.serialize(obj.airQualities));
                return object;
            }
        }
    }

    private static class MoonRiseSet {
        public List<String> moonPhaseValue = new ArrayList<>(); // numbers? 20 21 23...
        public List<Range> moonRise = new ArrayList<>(); // yyyy-MM-dd HH:mm:ss

        public static class Serializer implements JsonSerializer<MoonRiseSet> {
            @Override
            public JsonElement serialize(final MoonRiseSet obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("moonPhaseValue", context.serialize(obj.moonPhaseValue));
                object.add("moonRise", context.serialize(obj.moonRise));
                return object;
            }
        }

        public void add(final int rise, final int set, final int phase) {
            moonPhaseValue.add(String.valueOf(phase));

            final SimpleDateFormat moonRiseSetSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            moonRiseSetSdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            final String from = moonRiseSetSdf.format(new Date(rise * 1000L));
            final String to = moonRiseSetSdf.format(new Date(set * 1000L));

            moonRise.add(new Range(from, to));
        }
    }

    private static class Range {
        public String from;
        public String to;

        public Range(final String from, final String to) {
            this.from = from;
            this.to = to;
        }

        public Range(final int from, final int to) {
            this.from = String.valueOf(from);
            this.to = String.valueOf(to);
        }

        public static class Serializer implements JsonSerializer<Range> {
            @Override
            public JsonElement serialize(final Range obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("from", context.serialize(obj.from));
                object.add("to", context.serialize(obj.to));
                return object;
            }
        }
    }

    // /weather/index
    //
    // locale=zh_CN
    // deviceSource=11
    // days=3
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class IndexResponse extends Response {
        public Date pubTime;
        public List<IndexEntry> dataList = new ArrayList<>();

        public IndexResponse(final WeatherSpec weatherSpec, final int days) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);
        }

        public static class Serializer implements JsonSerializer<IndexResponse> {
            @Override
            public JsonElement serialize(final IndexResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("dataList", context.serialize(obj.dataList));
                return object;
            }
        }
    }

    private static class IndexEntry {
        public LocalDate date;
        public String osi; // int
        public String uvi; // int
        public Object pai;
        public String cwi; // int
        public String fi; // int

        public static class Serializer implements JsonSerializer<IndexEntry> {
            @Override
            public JsonElement serialize(final IndexEntry obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("date", context.serialize(obj.date));
                object.add("osi", context.serialize(obj.osi));
                object.add("uvi", context.serialize(obj.uvi));
                object.add("pai", context.serialize(obj.pai));
                object.add("cwi", context.serialize(obj.cwi));
                object.add("fi", context.serialize(obj.fi));
                return object;
            }
        }
    }

    // /weather/current
    //
    // locale=zh_CN
    // deviceSource=11
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class CurrentResponse extends Response {
        public CurrentWeatherModel currentWeatherModel;
        public AqiModel aqiModel;

        public CurrentResponse(final WeatherSpec weatherSpec) {
            this.currentWeatherModel = new CurrentWeatherModel(weatherSpec);
            this.aqiModel = new AqiModel(weatherSpec);
        }

        public static class Serializer implements JsonSerializer<CurrentResponse> {
            @Override
            public JsonElement serialize(final CurrentResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("currentWeatherModel", context.serialize(obj.currentWeatherModel));
                object.add("aqiModel", context.serialize(obj.aqiModel));
                return object;
            }
        }
    }

    private static class CurrentWeatherModel {
        public UnitValue humidity;
        public UnitValue pressure;
        public Date pubTime;
        public UnitValue temperature;
        public String uvIndex;
        public UnitValue visibility;
        public String weather;
        public Wind wind;

        public CurrentWeatherModel(final WeatherSpec weatherSpec) {
            humidity = new UnitValue(Unit.PERCENTAGE, weatherSpec.currentHumidity);
            pressure = new UnitValue(Unit.PRESSURE_MB, Math.round(weatherSpec.pressure));
            pubTime = new Date(weatherSpec.timestamp * 1000L);
            temperature = new UnitValue(Unit.TEMPERATURE_C, weatherSpec.currentTemp - 273);
            uvIndex = String.valueOf(Math.round(weatherSpec.uvIndex));
            visibility = new UnitValue(Unit.KM, Math.round(weatherSpec.visibility / 1000));
            weather = String.valueOf(mapToZeppOsWeatherCode(weatherSpec.currentConditionCode));
            wind = new Wind(weatherSpec.windDirection, Math.round(weatherSpec.windSpeed));
        }

        public static class Serializer implements JsonSerializer<CurrentWeatherModel> {
            @Override
            public JsonElement serialize(final CurrentWeatherModel obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("humidity", context.serialize(obj.humidity));
                object.add("pressure", context.serialize(obj.pressure));
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("temperature", context.serialize(obj.temperature));
                object.add("uvIndex", context.serialize(obj.uvIndex));
                object.add("visibility", context.serialize(obj.visibility));
                object.add("weather", context.serialize(obj.weather));
                object.add("wind", context.serialize(obj.wind));
                return object;
            }
        }
    }

    private static class AqiModel {
        public String aqi; // int
        public String co; // float
        public String no2; // float
        public String o3; // float
        public String pm10; // int
        public String pm25; // int
        public Date pubTime; // 2023-05-14T12:00:00-0400
        public String so2; // float

        public AqiModel(final WeatherSpec weatherSpec) {
            if (weatherSpec.airQuality == null) {
                return;
            }

            this.aqi = String.valueOf(weatherSpec.airQuality.aqi);
            this.co = String.format(Locale.ROOT, "%.1f", (float) weatherSpec.airQuality.coAqi);
            this.no2 = String.format(Locale.ROOT, "%.1f", (float) weatherSpec.airQuality.no2Aqi);
            this.o3 = String.format(Locale.ROOT, "%.1f", (float) weatherSpec.airQuality.o3Aqi);
            this.pm10 = String.valueOf(Math.round((float) weatherSpec.airQuality.pm10Aqi));
            this.pm25 = String.valueOf(Math.round((float) weatherSpec.airQuality.pm25Aqi));
            this.pubTime = new Date(weatherSpec.timestamp * 1000L);
            this.so2 = String.format(Locale.ROOT, "%.1f", (float) weatherSpec.airQuality.so2Aqi);
        }

        public static class Serializer implements JsonSerializer<AqiModel> {
            @Override
            public JsonElement serialize(final AqiModel obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("aqi", context.serialize(obj.aqi));
                object.add("co", context.serialize(obj.co));
                object.add("no2", context.serialize(obj.no2));
                object.add("o3", context.serialize(obj.o3));
                object.add("pm10", context.serialize(obj.pm10));
                object.add("pm25", context.serialize(obj.pm25));
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("so2", context.serialize(obj.so2));
                return object;
            }
        }
    }

    // /weather/tide
    //
    // locale=en_US
    // deviceSource=7930113
    // days=10
    // isGlobal=true
    // latitude=00.000
    // longitude=-00.000
    private static class TideResponse extends Response {
        public Date pubTime;
        public String poiName; // poi tide station name
        public String poiKey; // lat,lon,POI_ID
        public List<TideDataEntry> tideData = new ArrayList<>();

        public TideResponse(final WeatherSpec weatherSpec, int tideDays) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);

            // Fill all entries, even if without data
            final Calendar pubTimeDate = Calendar.getInstance();
            pubTimeDate.setTime(pubTime);
            LocalDate tideDate = LocalDate.of(pubTimeDate.get(Calendar.YEAR), pubTimeDate.get(Calendar.MONTH) + 1, pubTimeDate.get(Calendar.DAY_OF_MONTH));
            for (int i = 0; i < tideDays; i++, tideDate = tideDate.plusDays(1)) {
                final TideDataEntry tideDataEntry = new TideDataEntry();
                tideDataEntry.date = tideDate;
                tideData.add(tideDataEntry);
            }
        }

        public static class Serializer implements JsonSerializer<TideResponse> {
            @Override
            public JsonElement serialize(final TideResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("poiName", context.serialize(obj.poiName));
                object.add("poiKey", context.serialize(obj.poiKey));
                object.add("tideData", context.serialize(obj.tideData));
                return object;
            }
        }
    }

    private static class TideDataEntry {
        public LocalDate date;
        public List<TideTableEntry> tideTable = new ArrayList<>();
        public List<TideHourlyEntry> tideHourly = new ArrayList<>();

        public static class Serializer implements JsonSerializer<TideDataEntry> {
            @Override
            public JsonElement serialize(final TideDataEntry obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("date", context.serialize(obj.date));
                object.add("tideTable", context.serialize(obj.tideTable));
                object.add("tideHourly", context.serialize(obj.tideHourly));
                return object;
            }
        }
    }

    private static class TideTableEntry {
        public Date fxTime; // pubTime format
        public String height; // float, x.xx
        public String type; // H / L

        public static class Serializer implements JsonSerializer<TideTableEntry> {
            @Override
            public JsonElement serialize(final TideTableEntry obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("fxTime", context.serialize(obj.fxTime));
                object.add("height", context.serialize(obj.height));
                object.add("type", context.serialize(obj.type));
                return object;
            }
        }
    }

    private static class TideHourlyEntry {
        public Date fxTime; // pubTime format
        public String height; // float, x.xx

        public static class Serializer implements JsonSerializer<TideHourlyEntry> {
            @Override
            public JsonElement serialize(final TideHourlyEntry obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("fxTime", context.serialize(obj.fxTime));
                object.add("height", context.serialize(obj.height));
                return object;
            }
        }
    }

    private enum Unit {
        PRESSURE_MB("mb"),
        PERCENTAGE("%"),
        TEMPERATURE_C("℃"), // e2 84 83 in UTF-8
        WIND_DEGREES("°"), // c2 b0 in UTF-8
        KM("km"),
        KPH("km/h"),
        ;

        private final String value;

        Unit(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class UnitValue {
        public String unit;
        public String value;

        public UnitValue(final Unit unit, final String value) {
            this.unit = unit.getValue();
            this.value = value;
        }

        public UnitValue(final Unit unit, final int value) {
            this.unit = unit.getValue();
            this.value = String.valueOf(value);
        }

        public static class Serializer implements JsonSerializer<UnitValue> {
            @Override
            public JsonElement serialize(final UnitValue obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("unit", context.serialize(obj.unit));
                object.add("value", context.serialize(obj.value));
                return object;
            }
        }
    }

    private static class Wind {
        public UnitValue direction;
        public UnitValue speed;

        public Wind(final int direction, final int speed) {
            this.direction = new UnitValue(Unit.WIND_DEGREES, direction);
            this.speed = new UnitValue(Unit.KPH, Math.round(speed));
        }
    }

    // /weather/forecast/hourly
    //
    // locale=zh_CN
    // deviceSource=11
    // hourly=72
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class HourlyResponse extends Response {
        public Date pubTime;
        // One entry in each list per hour
        public List<String> weather = new ArrayList<>();
        public List<String> temperature = new ArrayList<>();
        public List<String> humidity = new ArrayList<>();
        public List<Date> fxTime = new ArrayList<>(); // pubTime format
        public List<String> windDirection = new ArrayList<>();
        public List<String> windSpeed = new ArrayList<>();
        public List<String> windScale = new ArrayList<>(); // each element in the form of 1-2

        public HourlyResponse(final WeatherSpec weatherSpec, final int hours) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);

            if (weatherSpec.hourly == null || weatherSpec.hourly.isEmpty()) {
                // We don't have hourly data, but some devices refuse to open the weather app without it

                final Calendar fxTimeCalendar = Calendar.getInstance();
                fxTimeCalendar.setTime(pubTime);
                fxTimeCalendar.set(Calendar.MINUTE, 0);
                fxTimeCalendar.set(Calendar.SECOND, 0);
                fxTimeCalendar.set(Calendar.MILLISECOND, 0);
                fxTimeCalendar.add(Calendar.HOUR, 1);

                for (int i = 0; i < hours; i++) {
                    weather.add(String.valueOf(mapToZeppOsWeatherCode(weatherSpec.currentConditionCode)));
                    temperature.add(String.valueOf(weatherSpec.currentTemp - 273));
                    humidity.add(String.valueOf(weatherSpec.currentHumidity));
                    fxTime.add(fxTimeCalendar.getTime());
                    windDirection.add(String.valueOf(weatherSpec.windDirection));
                    windSpeed.add(String.valueOf(Math.round(weatherSpec.windSpeed)));
                    windScale.add("1-2");

                    fxTimeCalendar.add(Calendar.HOUR, 1);
                }
            } else {
                int i = 0;

                for (final WeatherSpec.Hourly hourly : weatherSpec.hourly) {
                    if (hourly.timestamp < weatherSpec.timestamp) {
                        continue;
                    }

                    weather.add(String.valueOf(mapToZeppOsWeatherCode(hourly.conditionCode)));
                    temperature.add(String.valueOf(hourly.temp - 273));
                    humidity.add(String.valueOf(hourly.humidity));
                    fxTime.add(new Date(hourly.timestamp * 1000L));
                    windDirection.add(String.valueOf(hourly.windDirection));
                    windSpeed.add(String.valueOf(Math.round(hourly.windSpeed)));
                    windScale.add(hourly.windSpeedAsBeaufort() + "-" + hourly.windSpeedAsBeaufort());

                    if (++i >= hours) {
                        break;
                    }
                }
            }
        }

        public static class Serializer implements JsonSerializer<HourlyResponse> {
            @Override
            public JsonElement serialize(final HourlyResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("pubTime", context.serialize(obj.pubTime));
                object.add("weather", context.serialize(obj.weather));
                object.add("temperature", context.serialize(obj.temperature));
                object.add("humidity", context.serialize(obj.humidity));
                object.add("fxTime", context.serialize(obj.fxTime));
                object.add("windDirection", context.serialize(obj.windDirection));
                object.add("windSpeed", context.serialize(obj.windSpeed));
                object.add("windScale", context.serialize(obj.windScale));
                return object;
            }
        }
    }

    // /weather/alerts
    //
    // locale=zh_CN
    // deviceSource=11
    // days=3
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class AlertsResponse extends Response {
        public List<Object> alerts = new ArrayList<>();

        public AlertsResponse(final WeatherSpec weatherSpec) {

        }

        public static class Serializer implements JsonSerializer<AlertsResponse> {
            @Override
            public JsonElement serialize(final AlertsResponse obj, final Type type, final JsonSerializationContext context) {
                final JsonObject object = new JsonObject();
                object.add("alerts", context.serialize(obj.alerts));
                return object;
            }
        }
    }

    private static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        @Override
        public JsonElement serialize(final LocalDate src, final Type typeOfSrc, final JsonSerializationContext context) {
            // Serialize as "YYYY-MM-DD" string
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }

    public static final int SUNNY = 0;
    public static final int SOME_CLOUDS = 1;
    public static final int CLOUDY = 2;
    public static final int RAIN_WITH_SUN = 3;
    public static final int THUNDERSTORM = 4;
    public static final int HAIL = 5;
    public static final int SLEET = 6;
    public static final int LIGHT_RAIN = 7;
    public static final int MODERATE_RAIN = 8;
    public static final int HEAVY_RAIN = 9;
    public static final int RAINSTORM = 10;
    public static final int HEAVY_RAINSTORM = 11;
    public static final int EXTRAORDINARY_RAINSTORM = 12;
    public static final int SNOW_SHOWER_WITH_SOME_SUN = 13;
    public static final int LIGHT_SNOW = 14;
    public static final int MODERATE_SNOW = 15;
    public static final int HEAVY_SNOW = 16;
    public static final int SNOWSTORM = 17;
    public static final int FOG = 18;
    public static final int FREEZING_RAIN = 19;
    public static final int SANDSTORM = 20;
    public static final int LIGHT_TO_MODERATE_RAIN = 21;
    public static final int MODERATE_TO_HEAVY_RAIN = 22;
    public static final int HEAVY_RAIN_TO_RAINSTORM = 23;
    public static final int RAINSTORM_TO_HEAVY_RAIN = 24;
    public static final int HEAVY_TO_SEVERE_STORM = 25;
    public static final int LIGHT_TO_MODERATE_SNOW = 26;
    public static final int MODERATE_TO_HEAVY_SNOW = 27;
    public static final int HEAVY_SNOW_TO_SNOWSTORM = 28;
    public static final int DUST = 29;
    public static final int SAND_BLOWING = 30;
    public static final int STRONG_SANDSTORM = 31;
    public static final int DENSE_FOG = 32;
    public static final int SNOW = 33;

    public static int mapToZeppOsWeatherCode(final int openWeatherMapCondition) {
        // openweathermap.org conditions:
        // http://openweathermap.org/weather-conditions
        switch (openWeatherMapCondition) {
            case 511:  //freezing rain:  //13d
                return FREEZING_RAIN;
            case 731:  //sand/dust whirls:  //50d
            case 751:  //sand:  //50d
                return SAND_BLOWING;
            case 761:  //dust:  //50d
            case 762:  //volcanic ash:  //50d
                return DUST;
            case 741:  //fog:  //50d
                return DENSE_FOG;
            default:
                return HuamiWeatherConditions.mapToAmazfitBipWeatherCode(openWeatherMapCondition) & 0xff;
        }
    }
}
