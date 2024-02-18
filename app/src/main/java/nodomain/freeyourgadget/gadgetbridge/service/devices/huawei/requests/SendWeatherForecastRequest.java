/*  Copyright (C) 2024 Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Weather.WeatherForecastData;

public class SendWeatherForecastRequest extends Request {
    WeatherSpec weatherSpec;

    public SendWeatherForecastRequest(HuaweiSupportProvider support, WeatherSpec weatherSpec) {
        super(support);
        this.serviceId = Weather.id;
        this.commandId = Weather.WeatherForecastData.id;
        this.weatherSpec = weatherSpec;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        // TODO: Weather settings
        int hourlyCount = Math.min(weatherSpec.hourly.size(), 24);
        int dayCount = Math.min(weatherSpec.forecasts.size(), 8);

        ArrayList<WeatherForecastData.TimeData> timeDataArrayList = new ArrayList<>(hourlyCount);
        ArrayList<WeatherForecastData.DayData> dayDataArrayList = new ArrayList<>(dayCount);
        for (int i = 0; i < hourlyCount; i++) {
            WeatherSpec.Hourly hourly = weatherSpec.hourly.get(i);
            WeatherForecastData.TimeData timeData = new WeatherForecastData.TimeData();
            timeData.timestamp = hourly.timestamp;
            timeData.icon = supportProvider.openWeatherMapConditionCodeToHuaweiIcon(hourly.conditionCode);
            timeData.temperature = (byte) (hourly.temp - 273);
            timeDataArrayList.add(timeData);
        }

        // Add today as well
        WeatherForecastData.DayData today = new WeatherForecastData.DayData();
        today.timestamp = weatherSpec.sunRise;
        today.icon = supportProvider.openWeatherMapConditionCodeToHuaweiIcon(weatherSpec.currentConditionCode);
        today.highTemperature = (byte) (weatherSpec.todayMaxTemp - 273);
        today.lowTemperature = (byte) (weatherSpec.todayMinTemp - 273);
        today.sunriseTime = weatherSpec.sunRise;
        today.sunsetTime = weatherSpec.sunSet;
        today.moonRiseTime = weatherSpec.moonRise;
        today.moonSetTime = weatherSpec.moonSet;
        today.moonPhase = (byte) 4; // weatherSpec.moonPhase; // TODO: check
        dayDataArrayList.add(today);

        for (int i = 0; i < dayCount - 1; i++) {
            WeatherSpec.Daily daily = weatherSpec.forecasts.get(i);
            WeatherForecastData.DayData dayData = new WeatherForecastData.DayData();
            dayData.timestamp = weatherSpec.timestamp + (60*60*24 * (i + 1));
            dayData.icon = supportProvider.openWeatherMapConditionCodeToHuaweiIcon(daily.conditionCode);
            dayData.highTemperature = (byte) (daily.maxTemp - 273);
            dayData.lowTemperature = (byte) (daily.minTemp - 273);
            dayData.sunriseTime = daily.sunRise;
            dayData.sunsetTime = daily.sunSet;
            dayData.moonRiseTime = daily.moonRise;
            dayData.moonSetTime = daily.moonSet;
            dayData.moonPhase = (byte) 4; // daily.moonPhase; // TODO: check
            dayDataArrayList.add(dayData);
        }
        try {
            return new WeatherForecastData.Request(
                    this.paramsProvider,
                    timeDataArrayList,
                    dayDataArrayList
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
