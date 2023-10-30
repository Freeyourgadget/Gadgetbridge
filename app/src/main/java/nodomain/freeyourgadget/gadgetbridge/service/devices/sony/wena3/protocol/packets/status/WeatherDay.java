/*  Copyright (C) 2023 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class WeatherDay {
    public final Weather day;
    public final Weather night;
    public final int temperatureMaximum;
    public final int temperatureMinimum;

    public WeatherDay(Weather day, Weather night, int temperatureMaximum, int temperatureMinimum) {
        this.day = day;
        this.night = night;
        // For some reason, Wena uses Farenheit on the wire, but Celsius on display...
        // Assume a middle ground input in Kelvin.
        this.temperatureMaximum = (int) Math.round(WeatherUtils.celsiusToFahrenheit(temperatureMaximum - 273.15));
        this.temperatureMinimum = (int) Math.round(WeatherUtils.celsiusToFahrenheit(temperatureMinimum - 273.15));
    }

    public static WeatherDay fromSpec(WeatherSpec.Daily daily) {
        return new WeatherDay(
                Weather.fromOpenWeatherMap(daily.conditionCode),
                Weather.fromOpenWeatherMap(daily.conditionCode),
                daily.maxTemp,
                daily.minTemp
        );
    }
}

