/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status;

import org.slf4j.LoggerFactory;

public class Weather {
    private final WeatherType main;
    private final WeatherType sub;

    public Weather(WeatherType main, WeatherType sub) {
        this.main = main;
        this.sub = sub;
    }

    public byte packed() {
        return (byte)((main.ordinal() << 3) | sub.ordinal());
    }

    public static Weather fromOpenWeatherMap(int conditionCode) {
        WeatherType main = WeatherType.SUNNY;
        WeatherType sub = WeatherType.SUNNY;

        switch (conditionCode) {
            // https://openweathermap.org/weather-conditions
            case 200:
            case 201:
            case 210:
            case 230:
            case 231:
            case 300:
            case 301:
            case 310:
            case 311:
            case 313:
            case 500:
            case 501:
            case 520:
            case 521:
                main = WeatherType.RAIN;
                sub = WeatherType.RAIN;
                break;

            case 202:
            case 212:
            case 221:
            case 232:
            case 302:
            case 312:
            case 314:
            case 321:
            case 502:
            case 503:
            case 504:
            case 522:
            case 531:
                main = WeatherType.HEAVY_RAIN;
                sub = WeatherType.HEAVY_RAIN;
                break;

            case 511:
            case 615:
            case 616:
                main = WeatherType.RAIN;
                sub = WeatherType.SNOW;
                break;

            case 600:
            case 601:
            case 602:
            case 611:
            case 612:
            case 613:
            case 620:
            case 621:
            case 622:
                main = WeatherType.SNOW;
                sub = WeatherType.SNOW;
                break;

            case 701:
            case 711:
            case 721:
            case 731:
            case 741:
            case 751:
            case 761:
            case 762:
            case 771:
            case 781:
                main = WeatherType.SUNNY;
                sub = WeatherType.CLOUDY;
                break;

            case 800:
            case 801:
                main = WeatherType.SUNNY;
                sub = WeatherType.SUNNY;
                break;

            case 802:
            case 803:
                main = WeatherType.SUNNY;
                sub = WeatherType.CLOUDY;
                break;

            case 804:
                main = WeatherType.CLOUDY;
                sub = WeatherType.CLOUDY;
                break;

            default:
                LoggerFactory.getLogger(Weather.class)
                        .warn("Unknown condition code " + conditionCode);
                break;
        }

        return new Weather(main, sub);
    }
}

