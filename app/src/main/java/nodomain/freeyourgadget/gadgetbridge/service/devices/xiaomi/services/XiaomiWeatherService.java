/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiWeatherConditions;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWeatherService extends AbstractXiaomiService {
    public static final int COMMAND_TYPE = 10;
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWeatherService.class);
    private static final int CMD_TEMPERATURE_UNIT_GET = 9;
    private static final int CMD_TEMPERATURE_UNIT_SET = 10;
    private static final int CMD_SET_CURRENT_WEATHER = 0;
    private static final int CMD_SET_DAILY_WEATHER = 1;
    private static final int CMD_SET_CURRENT_LOCATION = 6;

    public XiaomiWeatherService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        // TODO
    }

    @Override
    public void initialize() {
        // TODO setMeasurementSystem();, or request
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                setMeasurementSystem();
                return true;
        }

        return false;
    }

    public void onSendWeather(final WeatherSpec weatherSpec) {
        String timestamp = unixTimetstampToISOWithColons(weatherSpec.timestamp);

        final XiaomiCoordinator coordinator = getSupport().getCoordinator();

        if (coordinator.supportsMultipleWeatherLocations()) {
            // TODO actually support multiple locations (primary + 4 secondary)
            getSupport().sendCommand(
                    "set current location",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_SET_CURRENT_LOCATION)
                            .setWeather(XiaomiProto.Weather.newBuilder().setCurrentLocation(
                                    XiaomiProto.WeatherCurrentLocation.newBuilder()
                                            .setLocation(XiaomiProto.WeatherLocation.newBuilder()
                                                    .setCode("accu:123456") // FIXME:AccuWeather code (we do not have it here)
                                                    .setName(weatherSpec.location)
                                            )
                            ))
                            .build()
            );
        }

        getSupport().sendCommand(
                "set current weather",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SET_CURRENT_WEATHER)
                        .setWeather(XiaomiProto.Weather.newBuilder().setCurrent(
                                XiaomiProto.WeatherCurrent.newBuilder()
                                        .setTimeLocation(XiaomiProto.WeatherCurrentTimeLocation.newBuilder()
                                                .setTimestamp(timestamp)
                                                .setUnk2("")
                                                .setCurrentLocationString(weatherSpec.location)
                                                .setCurrentLocationCode("accu:123456") // FIXME:AccuWeather code (we do not have it here)
                                                .setUnk5(true)
                                        )
                                        .setWeatherCondition(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.currentConditionCode)) // *SEEMS* to work
                                        .setTemperature(XiaomiProto.WeatherCurrentTemperature.newBuilder()
                                                .setDegrees(weatherSpec.currentTemp - 273) // TODO: support inches for weather
                                                .setSymbol("℃")
                                        )
                                        .setHumidity(XiaomiProto.WeatherCurrentHumidity.newBuilder()
                                                .setHumidity(weatherSpec.currentHumidity)
                                                .setSymbol("%")
                                        )
                                        .setUnk5(XiaomiProto.WeatherCurrentUnk5.newBuilder()
                                                .setUnk1("")
                                                .setUnk2(0)
                                        )
                                        .setUnk6(XiaomiProto.WeatherCurrentUnk6.newBuilder()
                                                .setUnk1("")
                                                .setUnk2(0)
                                        )
                                        .setAQI(XiaomiProto.WeatherCurrentAQI.newBuilder()
                                                .setAQIText("Unknown") // some string like "Moderate"
                                                .setAQI(weatherSpec.airQuality != null && weatherSpec.airQuality.aqi >= 0 ? weatherSpec.airQuality.aqi : 0)
                                        )
                                        .setWarning(XiaomiProto.WeatherCurrentWarning.newBuilder()
                                                .addCurrentWarning1(XiaomiProto.WeatherCurrentWarning1.newBuilder()
                                                        .setCurrentWarningText("")
                                                        .setCurrentWarningSeverityText("")
                                                )
                                        )
                                        .setPressure(weatherSpec.pressure)
                        ))
                        .build()
        );


        if (weatherSpec.forecasts != null) {
            XiaomiProto.WeatherDailyList.Builder dailyListBuilder = XiaomiProto.WeatherDailyList.newBuilder();
            int daysToSend = Math.min(7, weatherSpec.forecasts.size());
            for (int i = 0; i < daysToSend; i++) {
                dailyListBuilder.addForecastDay(XiaomiProto.WeatherDailyForecastDay.newBuilder()
                        .setUnk1(XiaomiProto.DailyUnk1.newBuilder()
                                .setUnk1("")
                                .setUnk2(0)
                        )
                        .setUnk2(XiaomiProto.DailyUnk2.newBuilder()
                                .setUnk1(HuamiWeatherConditions.mapToAmazfitBipWeatherCode(weatherSpec.forecasts.get(i).conditionCode)) // TODO: verify
                                .setUnk2(0)
                        )
                        .setHighLowTemp(XiaomiProto.DailyHighLowTemp.newBuilder()
                                .setHigh(weatherSpec.forecasts.get(i).maxTemp - 273)
                                .setLow(weatherSpec.forecasts.get(i).minTemp - 273)
                        )
                        .setTemperatureSymbol("℃")
                        .setSunriseSunset(XiaomiProto.DailySunriseSunset.newBuilder()
                                .setSunrise(weatherSpec.forecasts.get(i).sunRise != 0 ? unixTimetstampToISOWithColons(weatherSpec.forecasts.get(i).sunRise) : "")
                                .setSunset(weatherSpec.forecasts.get(i).sunSet != 0 ? unixTimetstampToISOWithColons(weatherSpec.forecasts.get(i).sunSet) : "")
                        )
                );
            }

            getSupport().sendCommand(
                    "set daily forecast",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_SET_DAILY_WEATHER)
                            .setWeather(XiaomiProto.Weather.newBuilder().setDaily(
                                    XiaomiProto.WeatherDaily.newBuilder()
                                            .setTimeLocation(XiaomiProto.WeatherCurrentTimeLocation.newBuilder()
                                                    .setTimestamp(timestamp)
                                                    .setUnk2("")
                                                    .setCurrentLocationString(weatherSpec.location)
                                                    .setCurrentLocationCode("accu:123456") // FIXME:AccuWeather code (we do not have it here)
                                                    .setUnk5(true)
                                            )
                                            .setDailyList(dailyListBuilder)
                            ))
                            .build()
            );
        }
    }

    private void setMeasurementSystem() {
        final Prefs prefs = getDevicePrefs();
        final String measurementSystem = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric");
        LOG.info("Setting measurement system to {}", measurementSystem);

        final int unitValue = "metric".equals(measurementSystem) ? 1 : 2;

        getSupport().sendCommand(
                "set temperature unit",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_TEMPERATURE_UNIT_SET)
                        .setWeather(XiaomiProto.Weather.newBuilder().setTemperatureUnit(
                                XiaomiProto.WeatherTemperatureUnit.newBuilder().setUnit(unitValue)
                        ))
                        .build()
        );
    }

    private String unixTimetstampToISOWithColons(int timestamp) {
        return new StringBuilder(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                        .format(new Date(timestamp * 1000L)))
                .insert(22, ':') // FIXME: I bet this fails for some, but all this java date craps sucks
                .toString();
    }
}
