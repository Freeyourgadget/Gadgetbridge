/*  Copyright (C) 2017-2024 Andreas Shimokawa, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

public class XiaomiWeatherConditions {
    // Most of these conditions are the same on Huami and ZeppOS (see HuamiWeatherConditions), but
    // some have been renamed and a few were added. Nonetheless, some more detailed conditions,
    // such as drizzle, tornado, and tropical storm, do not have an equivalent
    // These conditions were mapped by sending the current condition and iterating over the condition
    // code.
    // At least the Xiaomi Watch S1 Active is known to crash if the condition cannot be mapped
    // to an icon while shown (above 33), so take that into account when adding new conditions below.
    public static final byte CLEAR_SKY                   = 0;
    public static final byte CLOUDY                      = 1; // may appear with a moon on some models
    public static final byte OVERCAST                    = 2;
    public static final byte SHOWER                      = 3; // 'light rain' (two drops) on MB8P
    public static final byte THUNDERSTORM                = 4;
    public static final byte HAIL                        = 5;
    public static final byte SLEET                       = 6;
    public static final byte LIGHT_RAIN                  = 7; // 'light rain' (two drops) on MB8P
    public static final byte MODERATE_RAIN               = 8; // 'moderate rain' (three drops) on MB8P
    public static final byte HEAVY_RAINFALL              = 9; // 'heavy rain' (four drops) on MB8P
    public static final byte RAINSTORM                   = 10; // 'heavy rain' (four drops) on MB8P
    public static final byte DOWNPOUR                    = 11; // 'heavy rain' (four drops) on MB8P
    public static final byte HEAVY_RAINSTORM             = 12; // 'heavy rain' (four drops) on MB8P
    public static final byte SNOW_SHOWERS                = 13; // 'light snow' (1 flake) on MB8P
    public static final byte LIGHT_SNOW                  = 14; // 'light snow' (1 flake) on MB8P
    public static final byte MODERATE_SNOW               = 15; // 'moderate snow' (1 large and 1 small flake) on MB8P
    public static final byte HEAVY_SNOW                  = 16; // 'heavy snow' (multiple flakes}) on MB8P
    public static final byte BLIZZARD                    = 17; // 'heavy snow' (multiple flakes}) on MB8P
    public static final byte MIST                        = 18;
    public static final byte FREEZING_RAIN               = 19; // 'sleet' on MB8P
    public static final byte SANDSTORM                   = 20; // sandstorm
    public static final byte LIGHT_TO_MODERATE_RAIN      = 21; // 'moderate rain' on MB8P
    public static final byte MODERATE_TO_HEAVY_RAIN      = 22; // 'heavy rain' on MB8P
    public static final byte HEAVY_RAIN_TO_RAINSTORM     = 23; // 'heavy rain' on MB8P
    public static final byte RAINSTORM_TO_DOWNPOUR       = 24; // 'heavy rain' on MB8P
    public static final byte DOWNPOUR_TO_HEAVY_RAINSTORM = 25; // 'heavy rain' on MB8P
    public static final byte LIGHT_TO_MODERATE_SNOW      = 26; // 'moderate snow' on MB8P
    public static final byte MODERATE_TO_HEAVY_SNOW      = 27; // 'heavy snow' on MB8P
    public static final byte HEAVY_SNOW_TO_BLIZZARD      = 28; // 'heavy snow' on MB8P
    public static final byte DUST                        = 29; // 'sandstorm' on MB8P
    public static final byte WINDY                       = 30; // 'sandstorm' on MB8P
    public static final byte STRONG_SANDSTORM            = 31; // 'sandstorm' on MB8P
    public static final byte MODERATE_FOG                = 32; // 'mist' on MB8P
    public static final byte SNOW                        = 33; // 'light snow' on MB8P
    // Please read the comment above before adding new condition codes here

    public static byte convertOwmConditionToXiaomi(int openWeatherMapCondition) {
        // openweathermap.org conditions:
        // http://openweathermap.org/weather-conditions
        switch (openWeatherMapCondition) {
//Group 2xx: Thunderstorm
            case 200:  // thunderstorm with light rain
            case 201:  // thunderstorm with rain
            case 202:  // thunderstorm with heavy rain
            case 210:  // light thunderstorm
            case 211:  // thunderstorm
            case 212:  // heavy thunderstorm
            case 221:  // ragged thunderstorm
            case 230:  // thunderstorm with light drizzle
            case 231:  // thunderstorm with drizzle
            case 232:  // thunderstorm with heavy drizzle
                return THUNDERSTORM;
//Group 3xx: Drizzle
            case 300:  // light intensity drizzle
            case 301:  // drizzle
            case 302:  // heavy intensity drizzle
            case 310:  // light intensity drizzle rain
            case 311:  // drizzle rain
            case 312:  // heavy intensity drizzle rain
            case 313:  // shower rain and drizzle
            case 314:  // heavy shower rain and drizzle
            case 321:  // shower drizzle
                // for lack of a better transcription, drizzle -> light rain
//Group 5xx: Rain
            case 500:  // light rain
                return LIGHT_RAIN;
            case 501:  // moderate rain
                return MODERATE_RAIN;
            case 502:  // heavy intensity rain
                return RAINSTORM;
            case 503:  // very heavy rain
                return DOWNPOUR;
            case 504:  // extreme rain
                return HEAVY_RAINSTORM;
            case 511:  // freezing rain
                return FREEZING_RAIN;
            case 520:  // light intensity shower rain
            case 521:  // shower rain
                return SHOWER;
            case 522:  // heavy intensity shower rain
            case 531:  // ragged shower rain
                return HEAVY_RAINFALL;

//Group 6xx: Snow
            case 600:  // light snow
                return LIGHT_SNOW;
            case 601:  // snow
                return MODERATE_SNOW;
            case 602:  // heavy snow
                return HEAVY_SNOW;
            case 611:  // sleet
            case 612:  // shower sleet
                return SLEET;
            case 615:  // light rain and snow
            case 616:  // rain and snow
                return SNOW_SHOWERS;
            case 620:  // light shower snow
            case 621:  // shower snow
                return HEAVY_SNOW;
            case 622:  // heavy shower snow
                return BLIZZARD;

//Group 7xx: Atmosphere
            case 701:  // mist
            case 711:  // smoke
            case 721:  // haze
            case 731:  // sand/dust whirls
            case 741:  // fog
                return MIST;
            case 751:  // sand
                return SANDSTORM;
            case 761:  // dust
            case 762:  // volcanic ash
            case 771:  // squalls
                return DUST;
            case 781:  // tornado
            case 900:  // tornado
                return WINDY;
//Group 800: Clear
            case 800:  // clear sky
                return CLEAR_SKY;
//Group 80x: Clouds
            case 801:  // few clouds
            case 802:  // scattered clouds
            case 803:  // broken clouds
            case 804:  // overcast clouds
                return OVERCAST;
//Group 90x: Extreme
            case 901:  // tropical storm
                return WINDY;
            case 903:  // cold
            case 904:  // hot
                return CLEAR_SKY;
            case 905:  // windy
                return WINDY;
            case 906:  // hail
                return HAIL;
//Group 9xx: Additional
            case 951:  // calm
            case 952:  // light breeze
            case 953:  // gentle breeze
            case 954:  // moderate breeze
            case 955:  // fresh breeze
                return CLEAR_SKY;

            case 956:  // strong breeze
            case 957:  // high wind/near gale
            case 958:  // gale
            case 959:  // severe gale
            case 960:  // storm
            case 961:  // violent storm
            case 902:  // hurricane
            case 962:  // hurricane
                return WINDY;

            default:
                return CLEAR_SKY;
        }
    }
}