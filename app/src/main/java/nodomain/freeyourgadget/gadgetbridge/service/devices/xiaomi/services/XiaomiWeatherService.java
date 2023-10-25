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
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWeatherService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWeatherService.class);

    public static final int COMMAND_TYPE = 10;

    private static final int CMD_TEMPERATURE_UNIT_GET = 9;
    private static final int CMD_TEMPERATURE_UNIT_SET = 10;
    private static final int CMD_SET_CURRENT_WEATHER = 0;

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
        String timestamp = new StringBuilder(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                        .format(new Date(weatherSpec.timestamp * 1000L)))
                .insert(22, ':') // FIXME: I bet this fails for some, but all this java date craps sucks
                .toString();


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
                                                .setAQI(weatherSpec.airQuality.aqi)
                                        )
                                        .setWarning(XiaomiProto.WeatherCurrentWarning.newBuilder()
                                                .setCurrentWarning1(XiaomiProto.WeatherCurrentWarning1.newBuilder()
                                                        .setCurrentWarningText("")
                                                        .setCurrentWarningSeverityText("")
                                                )
                                        )
                                        .setPressure(weatherSpec.pressure)

                        ))
                        .build()
        );
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
}
