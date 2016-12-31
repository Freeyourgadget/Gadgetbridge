package nodomain.freeyourgadget.gadgetbridge.model;

import ru.gelin.android.weather.notification.ParcelableWeather2;

public class Weather {
    private ParcelableWeather2 weather2 = null;
    private WeatherSpec weatherSpec = null;

    public ParcelableWeather2 getWeather2() {
        return weather2;
    }

    public void setWeather2(ParcelableWeather2 weather2) {
        this.weather2 = weather2;
    }

    public WeatherSpec getWeatherSpec() {
        return weatherSpec;
    }

    public void setWeatherSpec(WeatherSpec weatherSpec) {
        this.weatherSpec = weatherSpec;
    }

    private static final Weather weather = new Weather();
    public static Weather getInstance() {return weather;}

    public static byte mapToPebbleCondition(int openWeatherMapCondition) {
/* deducted values:
    0 = sun + cloud
    1 = clouds
    2 = some snow
    3 = some rain
    4 = heavy rain
    5 = heavy snow
    6 = sun + cloud + rain (default icon?)
    7 = sun
    8 = rain + snow
    9 = 6
    10, 11, ... = empty icon
 */
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
                return 4;
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
            case 500:  //light rain:  //10d
            case 501:  //moderate rain:  //10d
                return 3;
//Group 5xx: Rain
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 511:  //freezing rain:  //13d
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
            case 522:  //heavy intensity shower rain:  //09d
            case 531:  //ragged shower rain:  //09d
                return 4;
//Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
            case 601:  //snow:  //[[file:13d.png]]
            case 620:  //light shower snow:  //[[file:13d.png]]
                return 2;
            case 602:  //heavy snow:  //[[file:13d.png]]
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return 5;
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
                return 8;
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
                return 6;
//Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return 7;
//Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return 0;
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
            default:
                return 6;

        }
    }
    public static int mapToYahooCondition(int openWeatherMapCondition) {
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
                return 4;
            case 212:  //heavy thunderstorm:  //11d
            case 221:  //ragged thunderstorm:  //11d
                return 3;
//Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 301:  //drizzle:  //09d
            case 302:  //heavy intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 311:  //drizzle rain:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
                return 9;
            case 313:  //shower rain and drizzle:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
            case 321:  //shower drizzle:  //09d
                return 11;
//Group 5xx: Rain
            case 500:  //light rain:  //10d
            case 501:  //moderate rain:  //10d
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 511:  //freezing rain:  //13d
                return 10;
            case 520:  //light intensity shower rain:  //09d
                return 40;
            case 521:  //shower rain:  //09d
            case 522:  //heavy intensity shower rain:  //09d
            case 531:  //ragged shower rain:  //09d
                return 12;
//Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
                return 7;
            case 601:  //snow:  //[[file:13d.png]]
                return 16;
            case 602:  //heavy snow:  //[[file:13d.png]]
                return 15;
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //shower sleet:  //[[file:13d.png]]
                return 18;
            case 615:  //light rain and snow:  //[[file:13d.png]]
            case 616:  //rain and snow:  //[[file:13d.png]]
                return 5;
            case 620:  //light shower snow:  //[[file:13d.png]]
                return 14;
            case 621:  //shower snow:  //[[file:13d.png]]
                return 46;
            case 622:  //heavy shower snow:  //[[file:13d.png]]
//Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
                return 22;
            case 721:  //haze:  //[[file:50d.png]]
                return 21;
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
                return 3200;
            case 741:  //fog:  //[[file:50d.png]]
                return 20;
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
                return 19;
            case 762:  //volcanic ash:  //[[file:50d.png]]
            case 771:  //squalls:  //[[file:50d.png]]
                return 3200;
            case 781:  //tornado:  //[[file:50d.png]]
            case 900:  //tornado
                return 0;
//Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return 32;
//Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                return 34;
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return 44;
//Group 90x: Extreme
            case 901:  //tropical storm
                return 1;
            case 903:  //cold
                return 25;
            case 904:  //hot
                return 36;
            case 905:  //windy
                return 24;
            case 906:  //hail
                return 17;
//Group 9xx: Additional
            case 951:  //calm
            case 952:  //light breeze
            case 953:  //gentle breeze
            case 954:  //moderate breeze
            case 955:  //fresh breeze
                return 34;
            case 956:  //strong breeze
            case 957:  //high windcase  near gale
                return 24;
            case 958:  //gale
            case 959:  //severe gale
            case 960:  //storm
            case 961:  //violent storm
                return 3200;
            case 902:  //hurricane
            case 962:  //hurricane
                return 2;
            default:
                return 3200;

        }
    }

    public static int mapToOpenWeatherMapCondition(int yahooCondition) {
        switch (yahooCondition) {
//yahoo weather conditions:
//https://developer.yahoo.com/weather/documentation.html
            case 0:  //tornado
                return 900;
            case 1:  //tropical storm
                return 901;
            case 2:  //hurricane
                return 962;
            case 3:  //severe thunderstorms
                return 212;
            case 4:  //thunderstorms
                return 211;
            case 5:  //mixed rain and snow
            case 6:  //mixed rain and sleet
                return 616;
            case 7:  //mixed snow and sleet
                return 600;
            case 8:  //freezing drizzle
            case 9:  //drizzle
                return 301;
            case 10:  //freezing rain
                return 511;
            case 11:  //showers
            case 12:  //showers
                return 521;
            case 13:  //snow flurries
            case 14:  //light snow showers
                return 620;
            case 15:  //blowing snow
            case 41:  //heavy snow
            case 42:  //scattered snow showers
            case 43:  //heavy snow
            case 46:  //snow showers
                return 602;
            case 16:  //snow
                return 601;
            case 17:  //hail
            case 35:  //mixed rain and hail
                return 906;
            case 18:  //sleet
                return 611;
            case 19:  //dust
                return 761;
            case 20:  //foggy
                return 741;
            case 21:  //haze
                return 721;
            case 22:  //smoky
                return 711;
            case 23:  //blustery
            case 24:  //windy
                return 905;
            case 25:  //cold
                return 903;
            case 26:  //cloudy
            case 27:  //mostly cloudy (night)
            case 28:  //mostly cloudy (day)
                return 804;
            case 29:  //partly cloudy (night)
            case 30:  //partly cloudy (day)
                return 801;
            case 31:  //clear (night)
            case 32:  //sunny
                return 800;
            case 33:  //fair (night)
            case 34:  //fair (day)
                return 801;
            case 36:  //hot
                return 904;
            case 37:  //isolated thunderstorms
            case 38:  //scattered thunderstorms
            case 39:  //scattered thunderstorms
                return 210;
            case 40:  //scattered showers
                return 520;
            case 44:  //partly cloudy
                return 801;
            case 45:  //thundershowers
            case 47:  //isolated thundershowers
                return 621;
            case 3200:  //not available
            default:
                return -1;
        }
    }
}
