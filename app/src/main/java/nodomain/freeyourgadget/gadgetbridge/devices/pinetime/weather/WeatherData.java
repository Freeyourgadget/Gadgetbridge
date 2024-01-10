/*  Copyright (C) 2022-2024 Andreas Shimokawa, FintasticMan, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather;

/**
 * Implemented based on and other material:
 * https://en.wikipedia.org/wiki/METAR
 * https://www.weather.gov/jetstream/obscurationtypes
 * http://www.faraim.org/aim/aim-4-03-14-493.html
 */
public class WeatherData {
    /**
     * OpenWeather's condition codes are a bit annoying,
     * they've mixed precipitation with other weather events.
     * <p>
     * Even if they're absolutely not mutually exclusive.
     * <p>
     * So, this function only returns PrecipitationType
     */
    public static final PrecipitationType mapOpenWeatherConditionToPineTimePrecipitation(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            // Group 2xx: Thunderstorm
            case 200:  // Thunderstorm with light rain
            case 201:  // Thunderstorm with rain
            case 202:  // Thunderstorm with heavy rain
            case 210:  // Light thunderstorm
            case 211:  // Thunderstorm
            case 230:  // Thunderstorm with light drizzle
            case 231:  // Thunderstorm with drizzle
            case 232:  // Thunderstorm with heavy drizzle
            case 212:  // Heavy thunderstorm
            case 221:  // Ragged thunderstorm
                return PrecipitationType.Rain;
            // Group 3xx: Drizzle
            case 300:  // Light intensity drizzle
            case 301:  // Drizzle
            case 302:  // Heavy intensity drizzle
            case 310:  // Light intensity drizzle rain
            case 311:  // Drizzle rain
            case 312:  // Heavy intensity drizzle rain
            case 313:  // Shower rain and drizzle
            case 314:  // Heavy shower rain and drizzle
            case 321:  // Shower drizzle
                return PrecipitationType.Drizzle;
            // Group 5xx: Rain
            case 500:  // Light rain
            case 501:  // Moderate rain
            case 502:  // Heavy intensity rain
            case 503:  // Very heavy rain
            case 504:  // Extreme rain
                return PrecipitationType.Rain;
            case 511:  // Freezing rain
                return PrecipitationType.FreezingRain;
            case 520:  // Light intensity shower rain
            case 521:  // Shower rain
            case 522:  // Heavy intensity shower rain
            case 531:  // Ragged shower rain
                return PrecipitationType.Rain;
            // Group 6xx: Snow
            case 600:  // Light snow
            case 601:  // Snow
            case 620:  // Light shower snow
            case 602:  // Heavy snow
                return PrecipitationType.Snow;
            case 611:  // Sleet
            case 612:  // Shower sleet
                return PrecipitationType.Sleet;
            case 621:  // Shower snow
            case 622:  // Heavy shower snow
                return PrecipitationType.Snow;
            case 615:  // Light rain and snow
            case 616:  // Rain and snow
                return PrecipitationType.Sleet;
            // Group 7xx: Atmosphere
            case 701:  // Mist
            case 711:  // Smoke
            case 721:  // Haze
            case 731:  // Sandcase/dust whirls
            case 741:  // Fog
            case 751:  // Sand
            case 761:  // Dust
            case 762:  // Volcanic ash
                return PrecipitationType.Ash;
            case 771:  // Squalls
            case 781:  // Tornado
            case 900:  // Tornado
                // Group 800: Clear
            case 800:  // Clear sky
                return PrecipitationType.None;
            // Group 80x: Clouds
            case 801:  // Few clouds
            case 802:  // Scattered clouds
            case 803:  // Broken clouds
            case 804:  // Overcast clouds
                // Group 90x: Extreme
            case 901:  // Tropical storm
            case 903:  // Cold
            case 904:  // Hot
            case 905:  // Windy
            case 906:  // Hail
                return PrecipitationType.Hail;
            // Group 9xx: Additional
            case 951:  // Calm
            case 952:  // Light breeze
            case 953:  // Gentle breeze
            case 954:  // Moderate breeze
            case 955:  // Fresh breeze
            case 956:  // Strong breeze
            case 957:  // High windcase  near gale
            case 958:  // Gale
            case 959:  // Severe gale
            case 960:  // Storm
            case 961:  // Violent storm
            case 902:  // Hurricane
            case 962:  // Hurricane
            default:
                return PrecipitationType.Length;
        }
    }

    ;

    /**
     * OpenWeather's condition codes are a bit annoying,
     * they've mixed obscuration with other weather events.
     *
     * <p>
     * Even if they're absolutely not mutually exclusive.
     * <p>
     * So, this function only returns ObscurationType
     */
    public static final ObscurationType mapOpenWeatherConditionToPineTimeObscuration(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            // Group 2xx: Thunderstorm
            case 200:  // Thunderstorm with light rain
            case 201:  // Thunderstorm with rain
            case 202:  // Thunderstorm with heavy rain
            case 210:  // Light thunderstorm
            case 211:  // Thunderstorm
            case 230:  // Thunderstorm with light drizzle
            case 231:  // Thunderstorm with drizzle
            case 232:  // Thunderstorm with heavy drizzle
            case 212:  // Heavy thunderstorm
            case 221:  // Ragged thunderstorm
                return ObscurationType.Precipitation;
            // Group 3xx: Drizzle
            case 300:  // Light intensity drizzle
            case 301:  // Drizzle
            case 302:  // Heavy intensity drizzle
            case 310:  // Light intensity drizzle rain
            case 311:  // Drizzle rain
            case 312:  // Heavy intensity drizzle rain
            case 313:  // Shower rain and drizzle
            case 314:  // Heavy shower rain and drizzle
            case 321:  // Shower drizzle
                return ObscurationType.Precipitation;
            // Group 5xx: Rain
            case 500:  // Light rain
            case 501:  // Moderate rain
            case 502:  // Heavy intensity rain
            case 503:  // Very heavy rain
            case 504:  // Extreme rain
            case 511:  // Freezing rain
            case 520:  // Light intensity shower rain
            case 521:  // Shower rain
            case 522:  // Heavy intensity shower rain
            case 531:  // Ragged shower rain
                return ObscurationType.Precipitation;
            // Group 6xx: Snow
            case 600:  // Light snow
            case 601:  // Snow
            case 620:  // Light shower snow
            case 602:  // Heavy snow
            case 611:  // Sleet
            case 612:  // Shower sleet
            case 621:  // Shower snow
            case 622:  // Heavy shower snow
            case 615:  // Light rain and snow
            case 616:  // Rain and snow
                return ObscurationType.Precipitation;
            // Group 7xx: Atmosphere
            case 701:  // Mist
                return ObscurationType.Mist;
            case 711:  // Smoke
                return ObscurationType.Smoke;
            case 721:  // Haze
                return ObscurationType.Haze;
            case 731:  // Sandcase/dust whirls
                return ObscurationType.Sand;
            case 741:  // Fog
                return ObscurationType.Fog;
            case 751:  // Sand
                return ObscurationType.Sand;
            case 761:  // Dust
                return ObscurationType.Dust;
            case 762:  // Volcanic ash
                return ObscurationType.Ash;
            case 771:  // Squalls
                return ObscurationType.Length;
            case 781:  // Tornado
            case 900:  // Tornado
                return ObscurationType.Precipitation;
            // Group 800: Clear
            case 800:  // Clear sky
                return ObscurationType.Length;
            // Group 80x: Clouds
            case 801:  // Few clouds
            case 802:  // Scattered clouds
            case 803:  // Broken clouds
            case 804:  // Overcast clouds
                return ObscurationType.Length;
            // Group 90x: Extreme
            case 901:  // Tropical storm
                return ObscurationType.Precipitation;
            case 903:  // Cold
            case 904:  // Hot
                return ObscurationType.Length;
            case 905:  // Windy
            case 906:  // Hail
                return ObscurationType.Precipitation;
            // Group 9xx: Additional
            case 951:  // Calm
            case 952:  // Light breeze
            case 953:  // Gentle breeze
            case 954:  // Moderate breeze
            case 955:  // Fresh breeze
            case 956:  // Strong breeze
            case 957:  // High windcase near gale
            case 958:  // Gale
            case 959:  // Severe gale
                return ObscurationType.Length;
            case 960:  // Storm
            case 961:  // Violent storm
                return ObscurationType.Precipitation;
            case 902:  // Hurricane
                return ObscurationType.Precipitation;
            case 962:  // Hurricane
                return ObscurationType.Precipitation;
            default:
                return ObscurationType.Length;
        }
    }

    ;

    /**
     * OpenWeather's condition codes are a bit annoying,
     * they've mixed precipitation with other weather events.
     * <p>
     * Even if they're absolutely not mutually exclusive.
     *
     * <p>
     * So, this function only returns SpecialType
     */
    public static final SpecialType mapOpenWeatherConditionToPineTimeSpecial(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            // Group 2xx: Thunderstorm
            case 200:  // Thunderstorm with light rain
            case 201:  // Thunderstorm with rain
            case 202:  // Thunderstorm with heavy rain
            case 210:  // Light thunderstorm
            case 211:  // Thunderstorm
            case 230:  // Thunderstorm with light drizzle
            case 231:  // Thunderstorm with drizzle
            case 232:  // Thunderstorm with heavy drizzle
            case 212:  // Heavy thunderstorm
            case 221:  // Ragged thunderstorm
                // Group 3xx: Drizzle
            case 300:  // Light intensity drizzle
            case 301:  // Drizzle
            case 302:  // Heavy intensity drizzle
            case 310:  // Light intensity drizzle rain
            case 311:  // Drizzle rain
            case 312:  // Heavy intensity drizzle rain
            case 313:  // Shower rain and drizzle
            case 314:  // Heavy shower rain and drizzle
            case 321:  // Shower drizzle
                // Group 5xx: Rain
            case 500:  // Light rain
            case 501:  // Moderate rain
            case 502:  // Heavy intensity rain
            case 503:  // Very heavy rain
            case 504:  // Extreme rain
            case 511:  // Freezing rain
            case 520:  // Light intensity shower rain
            case 521:  // Shower rain
            case 522:  // Heavy intensity shower rain
            case 531:  // Ragged shower rain
                // Group 6xx: Snow
            case 600:  // Light snow
            case 601:  // Snow
            case 620:  // Light shower snow
            case 602:  // Heavy snow
            case 611:  // Sleet
            case 612:  // Shower sleet
            case 621:  // Shower snow
            case 622:  // Heavy shower snow
            case 615:  // Light rain and snow
            case 616:  // Rain and snow
                // Group 7xx: Atmosphere
            case 701:  // Mist
            case 711:  // Smoke
            case 721:  // Haze
            case 731:  // Sandcase/dust whirls
            case 741:  // Fog
            case 751:  // Sand
            case 761:  // Dust
            case 762:  // Volcanic ash
                return SpecialType.Length;
            case 771:  // Squalls
                return SpecialType.Squall;
            case 781:  // Tornado
            case 900:  // Tornado
                // Group 800: Clear
            case 800:  // Clear sky
                // Group 80x: Clouds
            case 801:  // Few clouds
            case 802:  // Scattered clouds
            case 803:  // Broken clouds
            case 804:  // Overcast clouds
                // Group 90x: Extreme
            case 901:  // Tropical storm
            case 903:  // Cold
                return SpecialType.Length;
            case 904:  // Hot
                return SpecialType.Fire;
            case 905:  // Windy
            case 906:  // Hail
                // Group 9xx: Additional
            case 951:  // Calm
            case 952:  // Light breeze
            case 953:  // Gentle breeze
            case 954:  // Moderate breeze
            case 955:  // Fresh breeze
            case 956:  // Strong breeze
            case 957:  // High windcase near gale
            case 958:  // Gale
            case 959:  // Severe gale
            case 960:  // Storm
            case 961:  // Violent storm
            case 902:  // Hurricane
            case 962:  // Hurricane
            default:
                return SpecialType.Length;
        }
    }

    ;

    /**
     * OpenWeather's condition codes are a bit annoying,
     * they've mixed precipitation with other weather events.
     * <p>
     * Even if they're absolutely not mutually exclusive.
     *
     * <p>
     * So, this function only returns cloudyness 0-100%
     */
    public static final int mapOpenWeatherConditionToCloudCover(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            // Group 2xx: Thunderstorm
            case 200:  // Thunderstorm with light rain
            case 201:  // Thunderstorm with rain
            case 202:  // Thunderstorm with heavy rain
            case 210:  // Light thunderstorm
            case 211:  // Thunderstorm
            case 230:  // Thunderstorm with light drizzle
            case 231:  // Thunderstorm with drizzle
            case 232:  // Thunderstorm with heavy drizzle
            case 212:  // Heavy thunderstorm
            case 221:  // Ragged thunderstorm
                return 100;
            // Group 3xx: Drizzle
            case 300:  // Light intensity drizzle
            case 301:  // Drizzle
            case 302:  // Heavy intensity drizzle
            case 310:  // Light intensity drizzle rain
            case 311:  // Drizzle rain
            case 312:  // Heavy intensity drizzle rain
            case 313:  // Shower rain and drizzle
            case 314:  // Heavy shower rain and drizzle
            case 321:  // Shower drizzle
                return 75;
            // Group 5xx: Rain
            case 500:  // Light rain
            case 501:  // Moderate rain
            case 502:  // Heavy intensity rain
            case 503:  // Very heavy rain
            case 504:  // Extreme rain
            case 511:  // Freezing rain
            case 520:  // Light intensity shower rain
            case 521:  // Shower rain
            case 522:  // Heavy intensity shower rain
            case 531:  // Ragged shower rain
                return 75;
            // Group 6xx: Snow
            case 600:  // Light snow
            case 601:  // Snow
            case 620:  // Light shower snow
            case 602:  // Heavy snow
            case 611:  // Sleet
            case 612:  // Shower sleet
            case 621:  // Shower snow
            case 622:  // Heavy shower snow
            case 615:  // Light rain and snow
            case 616:  // Rain and snow
                return 75;
            // Group 7xx: Atmosphere
            case 701:  // Mist
            case 711:  // Smoke
            case 721:  // Haze
            case 731:  // Sandcase/dust whirls
            case 741:  // Fog
            case 751:  // Sand
            case 761:  // Dust
                return -1;
            case 762:  // Volcanic ash
                return 100;
            case 771:  // Squalls
            case 781:  // Tornado
            case 900:  // Tornado
                // Group 800: Clouds & Clear
            case 800:  // Clear sky
                return 0;
            case 801:  // Few clouds
                return 25;
            case 802:  // Scattered clouds
                return 50;
            case 803:  // Broken clouds
                return 75;
            case 804:  // Overcast clouds
                return 100;
            // Group 90x: Extreme
            case 901:  // Tropical storm
            case 903:  // Cold
            case 904:  // Hot
            case 905:  // Windy
            case 906:  // Hail
                return 75;
            // Group 9xx: Additional
            case 951:  // Calm
            case 952:  // Light breeze
            case 953:  // Gentle breeze
            case 954:  // Moderate breeze
            case 955:  // Fresh breeze
            case 956:  // Strong breeze
            case 957:  // High windcase near gale
            case 958:  // Gale
            case 959:  // Severe gale
            case 960:  // Storm
            case 961:  // Violent storm
            case 902:  // Hurricane
            case 962:  // Hurricane
            default:
                return -1;
        }
    }

    ;

    /**
     * List of weather condition codes used to determine display
     * https://openweathermap.org/weather-conditions
     */
    public static final ConditionType mapOpenWeatherConditionToPineTimeCondition(int openWeatherMapCondition) {
        switch (openWeatherMapCondition) {
            // Group 2xx: Thunderstorm
            case 200:  // Thunderstorm with light rain
            case 201:  // Thunderstorm with rain
            case 202:  // Thunderstorm with heavy rain
            case 210:  // Light thunderstorm
            case 211:  // Thunderstorm
            case 212:  // Heavy thunderstorm
            case 221:  // Ragged thunderstorm
            case 230:  // Thunderstorm with light drizzle
            case 231:  // Thunderstorm with drizzle
            case 232:  // Thunderstorm with heavy drizzle
                return ConditionType.Thunderstorm;
            // Group 3xx: Drizzle
            case 300:  // Light intensity drizzle
            case 301:  // Drizzle
            case 302:  // Heavy intensity drizzle
            case 310:  // Light intensity drizzle rain
            case 311:  // Drizzle rain
            case 312:  // Heavy intensity drizzle rain
            case 313:  // Shower rain and drizzle
            case 314:  // Heavy shower rain and drizzle
            case 321:  // Shower drizzle
                return ConditionType.CloudsAndRain;
            // Group 5xx: Rain
            case 500:  // Light rain
            case 501:  // Moderate rain
            case 502:  // Heavy intensity rain
            case 503:  // Very heavy rain
            case 504:  // Extreme rain
                return ConditionType.Rain;
            case 511:  // Freezing rain
                return ConditionType.Snow;
            case 520:  // Light intensity shower rain
            case 521:  // Shower rain
            case 522:  // Heavy intensity shower rain
            case 531:  // Ragged shower rain
                return ConditionType.CloudsAndRain;
            // Group 6xx: Snow
            case 600:  // Light snow
            case 601:  // Snow
            case 602:  // Heavy snow
            case 611:  // Sleet
            case 612:  // Light shower sleet
            case 613:  // Shower sleet
            case 615:  // Light rain and snow
            case 616:  // Rain and snow
            case 620:  // Light shower snow
            case 621:  // Shower snow
            case 622:  // Heavy shower snow
                return ConditionType.Snow;
            // Group 7xx: Atmosphere
            case 701:  // Mist
            case 711:  // Smoke
            case 721:  // Haze
            case 731:  // Sandcase/dust whirls
            case 741:  // Fog
            case 751:  // Sand
            case 761:  // Dust
            case 762:  // Volcanic ash
            case 771:  // Squalls
            case 781:  // Tornado
                return ConditionType.Mist;
            // Group 800: Clear
            case 800:  // Clear sky
                return ConditionType.ClearSky;
            // Group 80x: Clouds
            case 801:  // Few clouds
                return ConditionType.FewClouds;
            case 802:  // Scattered clouds
                return ConditionType.Clouds;
            case 803:  // Broken clouds
            case 804:  // Overcast clouds
                return ConditionType.HeavyClouds;
            default:
                return ConditionType.Length;
        }
    }

    ;

    /**
     * Visibility obscuration types
     */
    public enum ObscurationType {
        /**
         * No obscuration
         */
        None(0),
        /**
         * Water particles suspended in the air; low visibility; does not fall
         */
        Fog(1),
        /**
         * Tiny, dry particles in the air; invisible to the eye; opalescent
         */
        Haze(2),
        /**
         * Small fire-created particles suspended in the air
         */
        Smoke(3),
        /**
         * Fine rock powder, from for example volcanoes
         */
        Ash(4),
        /**
         * Fine particles of earth suspended in the air by the wind
         */
        Dust(5),
        /**
         * Fine particles of sand suspended in the air by the wind
         */
        Sand(6),
        /**
         * Water particles suspended in the air; low-ish visibility; temperature is near dewpoint
         */
        Mist(7),
        /**
         * This is special in the sense that the thing falling down is doing the obscuration
         */
        Precipitation(8),
        Length(9);

        public final int value;

        ObscurationType(int value) {
            this.value = value;
        }
    }

    /**
     * Types of precipitation
     */
    public enum PrecipitationType {
        /**
         * No precipitation
         * <p>
         * Theoretically we could just _not_ send the event, but then
         * how do we differentiate between no precipitation and
         * no information about precipitation
         */
        None(0),
        /**
         * Drops larger than a drizzle; also widely separated drizzle
         */
        Rain(1),
        /**
         * Fairly uniform rain consisting of fine drops
         */
        Drizzle(2),
        /**
         * Rain that freezes upon contact with objects and ground
         */
        FreezingRain(3),
        /**
         * Rain + hail; ice pellets; small translucent frozen raindrops
         */
        Sleet(4),
        /**
         * Larger ice pellets; falling separately or in irregular clumps
         */
        Hail(5),
        /**
         * Hail with smaller grains of ice; mini-snowballs
         */
        SmallHail(6),
        /**
         * Snow...
         */
        Snow(7),
        /**
         * Frozen drizzle; very small snow crystals
         */
        SnowGrains(8),
        /**
         * Needles; columns or plates of ice. Sometimes described as "diamond dust". In very cold regions
         */
        IceCrystals(9),
        /**
         * It's raining down ash, e.g. from a volcano
         */
        Ash(10),
        Length(11);

        public final int value;

        PrecipitationType(int value) {
            this.value = value;
        }
    }

    /**
     * These are special events that can "enhance" the "experience" of existing weather events
     */
    public enum SpecialType {
        /**
         * Strong wind with a sudden onset that lasts at least a minute
         */
        Squall(0),
        /**
         * Series of waves in a water body caused by the displacement of a large volume of water
         */
        Tsunami(1),
        /**
         * Violent; rotating column of air
         */
        Tornado(2),
        /**
         * Unplanned; unwanted; uncontrolled fire in an area
         */
        Fire(3),
        /**
         * Thunder and/or lightning
         */
        Thunder(4),
        Length(5);
        public final int value;

        SpecialType(int value) {
            this.value = value;
        }
    }

    /**
     * List of weather condition codes used to determine display
     */
    public enum ConditionType {
        /**
         * Clear sky
         */
        ClearSky(0),
        /**
         * Few clouds
         */
        FewClouds(1),
        /**
         * Scattered clouds
         */
        Clouds(2),
        /**
         * Broken/heavy clouds
         */
        HeavyClouds(3),
        /**
         * Shower rain
         */
        CloudsAndRain(4),
        /**
         * Rain
         */
        Rain(5),
        /**
         * Thunderstorm
         */
        Thunderstorm(6),
        /**
         * Snow
         */
        Snow(7),
        /**
         * Mist
         */
        Mist(8),
        Length(9);
        public final byte value;

        ConditionType(int value) { this.value = (byte) value; }
    }

    /**
     * These are used for weather timeline manipulation
     * that isn't just adding to the stack of weather events
     */
    public enum ControlCodes {
        /**
         * How much is stored already
         */
        GetLength(0),
        /**
         * This wipes the entire timeline
         */
        DelTimeline(1),
        /**
         * There's a currently valid timeline event with the given type
         */
        HasValidEvent(3),
        Length(4);

        public final int value;

        ControlCodes(int value) {
            this.value = value;
        }
    }

    /**
     * Events have types
     * then they're easier to parse after sending them over the air
     */
    public enum EventType {
        /**
         * @see WeatherData.Obscuration
         */
        Obscuration(0),
        /**
         * @see WeatherData.Precipitation
         */
        Precipitation(1),
        /**
         * @see WeatherData.Wind
         */
        Wind(2),
        /**
         * @see WeatherData.Temperature
         */
        Temperature(3),
        /**
         * @see WeatherData.AirQuality
         */
        AirQuality(4),
        /**
         * @see WeatherData.Special
         */
        Special(5),
        /**
         * @see WeatherData.Pressure
         */
        Pressure(6),
        /**
         * @see WeatherData.Location
         */
        Location(7),
        /**
         * @see WeatherData.Clouds
         */
        Clouds(8),
        /**
         * @see WeatherData.Humidity
         */
        Humidity(9),
        Length(10);

        public final int value;

        EventType(int value) {
            this.value = value;
        }
    }

    ;

    /**
     * Valid event query
     */
    static public class ValidEventQuery {
        ControlCodes code = ControlCodes.HasValidEvent;
        EventType eventType;
    }

    ;

    /**
     * The header used for further parsing
     */
    static public class TimelineHeader {
        /**
         * UNIX timestamp
         */
        long timestamp;
        /**
         * Time in seconds until the event expires
         * <p>
         * 32 bits ought to be enough for everyone
         * <p>
         * If there's a newer event of the same type then it overrides this one, even if it hasn't expired
         */
        int expires;
        /**
         * What type of weather-related event
         */
        EventType eventType;
    }

    /**
     * Specifies how cloudiness is stored
     */
    static public class Clouds extends TimelineHeader {
        /**
         * Cloud coverage in percentage, 0-100%
         */
        byte amount;
    }

    /**
     * Specifies how obscuration is stored
     */
    static public class Obscuration extends TimelineHeader {
        /**
         * Type
         */
        ObscurationType type;
        /**
         * Visibility distance in meters (0-65535)
         */
        int amount;
    }

    /**
     * Specifies how precipitation is stored
     */
    static public class Precipitation extends TimelineHeader {
        /**
         * Type
         */
        PrecipitationType type;
        /**
         * How much is it going to rain? In millimeters (0-255)
         */
        int amount;
    }

    /**
     * How wind speed is stored
     * <p>
     * In order to represent bursts of wind instead of constant wind,
     * you have minimum and maximum speeds.
     * <p>
     * As direction can fluctuate wildly and some watchfaces might wish to display it nicely,
     * we're following the aerospace industry weather report option of specifying a range.
     */
    static public class Wind extends TimelineHeader {
        /**
         * Meters per second (0-255)
         */
        byte speedMin;
        /**
         * Meters per second (0-255)
         */
        byte speedMax;
        /**
         * Unitless direction between 0-255; approximately 1 unit per 0.71 degrees
         */
        byte directionMin;
        /**
         * Unitless direction between 0-255; approximately 1 unit per 0.71 degrees
         */
        byte directionMax;
    }

    /**
     * How temperature is stored
     * <p>
     * As it's annoying to figure out the dewpoint on the watch,
     * please send it from the companion
     * <p>
     * We don't do floats, microdegrees are not useful. Make sure to multiply.
     */
    static public class Temperature extends TimelineHeader {
        /**
         * Temperature °C but multiplied by 100 (e.g. -12.50°C becomes -1250, 0-65535)
         */
        short temperature;
        /**
         * Dewpoint °C but multiplied by 100 (e.g. -12.50°C becomes -1250, 0-65535)
         */
        short dewPoint;
    }

    /**
     * How location info is stored
     * <p>
     * This can be mostly static with long expiration,
     * as it usually is, but it could change during a trip for ex.
     * so we allow changing it dynamically.
     * <p>
     * Location info can be for some kind of map watchface
     * or daylight calculations, should those be required.
     */
    static public class Location extends TimelineHeader {
        /**
         * Location name
         */
        String location;
        /**
         * Altitude relative to sea level in meters (0-65535)
         */
        short altitude;
        /**
         * Latitude, EPSG:3857 (Google Maps, Openstreetmaps datum, 0-4294967295)
         */
        int latitude;
        /**
         * Longitude, EPSG:3857 (Google Maps, Openstreetmaps datum, 0-4294967295)
         */
        int longitude;
    }

    /**
     * How humidity is stored
     */
    static public class Humidity extends TimelineHeader {
        /**
         * Relative humidity, 0-100%
         */
        byte humidity;
    }

    /**
     * How air pressure is stored
     */
    static public class Pressure extends TimelineHeader {
        /**
         * Air pressure in hectopascals (hPa, 0-65535)
         */
        short pressure;
    }

    /**
     * How special events are stored
     */
    static public class Special extends TimelineHeader {
        /**
         * Special event's type
         */
        SpecialType type;
    }

    /**
     * How air quality is stored
     * <p>
     * These events are a bit more complex because the topic is not simple,
     * the intention is to heavy-lift the annoying preprocessing from the watch
     * this allows watchface or watchapp makers to generate accurate alerts and graphics
     * <p>
     * If this needs further enforced standardization, pull requests are welcome
     */
    static public class AirQuality extends TimelineHeader {
        /**
         * The name of the pollution
         * <p>
         * for the sake of better compatibility with watchapps
         * that might want to use this data for say visuals
         * don't localize the name.
         * <p>
         * Ideally watchapp itself localizes the name, if it's at all needed.
         * <p>
         * E.g.
         * For generic ones use "PM0.1", "PM5", "PM10"
         * For chemical compounds use the molecular formula e.g. "NO2", "CO2", "O3"
         * For pollen use the genus, e.g. "Betula" for birch or "Alternaria" for that mold's spores
         */
        String polluter;
        /**
         * Amount of the pollution in SI units,
         * otherwise it's going to be difficult to create UI, alerts
         * and so on and for.
         * <p>
         * See more:
         * https://ec.europa.eu/environment/air/quality/standards.htm
         * http://www.ourair.org/wp-content/uploads/2012-aaqs2.pdf
         * <p>
         * Example units:
         * count/m³ for pollen
         * µgC/m³ for micrograms of organic carbon
         * µg/m³ sulfates, PM0.1, PM1, PM2, PM10 and so on, dust
         * mg/m³ CO2, CO
         * ng/m³ for heavy metals
         * <p>
         * List is not comprehensive, should be improved.
         * The current ones are what watchapps assume.
         * <p>
         * Note: ppb and ppm to concentration should be calculated on the companion, using
         * the correct formula (taking into account temperature and air pressure)
         * <p>
         * Note2: The amount is off by times 100, for two decimal places of precision.
         * E.g. 54.32µg/m³ is 5432
         */
        int amount;
    }
}
