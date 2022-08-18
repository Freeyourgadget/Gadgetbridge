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
import com.google.gson.JsonElement;
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
            .create();

    public static Response handleHttpRequest(final String path, final Map<String, String> query) {
        final WeatherSpec weatherSpec = Weather.getInstance().getWeatherSpec();

        if (weatherSpec == null) {
            LOG.error("No weather in weather instance");
            return null;
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
            default:
                LOG.error("Unknown weather path {}", path);
        }

        return null;
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

    public static abstract class Response {
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
        public List<Object> humidity = new ArrayList<>();
        public List<Range> temperature = new ArrayList<>();
        public List<Range> weather = new ArrayList<>();
        public List<Range> windDirection = new ArrayList<>();
        public List<Range> sunRiseSet = new ArrayList<>();
        public List<Range> windSpeed = new ArrayList<>();
        public List<Object> moonRiseSet = new ArrayList<>();
        public List<Object> airQualities = new ArrayList<>();

        public ForecastResponse(final WeatherSpec weatherSpec, final int days) {
            pubTime = new Date(weatherSpec.timestamp * 1000L);

            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(new Date(weatherSpec.timestamp * 1000L));

            // TODO: We should send sunrise on the same location as the weather
            final SimpleDateFormat sunRiseSetSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            final Location lastKnownLocation = new CurrentPosition().getLastKnownLocation();
            final GregorianCalendar sunriseDate = new GregorianCalendar();
            sunriseDate.setTime(calendar.getTime());

            for (int i = 0; i < Math.min(weatherSpec.forecasts.size(), days); i++) {
                final WeatherSpec.Forecast forecast = weatherSpec.forecasts.get(i);
                temperature.add(new Range(forecast.minTemp - 273, forecast.maxTemp - 273));
                final String weatherCode = String.valueOf(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(forecast.conditionCode) & 0xff); // is it?
                weather.add(new Range(weatherCode, weatherCode));

                final GregorianCalendar[] sunriseTransitSet = SPA.calculateSunriseTransitSet(
                        sunriseDate,
                        lastKnownLocation.getLatitude(),
                        lastKnownLocation.getLongitude(),
                        DeltaT.estimate(sunriseDate)
                );

                final String from = sunRiseSetSdf.format(sunriseTransitSet[0].getTime());
                final String to = sunRiseSetSdf.format(sunriseTransitSet[2].getTime());
                sunRiseSet.add(new Range(from, to));

                sunriseDate.add(Calendar.DAY_OF_MONTH, 1);

                windDirection.add(new Range(0, 0));
                windSpeed.add(new Range(0, 0));
            }
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
        public LocalDate date;
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
            weather = String.valueOf(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode) & 0xff); // is it?
            wind = new Wind(weatherSpec.windDirection, Math.round(weatherSpec.windSpeed));
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
        public Object pubTime;
        public Object weather;
        public Object temperature;
        public Object humidity;
        public Object fxTime;
        public Object windDirection;
        public Object windSpeed;
        public Object windScale;
    }

    // /weather/alerts
    //
    // locale=zh_CN
    // deviceSource=11
    // days=3
    // isGlobal=true
    // locationKey=00.000,-0.000,xiaomi_accu:000000
    public static class AlertsResponse extends Response {
    }

    private static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        @Override
        public JsonElement serialize(final LocalDate src, final Type typeOfSrc, final JsonSerializationContext context) {
            // Serialize as "yyyy-MM-dd" string
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }
}
