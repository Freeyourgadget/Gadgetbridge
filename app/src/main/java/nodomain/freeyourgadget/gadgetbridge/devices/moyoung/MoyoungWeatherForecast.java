/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class MoyoungWeatherForecast {
    public final byte conditionId;
    public final byte minTemp;
    public final byte maxTemp;

    public MoyoungWeatherForecast(byte conditionId, byte minTemp, byte maxTemp) {
        this.conditionId = conditionId;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public MoyoungWeatherForecast(WeatherSpec.Forecast forecast)
    {
        conditionId = MoyoungConstants.openWeatherConditionToMoyoungConditionId(forecast.conditionCode);
        minTemp = (byte)(forecast.minTemp - 273); // Kelvin -> Celcius
        maxTemp = (byte)(forecast.maxTemp - 273); // Kelvin -> Celcius
    }
}
