/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

public final class FitWeatherConditions {
    public static final int CLEAR = 0;
    public static final int PARTLY_CLOUDY = 1;
    public static final int MOSTLY_CLOUDY = 2;
    public static final int RAIN = 3;
    public static final int SNOW = 4;
    public static final int WINDY = 5;
    public static final int THUNDERSTORMS = 6;
    public static final int WINTRY_MIX = 7;
    public static final int FOG = 8;
    public static final int HAZY = 11;
    public static final int HAIL = 12;
    public static final int SCATTERED_SHOWERS = 13;
    public static final int SCATTERED_THUNDERSTORMS = 14;
    public static final int UNKNOWN_PRECIPITATION = 15;
    public static final int LIGHT_RAIN = 16;
    public static final int HEAVY_RAIN = 17;
    public static final int LIGHT_SNOW = 18;
    public static final int HEAVY_SNOW = 19;
    public static final int LIGHT_RAIN_SNOW = 20;
    public static final int HEAVY_RAIN_SNOW = 21;
    public static final int CLOUDY = 22;

    public static final int ALERT_SEVERITY_UNKNOWN = 0;
    public static final int ALERT_SEVERITY_WARNING = 1;
    public static final int ALERT_SEVERITY_WATCH = 2;
    public static final int ALERT_SEVERITY_ADVISORY = 3;
    public static final int ALERT_SEVERITY_STATEMENT = 4;

    public static final int ALERT_TYPE_UNSPECIFIED = 0;
    public static final int ALERT_TYPE_TORNADO = 1;
    public static final int ALERT_TYPE_TSUNAMI = 2;
    public static final int ALERT_TYPE_HURRICANE = 3;
    public static final int ALERT_TYPE_EXTREME_WIND = 4;
    public static final int ALERT_TYPE_TYPHOON = 5;
    public static final int ALERT_TYPE_INLAND_HURRICANE = 6;
    public static final int ALERT_TYPE_HURRICANE_FORCE_WIND = 7;
    public static final int ALERT_TYPE_WATERSPOUT = 8;
    public static final int ALERT_TYPE_SEVERE_THUNDERSTORM = 9;
    public static final int ALERT_TYPE_WRECKHOUSE_WINDS = 10;
    public static final int ALERT_TYPE_LES_SUETES_WIND = 11;
    public static final int ALERT_TYPE_AVALANCHE = 12;
    public static final int ALERT_TYPE_FLASH_FLOOD = 13;
    public static final int ALERT_TYPE_TROPICAL_STORM = 14;
    public static final int ALERT_TYPE_INLAND_TROPICAL_STORM = 15;
    public static final int ALERT_TYPE_BLIZZARD = 16;
    public static final int ALERT_TYPE_ICE_STORM = 17;
    public static final int ALERT_TYPE_FREEZING_RAIN = 18;
    public static final int ALERT_TYPE_DEBRIS_FLOW = 19;
    public static final int ALERT_TYPE_FLASH_FREEZE = 20;
    public static final int ALERT_TYPE_DUST_STORM = 21;
    public static final int ALERT_TYPE_HIGH_WIND = 22;
    public static final int ALERT_TYPE_WINTER_STORM = 23;
    public static final int ALERT_TYPE_HEAVY_FREEZING_SPRAY = 24;
    public static final int ALERT_TYPE_EXTREME_COLD = 25;
    public static final int ALERT_TYPE_WIND_CHILL = 26;
    public static final int ALERT_TYPE_COLD_WAVE = 27;
    public static final int ALERT_TYPE_HEAVY_SNOW_ALERT = 28;
    public static final int ALERT_TYPE_LAKE_EFFECT_BLOWING_SNOW = 29;
    public static final int ALERT_TYPE_SNOW_SQUALL = 30;
    public static final int ALERT_TYPE_LAKE_EFFECT_SNOW = 31;
    public static final int ALERT_TYPE_WINTER_WEATHER = 32;
    public static final int ALERT_TYPE_SLEET = 33;
    public static final int ALERT_TYPE_SNOWFALL = 34;
    public static final int ALERT_TYPE_SNOW_AND_BLOWING_SNOW = 35;
    public static final int ALERT_TYPE_BLOWING_SNOW = 36;
    public static final int ALERT_TYPE_SNOW_ALERT = 37;
    public static final int ALERT_TYPE_ARCTIC_OUTFLOW = 38;
    public static final int ALERT_TYPE_FREEZING_DRIZZLE = 39;
    public static final int ALERT_TYPE_STORM = 40;
    public static final int ALERT_TYPE_STORM_SURGE = 41;
    public static final int ALERT_TYPE_RAINFALL = 42;
    public static final int ALERT_TYPE_AREAL_FLOOD = 43;
    public static final int ALERT_TYPE_COASTAL_FLOOD = 44;
    public static final int ALERT_TYPE_LAKESHORE_FLOOD = 45;
    public static final int ALERT_TYPE_EXCESSIVE_HEAT = 46;
    public static final int ALERT_TYPE_HEAT = 47;
    public static final int ALERT_TYPE_WEATHER = 48;
    public static final int ALERT_TYPE_HIGH_HEAT_AND_HUMIDITY = 49;
    public static final int ALERT_TYPE_HUMIDEX_AND_HEALTH = 50;
    public static final int ALERT_TYPE_HUMIDEX = 51;
    public static final int ALERT_TYPE_GALE = 52;
    public static final int ALERT_TYPE_FREEZING_SPRAY = 53;
    public static final int ALERT_TYPE_SPECIAL_MARINE = 54;
    public static final int ALERT_TYPE_SQUALL = 55;
    public static final int ALERT_TYPE_STRONG_WIND = 56;
    public static final int ALERT_TYPE_LAKE_WIND = 57;
    public static final int ALERT_TYPE_MARINE_WEATHER = 58;
    public static final int ALERT_TYPE_WIND = 59;
    public static final int ALERT_TYPE_SMALL_CRAFT_HAZARDOUS_SEAS = 60;
    public static final int ALERT_TYPE_HAZARDOUS_SEAS = 61;
    public static final int ALERT_TYPE_SMALL_CRAFT = 62;
    public static final int ALERT_TYPE_SMALL_CRAFT_WINDS = 63;
    public static final int ALERT_TYPE_SMALL_CRAFT_ROUGH_BAR = 64;
    public static final int ALERT_TYPE_HIGH_WATER_LEVEL = 65;
    public static final int ALERT_TYPE_ASHFALL = 66;
    public static final int ALERT_TYPE_FREEZING_FOG = 67;
    public static final int ALERT_TYPE_DENSE_FOG = 68;
    public static final int ALERT_TYPE_DENSE_SMOKE = 69;
    public static final int ALERT_TYPE_BLOWING_DUST = 70;
    public static final int ALERT_TYPE_HARD_FREEZE = 71;
    public static final int ALERT_TYPE_FREEZE = 72;
    public static final int ALERT_TYPE_FROST = 73;
    public static final int ALERT_TYPE_FIRE_WEATHER = 74;
    public static final int ALERT_TYPE_FLOOD = 75;
    public static final int ALERT_TYPE_RIP_TIDE = 76;
    public static final int ALERT_TYPE_HIGH_SURF = 77;
    public static final int ALERT_TYPE_SMOG = 78;
    public static final int ALERT_TYPE_AIR_QUALITY = 79;
    public static final int ALERT_TYPE_BRISK_WIND = 80;
    public static final int ALERT_TYPE_AIR_STAGNATION = 81;
    public static final int ALERT_TYPE_LOW_WATER = 82;
    public static final int ALERT_TYPE_HYDROLOGICAL = 83;
    public static final int ALERT_TYPE_SPECIAL_WEATHER = 84;

    public static int openWeatherCodeToFitWeatherStatus(int openWeatherCode) {
        switch (openWeatherCode) {
            case 800:
                return CLEAR;
            case 801:
            case 802:
                return PARTLY_CLOUDY;
            case 803:
                return MOSTLY_CLOUDY;
            case 804:
                return CLOUDY;
            case 701:
            case 721:
                return HAZY;
            case 741:
                return FOG;
            case 771:
            case 781:
                return WINDY;
            case 615:
                return LIGHT_RAIN_SNOW;
            case 616:
                return HEAVY_RAIN_SNOW;
            case 611:
            case 612:
            case 613:
                return WINTRY_MIX;
            case 500:
            case 520:
            case 521:
            case 300:
            case 310:
            case 313:
                return LIGHT_RAIN;
            case 501:
            case 531:
            case 301:
            case 311:
                return RAIN;
            case 502:
            case 503:
            case 504:
            case 522:
            case 302:
            case 312:
            case 314:
                return HEAVY_RAIN;
            case 321:
                return SCATTERED_SHOWERS;
            case 511:
                return UNKNOWN_PRECIPITATION;
            case 200:
            case 201:
            case 202:
            case 210:
            case 211:
            case 212:
            case 230:
            case 231:
            case 232:
                return THUNDERSTORMS;
            case 221:
                return SCATTERED_THUNDERSTORMS;
            case 600:
                return LIGHT_SNOW;
            case 601:
                return SNOW;
            case 602:
                return HEAVY_SNOW;
            default:
                throw new IllegalArgumentException("Unknown weather code " + openWeatherCode);
        }
    }
}
