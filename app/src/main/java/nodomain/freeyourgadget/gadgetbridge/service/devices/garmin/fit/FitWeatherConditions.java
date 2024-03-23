package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

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

    public static int openWeatherCodeToFitWeatherStatus(int openWeatherCode) {
        switch (openWeatherCode) {
//Group 2xx: Thunderstorm
            case 200:  //thunderstorm with light rain:  //11d
            case 201:  //thunderstorm with rain:  //11d
            case 202:  //thunderstorm with heavy rain:  //11d
            case 210:  //light thunderstorm::  //11d
            case 211:  //thunderstorm:  //11d
            case 212:  //heavy thunderstorm:  //11d
            case 230:  //thunderstorm with light drizzle:  //11d
            case 231:  //thunderstorm with drizzle:  //11d
            case 232:  //thunderstorm with heavy drizzle:  //11d
                return THUNDERSTORMS;
            case 221:  //ragged thunderstorm:  //11d
                return SCATTERED_THUNDERSTORMS;
//Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 313:  //shower rain and drizzle:  //09d
                return LIGHT_RAIN;
            case 301:  //drizzle:  //09d
            case 311:  //drizzle rain:  //09d
                return RAIN;
            case 302:  //heavy intensity drizzle:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
                return HEAVY_RAIN;
            case 321:  //shower drizzle:  //09d
                return SCATTERED_SHOWERS;
//Group 5xx: Rain
            case 500:  //light rain:  //10d
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
                return LIGHT_RAIN;
            case 501:  //moderate rain:  //10d
            case 531:  //ragged shower rain:  //09d
                return RAIN;
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 522:  //heavy intensity shower rain:  //09d
                return HEAVY_RAIN;
            case 511:  //freezing rain:  //13d
                return UNKNOWN_PRECIPITATION;
//Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
                return LIGHT_SNOW;
            case 601:  //snow:  //[[file:13d.png]]
            case 620:  //light shower snow:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
                return SNOW;
            case 602:  //heavy snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return HEAVY_SNOW;
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //light shower sleet:  //[[file:13d.png]]
            case 613:  //shower sleet:  //[[file:13d.png]]
                return WINTRY_MIX;
            case 615:  //light rain and snow:  //[[file:13d.png]]
                return LIGHT_RAIN_SNOW;
            case 616:  //rain and snow:  //[[file:13d.png]]
                return HEAVY_RAIN_SNOW;

//Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
            case 721:  //haze:  //[[file:50d.png]]
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
            case 762:  //volcanic ash:  //[[file:50d.png]]
                return HAZY;
            case 741:  //fog:  //[[file:50d.png]]
                return FOG;
            case 771:  //squalls:  //[[file:50d.png]]
            case 781:  //tornado:  //[[file:50d.png]]
                return WINDY;
//Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return CLEAR;

//Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                return PARTLY_CLOUDY;
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return MOSTLY_CLOUDY;
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return CLOUDY;
//Group 90x: Extreme
            case 901:  //tropical storm
                return THUNDERSTORMS;
            case 906:  //hail
                return HAIL;
            case 903:  //cold
            case 904:  //hot
            case 905:  //windy
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
                throw new IllegalArgumentException("Unknown weather code " + openWeatherCode);
        }
    }
}
