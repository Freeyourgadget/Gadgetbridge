/*  Copyright (C) 2020-2021 Lesur Frederic

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
package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

public class HPlusWeatherCode {
    // Weather code from https://github.com/heweather/WeatherIcon
    public static final int SUNNY = 100;
    public static final int CLOUDY = 101;
    public static final int FEW_CLOUDS = 102;
    public static final int PARTLY_CLOUDY = 103;
    public static final int OVERCAST = 104;
    public static final int CLEAR = 150;
    public static final int PARTLY_CLOUDY_NIGHT = 153;
    public static final int OVERCAST_NIGHT = 154;
    public static final int SHOWER_RAIN = 300;
    public static final int HEAVY_SHOWER_RAIN = 301;
    public static final int THUNDERSHOWER = 302;
    public static final int HEAVY_THUNDERSTORM = 303;
    public static final int THUNDERSHOWER_WITH_HAIL = 304;
    public static final int LIGHT_RAIN = 305;
    public static final int MODERATE_RAIN = 306;
    public static final int HEAVY_RAIN = 307;
    public static final int EXTREME_RAIN = 308;
    public static final int DRIZZLE_RAIN = 309;
    public static final int STORM = 310;
    public static final int HEAVY_STORM = 311;
    public static final int SEVERE_STORM = 312;
    public static final int FREEZING_RAIN = 313;
    public static final int LIGHT_TO_MODERATE_RAIN = 314;
    public static final int MODERATE_TO_HEAVY_RAIN = 315;
    public static final int HEAVY_RAIN_TO_STORM = 316;
    public static final int STORM_TO_HEAVY_STORM = 317;
    public static final int HEAVY_TO_SEVERE_STORM = 318;
    public static final int RAIN = 399;
    public static final int SHOWER_RAIN_NIGHT = 350;
    public static final int HEAVY_SHOWER_RAIN_NIGHT = 351;
    public static final int LIGHT_SNOW = 400;
    public static final int MODERATE_SNOW = 401;
    public static final int HEAVY_SNOW = 402;
    public static final int SNOWSTORM = 403;
    public static final int SLEET = 404;
    public static final int RAIN_AND_SNOW = 405;
    public static final int SHOWER_SNOW = 406;
    public static final int SNOW_FLURRY = 407;
    public static final int LIGHT_TO_MODERATE_SNOW = 408;
    public static final int MODERATE_TO_HEAVY_SNOW = 409;
    public static final int HEAVY_SNOW_TO_SNOWSTORM = 410;
    public static final int SNOW = 499;
    public static final int SHOWER_SNOW_NIGHT = 456;
    public static final int SNOW_FLURRY_NIGHT = 457;
    public static final int MIST = 500;
    public static final int FOGGY = 501;
    public static final int HAZE = 502;
    public static final int SAND = 503;
    public static final int DUST = 504;
    public static final int DUSTSTORM = 507;
    public static final int SANDSTORM = 508;
    public static final int DENSE_FOG = 509;
    public static final int STRONG_FOG = 510;
    public static final int MODERATE_HAZE = 511;
    public static final int HEAVY_HAZE = 512;
    public static final int SEVERE_HAZE = 513;
    public static final int HEAVY_FOG = 514;
    public static final int EXTRA_HEAVY_FOG = 515;
    public static final int HOT = 900;
    public static final int COLD = 901;
    public static final int UNKNOWN = 999;

    public static final int mapOpenWeatherConditionToHPlusCondition(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            //Group 2xx: Thunderstorm
            case 200:  //thunderstorm with light rain:  //11d
                return HPlusWeatherCode.STORM;
            case 201:  //thunderstorm with rain:  //11d
            case 202:  //thunderstorm with heavy rain:  //11d
                return HPlusWeatherCode.HEAVY_RAIN_TO_STORM;
            case 210:  //light thunderstorm::  //11d
            case 211:  //thunderstorm:  //11d
            case 230:  //thunderstorm with light drizzle:  //11d
            case 231:  //thunderstorm with drizzle:  //11d
            case 232:  //thunderstorm with heavy drizzle:  //11d
            case 212:  //heavy thunderstorm:  //11d
            case 221:  //ragged thunderstorm:  //11d
                return HPlusWeatherCode.HEAVY_THUNDERSTORM;
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
                return HPlusWeatherCode.DRIZZLE_RAIN;
            case 500:  //light rain:  //10d
                return HPlusWeatherCode.LIGHT_RAIN;
            case 501:  //moderate rain:  //10d
                return HPlusWeatherCode.MODERATE_RAIN;
            //Group 5xx: Rain
            case 502:  //heavy intensity rain:  //10d
                return HPlusWeatherCode.HEAVY_RAIN;
            case 503:  //very heavy rain:  //10d
                return HPlusWeatherCode.HEAVY_RAIN_TO_STORM;
            case 504:  //extreme rain:  //10d
                return HPlusWeatherCode.EXTREME_RAIN;
            case 511:  //freezing rain:  //13d
                return HPlusWeatherCode.FREEZING_RAIN;
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
                return HPlusWeatherCode.SHOWER_RAIN;
            case 522:  //heavy intensity shower rain:  //09d
            case 531:  //ragged shower rain:  //09d
                return HPlusWeatherCode.HEAVY_SHOWER_RAIN;
            //Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
                return HPlusWeatherCode.LIGHT_SNOW;
            case 601:  //snow:  //[[file:13d.png]]
                return HPlusWeatherCode.SNOW;
            case 620:  //light shower snow:  //[[file:13d.png]]
                return HPlusWeatherCode.LIGHT_TO_MODERATE_SNOW;
            case 602:  //heavy snow:  //[[file:13d.png]]
                return HPlusWeatherCode.MODERATE_TO_HEAVY_SNOW;
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
                return HPlusWeatherCode.SNOW_FLURRY;
            case 621:  //shower snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return HPlusWeatherCode.SHOWER_SNOW;
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
                return HPlusWeatherCode.RAIN_AND_SNOW;
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
            case 781:  //tornado:  //[[file:50d.png]]
            case 900:  //tornado
                return HPlusWeatherCode.SANDSTORM;
            //Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return HPlusWeatherCode.CLEAR;
            //Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
                return HPlusWeatherCode.FEW_CLOUDS;
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return HPlusWeatherCode.PARTLY_CLOUDY;
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return HPlusWeatherCode.CLOUDY;

            //Group 90x: Extreme
            case 901:  //tropical storm
            case 903:  //cold
            case 904:  //hot
            case 905:  //windy
            case 906:  //hail
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
                return HPlusWeatherCode.SEVERE_HAZE;
            default:
                return HPlusWeatherCode.UNKNOWN;
        }
    }
}
