/*  Copyright (C) 2022-2024 José Rebelo, Noodlez

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
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;


/**
 * An adapter class for weather
 */
public class AsteroidOSWeather {
    /**
     * Provides a day's worth of weather
     */
    public static class Day {
        /**
         * The minimum temp of the day
         */
        public int minTemp;
        /**
         * The maximum temp of the day
         */
        public int maxTemp;
        /**
         * The current OWM weather condition code
         */
        public int condition;

        /**
         * Creates a Day from the forecast given
         * @param forecast A day in the weather forecast
         */
        public Day(WeatherSpec.Daily forecast) {
            minTemp = forecast.minTemp;
            maxTemp = forecast.maxTemp;
            condition = forecast.conditionCode;
        }

        /**
         * Creates a Day from the WeatherSpec given
         * @param spec The weather spec itself
         */
        public Day(WeatherSpec spec) {
            minTemp = spec.todayMinTemp;
            maxTemp = spec.todayMaxTemp;
            condition = spec.currentConditionCode;
        }
    }

    /**
     * The days of the weather
     */
    public ArrayList<Day> days = new ArrayList<>();
    /**
     * The city name of the weather
     */
    public String cityName;


    /**
     * Creates an AsteroidOSWeather from the WeatherSpec given
     * @param spec The WeatherSpec given to the device support class
     */
    public AsteroidOSWeather(WeatherSpec spec) {
        cityName = spec.location;
        days.add(new Day(spec));
        for (int i = 1; i < spec.forecasts.size(); i++) {
            days.add(new Day(spec.forecasts.get(i - 1)));
        }
    }

    /**
     * Returns a byte array of the city name
     * @return a byte array of the city name
     */
    public byte[] getCityName() {
        return cityName.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parses the days' weather conditions and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getWeatherConditions() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.condition >> 8));
            stream.write((byte) (day.condition));
        }
        return stream.toByteArray();
    }

    /**
     * Parses the days' min temps and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getMinTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.minTemp >> 8));
            stream.write((byte) (day.minTemp));
        }
        return stream.toByteArray();
    }

    /**
     * Parses the days' max temps and returns them in a format AsteroidOS can handle
     * @return a byte array to be sent to the device
     */
    public byte[] getMaxTemps() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Day day : days) {
            stream.write((byte) (day.maxTemp >> 8));
            stream.write((byte) (day.maxTemp));
        }
        return stream.toByteArray();
    }
}
