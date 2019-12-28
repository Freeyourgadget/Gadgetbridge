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
package nodomain.freeyourgadget.gadgetbridge.devices.dafit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class DaFitWeatherToday {
    public final byte conditionId;
    public final byte currentTemp;
    public final Short pm25; // (*)
    public final String lunar_or_festival; // (*)
    public final String city; // (*)

    public DaFitWeatherToday(byte conditionId, byte currentTemp, @Nullable Short pm25, @NonNull String lunar_or_festival, @NonNull String city) {
        if (lunar_or_festival.length() != 4)
            throw new IllegalArgumentException("lunar_or_festival");
        if (city.length() != 4)
            throw new IllegalArgumentException("city");
        this.conditionId = conditionId;
        this.currentTemp = currentTemp;
        this.pm25 = pm25;
        this.lunar_or_festival = lunar_or_festival;
        this.city = city;
    }

    public DaFitWeatherToday(WeatherSpec weatherSpec)
    {
        conditionId = DaFitConstants.openWeatherConditionToDaFitConditionId(weatherSpec.currentConditionCode);
        currentTemp = (byte)(weatherSpec.currentTemp - 273); // Kelvin -> Celcius
        pm25 = null;
        lunar_or_festival = StringUtils.pad("", 4);
        city = StringUtils.pad(weatherSpec.location.substring(0, 4), 4);
    }
}
