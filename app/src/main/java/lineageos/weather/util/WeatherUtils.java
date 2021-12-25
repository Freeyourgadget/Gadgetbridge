/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lineageos.weather.util;


import lineageos.providers.WeatherContract;

import java.text.DecimalFormat;

/**
 * Helper class to perform operations and formatting of weather data
 */
public class WeatherUtils {

    /**
     * Converts a temperature expressed in degrees Celsius to degrees Fahrenheit
     * @param celsius temperature in Celsius
     * @return the temperature in degrees Fahrenheit
     */
    public static double celsiusToFahrenheit(double celsius) {
        return ((celsius * (9d/5d)) + 32d);
    }

    /**
     * Converts a temperature expressed in degrees Fahrenheit to degrees Celsius
     * @param fahrenheit temperature in Fahrenheit
     * @return the temperature in degrees Celsius
     */
    public static double fahrenheitToCelsius(double fahrenheit) {
        return  ((fahrenheit - 32d) * (5d/9d));
    }

    /**
     * Returns a string representation of the temperature and unit supplied. The temperature value
     * will be half-even rounded.
     * @param temperature the temperature value
     * @param tempUnit A valid {@link WeatherContract.WeatherColumns.TempUnit}
     * @return A string with the format XX&deg;F or XX&deg;C (where XX is the temperature)
     * depending on the temperature unit that was provided or null if an invalid unit is supplied
     */
    public static String formatTemperature(double temperature, int tempUnit) {
        if (!isValidTempUnit(tempUnit)) return null;
        if (Double.isNaN(temperature)) return "-";

        DecimalFormat noDigitsFormat = new DecimalFormat("0");
        String noDigitsTemp = noDigitsFormat.format(temperature);
        if (noDigitsTemp.equals("-0")) {
            noDigitsTemp = "0";
        }

        StringBuilder formatted = new StringBuilder()
                .append(noDigitsTemp).append("\u00b0");
        if (tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS) {
            formatted.append("C");
        } else if (tempUnit == WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT) {
            formatted.append("F");
        }
        return formatted.toString();
    }

    private static boolean isValidTempUnit(int unit) {
        switch (unit) {
            case WeatherContract.WeatherColumns.TempUnit.CELSIUS:
            case WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT:
                return true;
            default:
                return false;
        }
    }
}
