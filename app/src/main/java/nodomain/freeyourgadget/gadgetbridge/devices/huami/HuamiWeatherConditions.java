/*  Copyright (C) 2017-2018 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.huami;


public class HuamiWeatherConditions {
    public static final byte CLEAR_SKY = 0;
    public static final byte SCATTERED_CLOUDS = 1;
    public static final byte CLOUDY = 2;
    public static final byte RAIN_WITH_SUN = 3;
    public static final byte THUNDERSTORM = 4;
    public static final byte HAIL = 5;
    public static final byte RAIN_AND_SNOW = 6;
    public static final byte LIGHT_RAIN = 7;
    public static final byte MEDIUM_RAIN = 8;
    public static final byte HEAVY_RAIN = 9;
    public static final byte EXTREME_RAIN = 10;
    public static final byte SUPER_EXTREME_RAIN = 11;
    public static final byte TORRENTIAL_RAIN = 12;
    public static final byte SNOW_AND_SUN = 13;
    public static final byte LIGHT_SNOW = 14;
    public static final byte MEDIUM_SNOW = 15;
    public static final byte HEAVY_SNOW = 16;
    public static final byte EXTREME_SNOW = 17;
    public static final byte MIST = 18;
    public static final byte DRIZZLE = 19;
    public static final byte WIND_AND_RAIN = 20;
    // 21- various types of rain

    public static byte mapToAmazfitBipWeatherCode(int openWeatherMapCondition) {
        // openweathermap.org conditions:
        // http://openweathermap.org/weather-conditions
        switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
            case 200:  //thunderstorm with light rain:  //11d
            case 201:  //thunderstorm with rain:  //11d
            case 202:  //thunderstorm with heavy rain:  //11d
            case 210:  //light thunderstorm::  //11d
            case 211:  //thunderstorm:  //11d
            case 230:  //thunderstorm with light drizzle:  //11d
            case 231:  //thunderstorm with drizzle:  //11d
            case 232:  //thunderstorm with heavy drizzle:  //11d
            case 212:  //heavy thunderstorm:  //11d
            case 221:  //ragged thunderstorm:  //11d
                return THUNDERSTORM;
//Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 301:  //drizzle:  //09d
            case 302:  //heavy intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 311:  //drizzle rain:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
            case 313:  //shower rain and drizzle:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
            case 321:  //shower drizzle:  //09d
                return DRIZZLE;
//Group 5xx: Rain
            case 500:  //light rain:  //10d
                return LIGHT_RAIN;
            case 501:  //moderate rain:  //10d
                return MEDIUM_RAIN;
            case 502:  //heavy intensity rain:  //10d
                return HEAVY_RAIN;
            case 503:  //very heavy rain:  //10d
                return EXTREME_RAIN;
            case 504:  //extreme rain:  //10d
                return TORRENTIAL_RAIN;
            case 511:  //freezing rain:  //13d
                return MEDIUM_RAIN;
            case 520:  //light intensity shower rain:  //09d
                return LIGHT_RAIN;
            case 521:  //shower rain:  //09d
                return MEDIUM_RAIN;
            case 522:  //heavy intensity shower rain:  //09d
                return HEAVY_RAIN;
            case 531:  //ragged shower rain:  //09d
                return MEDIUM_RAIN;
//Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
                return LIGHT_SNOW;
            case 601:  //snow:  //[[file:13d.png]]
                return MEDIUM_SNOW;
            case 602:  //heavy snow:  //[[file:13d.png]]
                return HEAVY_SNOW;
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
            case 620:  //light shower snow:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return RAIN_AND_SNOW;

//Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
            case 721:  //haze:  //[[file:50d.png]]
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
            case 741:  //fog:  //[[file:50d.png]]
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
            case 762:  //volcanic ash:  //[[file:50d.png]]
            case 771:  //squalls:  //[[file:50d.png]]
                return MIST;
            case 781:  //tornado:  //[[file:50d.png]]
            case 900:  //tornado
                return WIND_AND_RAIN;
//Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return CLEAR_SKY;
//Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return SCATTERED_CLOUDS;
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return CLOUDY;
//Group 90x: Extreme
            case 901:  //tropical storm
                return WIND_AND_RAIN;
            case 903:  //cold
            case 904:  //hot
            case 905:  //windy
                return 0;
            case 906:  //hail
                return HAIL;
//Group 9xx: Additional
            case 951:  //calm
            case 952:  //light breeze
            case 953:  //gentle breeze
            case 954:  //moderate breeze
            case 955:  //fresh breeze
            case 956:  //strong breeze
            case 957:  //high windcase  near gale
            case 958:  //gale
            case 959:  //severe gale
            case 960:  //storm
            case 961:  //violent storm
            case 902:  //hurricane
            case 962:  //hurricane
            default:
                return 0;
        }
    }
}