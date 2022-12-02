/*  Copyright (C) 2022 José Rebelo

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

import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            //.registerTypeAdapter(LocalDate.class, new LocalDateSerializer()) // Requires API 26
            .create();

    public static Response handleHttpRequest(final String path, final Map<String, String> query) {
        final WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) {
            LOG.error("No weather in weather instance");
            return new Huami2021Weather.ErrorResponse(404, -2001, "Not found");
        }

        switch (path) {
            case "/weather/v2/forecast":
                final String daysStr = query.get("days");
                final int days;
                if (daysStr != null) {
                    days = Integer.parseInt(daysStr);
                } else {
                    days = 10;
                }
                return new ForecastResponse(weatherSpec, days);
            case "/weather/index":
                return new IndexResponse(weatherSpec);
            case "/weather/current":
                return new CurrentResponse(weatherSpec);
            case "/weather/forecast/hourly":
                return new HourlyResponse();
            case "/weather/alerts":
                return new AlertsResponse();
            //case "/weather/tide":
            //    return new TideResponse(weatherSpec);
        }

        LOG.error("Unknown weather path {}", path);
        return new Huami2021Weather.ErrorResponse(404, -2001, "Not found");
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
        public List<String> humidity = new ArrayList<>();
        public List<Range> temperature = new ArrayList<>();
        public List<Range> weather = new ArrayList<>();
        public List<Range> windDirection = new ArrayList<>();
        public List<Range> sunRiseSet = new ArrayList<>();
        public List<Range> windSpeed = new ArrayList<>();
        public Object moonRiseSet = new Object(); // MoonRiseSet
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
            final String currentWeatherCode = String.valueOf(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode) & 0xff);
            weather.add(new Range(currentWeatherCode, currentWeatherCode));
            sunRiseSet.add(getSunriseSunset(sunriseDate, lastKnownLocation));
            sunriseDate.add(Calendar.DAY_OF_MONTH, 1);
            windDirection.add(new Range(0, 0));
            windSpeed.add(new Range(0, 0));

            for (int i = 0; i < actualDays; i++) {
                final WeatherSpec.Forecast forecast = weatherSpec.forecasts.get(i);
                temperature.add(new Range(forecast.minTemp - 273, forecast.maxTemp - 273));
                final String weatherCode = String.valueOf(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(forecast.conditionCode) & 0xff);
                weather.add(new Range(weatherCode, weatherCode));

                sunRiseSet.add(getSunriseSunset(sunriseDate, lastKnownLocation));
                sunriseDate.add(Calendar.DAY_OF_MONTH, 1);

                windDirection.add(new Range(0, 0));
                windSpeed.add(new Range(0, 0));
            }
        }

        private Range getSunriseSunset(final GregorianCalendar date, final Location location) {
            // TODO: We should send sunrise on the same location as the weather
            final SimpleDateFormat sunRiseSetSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

            final GregorianCalendar[] sunriseTransitSet = SPA.calculateSunriseTransitSet(
                    date,
                    location.getLatitude(),
                    location.getLongitude(),
                    DeltaT.estimate(date)
            );

            final String from = sunRiseSetSdf.format(sunriseTransitSet[0].getTime());
            final String to = sunRiseSetSdf.format(sunriseTransitSet[2].getTime());

            return new Range(from, to);
        }
    }

    private static class MoonRiseSet {
        public List<String> moonPhaseValue = new ArrayList<>(); // numbers? 20 21 23...
        public List<Range> moonRise = new ArrayList<>(); // yyyy-MM-dd HH:mm:ss
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

        public IndexResponse(final WeatherSpec weatherSpec) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);
        }
    }

    private static class IndexEntry {
        public String date; // YYYY-MM-DD, but LocalDate would need API 26+
        public String osi;
        public String uvi;
        public Object pai;
        public String cwi;
        public String fi;
    }

    // /weather/current
    //
    // locale=zh_CN
    // deviceSource=11
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class CurrentResponse extends Response {
        public CurrentWeatherModel currentWeatherModel;
        public AqiModel aqiModel = new AqiModel();

        public CurrentResponse(final WeatherSpec weatherSpec) {
            this.currentWeatherModel = new CurrentWeatherModel(weatherSpec);
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
            pressure = new UnitValue(Unit.PRESSURE_MB, "1015"); // ?
            pubTime = new Date(weatherSpec.timestamp * 1000L);
            temperature = new UnitValue(Unit.TEMPERATURE_C, weatherSpec.currentTemp - 273);
            uvIndex = "0";
            visibility = new UnitValue(Unit.KM, "");
            weather = String.valueOf(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode) & 0xff);
            wind = new Wind(weatherSpec.windDirection, Math.round(weatherSpec.windSpeed));
        }
    }

    private static class AqiModel {
        public String pm10 = "";
        public String pm25 = "";
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

        public TideResponse(final WeatherSpec weatherSpec) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);
        }
    }

    private static class TideDataEntry {
        public String date; // YYYY-MM-DD, but LocalDate would need API 26+
        public List<TideTableEntry> tideTable = new ArrayList<>();
        public List<TideHourlyEntry> tideHourly = new ArrayList<>();
    }

    private static class TideTableEntry {
        public Date fxTime; // pubTime format
        public String height; // float, x.xx
        public String type; // H / L
    }

    private static class TideHourlyEntry {
        public Date fxTime; // pubTime format
        public String height; // float, x.xx
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
        public List<String> weather;
        public List<String> temperature;
        public List<String> humidity;
        public List<String> fxTime; // pubTime format
        public List<String> windDirection;
        public List<String> windSpeed;
        public List<String> windScale; // each element in the form of 1-2
    }

    // /weather/alerts
    //
    // locale=zh_CN
    // deviceSource=11
    // days=3
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class AlertsResponse extends Response {
        public List<IndexEntry> alerts = new ArrayList<>();
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
    //private static class LocalDateSerializer implements JsonSerializer<LocalDate> {
    //    @Override
    //    public JsonElement serialize(final LocalDate src, final Type typeOfSrc, final JsonSerializationContext context) {
    //        // Serialize as "yyyy-MM-dd" string
    //        return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
    //    }
    //}
}
