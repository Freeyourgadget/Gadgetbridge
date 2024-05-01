package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionWeatherCondition extends FieldDefinition {

    public FieldDefinitionWeatherCondition(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int idx = (int) baseType.decode(byteBuffer, scale, offset);
        return Condition.values()[idx];
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Condition) {
            baseType.encode(byteBuffer, ((Condition) o).ordinal(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, openWeatherCodeToFitWeatherStatus((int) o), scale, offset);
    }

    private int openWeatherCodeToFitWeatherStatus(int openWeatherCode) {
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
                return Condition.THUNDERSTORMS.ordinal();
            case 221:  //ragged thunderstorm:  //11d
                return Condition.SCATTERED_THUNDERSTORMS.ordinal();
//Group 3xx: Drizzle
            case 300:  //light intensity drizzle:  //09d
            case 310:  //light intensity drizzle rain:  //09d
            case 313:  //shower rain and drizzle:  //09d
                return Condition.LIGHT_RAIN.ordinal();
            case 301:  //drizzle:  //09d
            case 311:  //drizzle rain:  //09d
                return Condition.RAIN.ordinal();
            case 302:  //heavy intensity drizzle:  //09d
            case 312:  //heavy intensity drizzle rain:  //09d
            case 314:  //heavy shower rain and drizzle:  //09d
                return Condition.HEAVY_RAIN.ordinal();
            case 321:  //shower drizzle:  //09d
                return Condition.SCATTERED_SHOWERS.ordinal();
//Group 5xx: Rain
            case 500:  //light rain:  //10d
            case 520:  //light intensity shower rain:  //09d
            case 521:  //shower rain:  //09d
                return Condition.LIGHT_RAIN.ordinal();
            case 501:  //moderate rain:  //10d
            case 531:  //ragged shower rain:  //09d
                return Condition.RAIN.ordinal();
            case 502:  //heavy intensity rain:  //10d
            case 503:  //very heavy rain:  //10d
            case 504:  //extreme rain:  //10d
            case 522:  //heavy intensity shower rain:  //09d
                return Condition.HEAVY_RAIN.ordinal();
            case 511:  //freezing rain:  //13d
                return Condition.UNKNOWN_PRECIPITATION.ordinal();
//Group 6xx: Snow
            case 600:  //light snow:  //[[file:13d.png]]
                return Condition.LIGHT_SNOW.ordinal();
            case 601:  //snow:  //[[file:13d.png]]
            case 620:  //light shower snow:  //[[file:13d.png]]
            case 621:  //shower snow:  //[[file:13d.png]]
                return Condition.SNOW.ordinal();
            case 602:  //heavy snow:  //[[file:13d.png]]
            case 622:  //heavy shower snow:  //[[file:13d.png]]
                return Condition.HEAVY_SNOW.ordinal();
            case 611:  //sleet:  //[[file:13d.png]]
            case 612:  //light shower sleet:  //[[file:13d.png]]
            case 613:  //shower sleet:  //[[file:13d.png]]
                return Condition.WINTRY_MIX.ordinal();
            case 615:  //light rain and snow:  //[[file:13d.png]]
                return Condition.LIGHT_RAIN_SNOW.ordinal();
            case 616:  //rain and snow:  //[[file:13d.png]]
                return Condition.HEAVY_RAIN_SNOW.ordinal();

//Group 7xx: Atmosphere
            case 701:  //mist:  //[[file:50d.png]]
            case 711:  //smoke:  //[[file:50d.png]]
            case 721:  //haze:  //[[file:50d.png]]
            case 731:  //sandcase  dust whirls:  //[[file:50d.png]]
            case 751:  //sand:  //[[file:50d.png]]
            case 761:  //dust:  //[[file:50d.png]]
            case 762:  //volcanic ash:  //[[file:50d.png]]
                return Condition.HAZY.ordinal();
            case 741:  //fog:  //[[file:50d.png]]
                return Condition.FOG.ordinal();
            case 771:  //squalls:  //[[file:50d.png]]
            case 781:  //tornado:  //[[file:50d.png]]
                return Condition.WINDY.ordinal();
//Group 800: Clear
            case 800:  //clear sky:  //[[file:01d.png]] [[file:01n.png]]
                return Condition.CLEAR.ordinal();

//Group 80x: Clouds
            case 801:  //few clouds:  //[[file:02d.png]] [[file:02n.png]]
            case 802:  //scattered clouds:  //[[file:03d.png]] [[file:03d.png]]
                return Condition.PARTLY_CLOUDY.ordinal();
            case 803:  //broken clouds:  //[[file:04d.png]] [[file:03d.png]]
                return Condition.MOSTLY_CLOUDY.ordinal();
            case 804:  //overcast clouds:  //[[file:04d.png]] [[file:04d.png]]
                return Condition.CLOUDY.ordinal();
//Group 90x: Extreme
            case 901:  //tropical storm
                return Condition.THUNDERSTORMS.ordinal();
            case 906:  //hail
                return Condition.HAIL.ordinal();
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
                return 255; //invalid
        }
    }

    public enum Condition {
        CLEAR,
        PARTLY_CLOUDY,
        MOSTLY_CLOUDY,
        RAIN,
        SNOW,
        WINDY,
        THUNDERSTORMS,
        WINTRY_MIX,
        FOG,
        UNK9,
        UNK10,
        HAZY,
        HAIL,
        SCATTERED_SHOWERS,
        SCATTERED_THUNDERSTORMS,
        UNKNOWN_PRECIPITATION,
        LIGHT_RAIN,
        HEAVY_RAIN,
        LIGHT_SNOW,
        HEAVY_SNOW,
        LIGHT_RAIN_SNOW,
        HEAVY_RAIN_SNOW,
        CLOUDY,
        ;
    }
}
