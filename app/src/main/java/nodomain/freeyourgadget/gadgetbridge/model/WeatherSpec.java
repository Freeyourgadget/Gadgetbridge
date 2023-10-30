/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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

package nodomain.freeyourgadget.gadgetbridge.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

// FIXME: document me and my fields, including units
public class WeatherSpec implements Parcelable, Serializable {

    public static final Creator<WeatherSpec> CREATOR = new Creator<WeatherSpec>() {
        @Override
        public WeatherSpec createFromParcel(Parcel in) {
            return new WeatherSpec(in);
        }

        @Override
        public WeatherSpec[] newArray(int size) {
            return new WeatherSpec[size];
        }
    };
    public static final int VERSION = 4;
    private static final long serialVersionUID = VERSION;
    public int timestamp; // unix epoch timestamp, in seconds
    public String location;
    public int currentTemp; // kelvin
    public int currentConditionCode = 3200; // OpenWeatherMap condition code
    public String currentCondition;
    public int currentHumidity;
    public int todayMaxTemp; // kelvin
    public int todayMinTemp; // kelvin
    public float windSpeed; // km per hour
    public int windDirection; // deg
    public float uvIndex;
    public int precipProbability; // %
    public int dewPoint; // kelvin
    public float pressure; // mb
    public int cloudCover; // %
    public float visibility; // m
    public int sunRise; // unix epoch timestamp, in seconds
    public int sunSet; // unix epoch timestamp, in seconds
    public int moonRise; // unix epoch timestamp, in seconds
    public int moonSet; // unix epoch timestamp, in seconds
    public int moonPhase; // deg
    public float latitude;
    public float longitude;
    public int feelsLikeTemp; // kelvin
    public int isCurrentLocation = -1; // 0 for false, 1 for true, -1 for unknown
    public AirQuality airQuality;

    // Forecasts from the next day onward, in chronological order, one entry per day.
    // It should not include the current or previous days
    public ArrayList<Daily> forecasts = new ArrayList<>();

    // Hourly forecasts
    public ArrayList<Hourly> hourly = new ArrayList<>();

    public WeatherSpec() {

    }

    // Lower bounds of beaufort regions 1 to 12
    // Values from https://en.wikipedia.org/wiki/Beaufort_scale
    static final float[] beaufort = new float[] { 2, 6, 12, 20, 29, 39, 50, 62, 75, 89, 103, 118 };
    //                                    level: 0 1  2   3   4   5   6   7   8   9   10   11   12

    public static int toBeaufort(final float speed) {
        int l = 0;
        while (l < beaufort.length && beaufort[l] < speed) {
            l++;
        }
        return l;
    }

    public int windSpeedAsBeaufort() {
        return toBeaufort(this.windSpeed);
    }

    @Nullable
    public Location getLocation() {
        if (latitude == 0 && longitude == 0) {
            return null;
        }
        final Location location = new Location("weatherSpec");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    protected WeatherSpec(Parcel in) {
        int version = in.readInt();
        if (version >= 2) {
            timestamp = in.readInt();
            location = in.readString();
            currentTemp = in.readInt();
            currentConditionCode = in.readInt();
            currentCondition = in.readString();
            currentHumidity = in.readInt();
            todayMaxTemp = in.readInt();
            todayMinTemp = in.readInt();
            windSpeed = in.readFloat();
            windDirection = in.readInt();
            if (version < 4) {
                // Deserialize the old Forecast list and convert them to Daily
                final ArrayList<Forecast> oldForecasts = new ArrayList<>();
                in.readList(oldForecasts, Forecast.class.getClassLoader());
                for (final Forecast forecast : oldForecasts) {
                    final Daily d = new Daily();
                    d.minTemp = forecast.minTemp;
                    d.maxTemp = forecast.maxTemp;
                    d.conditionCode = forecast.conditionCode;
                    d.humidity = forecast.humidity;
                    forecasts.add(d);
                }
            } else {
                in.readList(forecasts, Daily.class.getClassLoader());
            }
        }
        if (version >= 3) {
            uvIndex = in.readFloat();
            precipProbability = in.readInt();
        }
        if (version >= 4) {
            dewPoint = in.readInt();
            pressure = in.readFloat();
            cloudCover = in.readInt();
            visibility = in.readFloat();
            sunRise = in.readInt();
            sunSet = in.readInt();
            moonRise = in.readInt();
            moonSet = in.readInt();
            moonPhase = in.readInt();
            latitude = in.readFloat();
            longitude = in.readFloat();
            feelsLikeTemp = in.readInt();
            isCurrentLocation = in.readInt();
            airQuality = in.readParcelable(AirQuality.class.getClassLoader());
            in.readList(hourly, Hourly.class.getClassLoader());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(VERSION);
        dest.writeInt(timestamp);
        dest.writeString(location);
        dest.writeInt(currentTemp);
        dest.writeInt(currentConditionCode);
        dest.writeString(currentCondition);
        dest.writeInt(currentHumidity);
        dest.writeInt(todayMaxTemp);
        dest.writeInt(todayMinTemp);
        dest.writeFloat(windSpeed);
        dest.writeInt(windDirection);
        dest.writeList(forecasts);
        dest.writeFloat(uvIndex);
        dest.writeInt(precipProbability);
        dest.writeInt(dewPoint);
        dest.writeFloat(pressure);
        dest.writeInt(cloudCover);
        dest.writeFloat(visibility);
        dest.writeInt(sunRise);
        dest.writeInt(sunSet);
        dest.writeInt(moonRise);
        dest.writeInt(moonSet);
        dest.writeInt(moonPhase);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
        dest.writeInt(feelsLikeTemp);
        dest.writeInt(isCurrentLocation);
        dest.writeParcelable(airQuality, 0);
        dest.writeList(hourly);
    }

    @Deprecated // kept for backwards compatibility with old weather apps
    public static class Forecast implements Parcelable, Serializable {
        private static final long serialVersionUID = 1L;

        public static final Creator<Forecast> CREATOR = new Creator<Forecast>() {
            @Override
            public Forecast createFromParcel(Parcel in) {
                return new Forecast(in);
            }

            @Override
            public Forecast[] newArray(int size) {
                return new Forecast[size];
            }
        };
        public int minTemp; // Kelvin
        public int maxTemp; // Kelvin
        public int conditionCode; // OpenWeatherMap condition code
        public int humidity;

        public Forecast() {
        }

        public Forecast(int minTemp, int maxTemp, int conditionCode, int humidity) {
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.conditionCode = conditionCode;
            this.humidity = humidity;
        }

        Forecast(Parcel in) {
            minTemp = in.readInt();
            maxTemp = in.readInt();
            conditionCode = in.readInt();
            humidity = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(minTemp);
            dest.writeInt(maxTemp);
            dest.writeInt(conditionCode);
            dest.writeInt(humidity);
        }
    }

    public static class AirQuality implements Parcelable, Serializable {
        public static final int VERSION = 1;
        private static final long serialVersionUID = VERSION;

        public static final Creator<AirQuality> CREATOR = new Creator<AirQuality>() {
            @Override
            public AirQuality createFromParcel(final Parcel in) {
                return new AirQuality(in);
            }

            @Override
            public AirQuality[] newArray(final int size) {
                return new AirQuality[size];
            }
        };

        public int aqi = -1; // Air Quality Index - usually the max across all AQI values for pollutants

        public float co = -1; // Carbon Monoxide, mg/m^3
        public float no2 = -1; // Nitrogen Dioxide, ug/m^3
        public float o3 = -1; // Ozone, ug/m^3
        public float pm10 = -1; // Particulate Matter, 10 microns or less in diameter, ug/m^3
        public float pm25 = -1; // Particulate Matter, 2.5 microns or less in diameter, ug/m^3
        public float so2 = -1; // Sulphur Dioxide, ug/m^3

        // Air Quality Index values per pollutant
        // These are expected to be in the Plume scale (see https://plumelabs.files.wordpress.com/2023/06/plume_aqi_2023.pdf)
        // Some apps such as Breezy Weather fallback to the WHO 2021 AQI for pollutants that are not mapped in the Plume AQI
        // https://www.who.int/news-room/fact-sheets/detail/ambient-(outdoor)-air-quality-and-health
        //
        // Breezy Weather implementation for reference:
        // - https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/common/basic/models/weather/AirQuality.kt
        // - https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/common/basic/models/options/index/PollutantIndex.kt

        public int coAqi = -1;
        public int no2Aqi = -1;
        public int o3Aqi = -1;
        public int pm10Aqi = -1;
        public int pm25Aqi = -1;
        public int so2Aqi = -1;

        public AirQuality() {
        }

        AirQuality(final Parcel in) {
            in.readInt(); // version
            aqi = in.readInt();
            co = in.readFloat();
            no2 = in.readFloat();
            o3 = in.readFloat();
            pm10 = in.readFloat();
            pm25 = in.readFloat();
            so2 = in.readFloat();
            coAqi = in.readInt();
            no2Aqi = in.readInt();
            o3Aqi = in.readInt();
            pm10Aqi = in.readInt();
            pm25Aqi = in.readInt();
            so2Aqi = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(VERSION);
            dest.writeInt(aqi);
            dest.writeFloat(co);
            dest.writeFloat(no2);
            dest.writeFloat(o3);
            dest.writeFloat(pm10);
            dest.writeFloat(pm25);
            dest.writeFloat(so2);
            dest.writeInt(coAqi);
            dest.writeInt(no2Aqi);
            dest.writeInt(o3Aqi);
            dest.writeInt(pm10Aqi);
            dest.writeInt(pm25Aqi);
            dest.writeInt(so2Aqi);
        }
    }

    public static class Daily implements Parcelable, Serializable {
        public static final int VERSION = 1;
        private static final long serialVersionUID = VERSION;

        public static final Creator<Daily> CREATOR = new Creator<Daily>() {
            @Override
            public Daily createFromParcel(final Parcel in) {
                return new Daily(in);
            }

            @Override
            public Daily[] newArray(final int size) {
                return new Daily[size];
            }
        };
        public int minTemp; // Kelvin
        public int maxTemp; // Kelvin
        public int conditionCode; // OpenWeatherMap condition code
        public int humidity;
        public float windSpeed; // km per hour
        public int windDirection; // deg
        public float uvIndex;
        public int precipProbability; // %
        public int sunRise;
        public int sunSet;
        public int moonRise;
        public int moonSet;
        public int moonPhase;
        public AirQuality airQuality;

        public Daily() {
        }

        Daily(final Parcel in) {
            in.readInt(); // version
            minTemp = in.readInt();
            maxTemp = in.readInt();
            conditionCode = in.readInt();
            humidity = in.readInt();
            windSpeed = in.readFloat();
            windDirection = in.readInt();
            uvIndex = in.readFloat();
            precipProbability = in.readInt();
            sunRise = in.readInt();
            sunSet = in.readInt();
            moonRise = in.readInt();
            moonSet = in.readInt();
            moonPhase = in.readInt();
            airQuality = in.readParcelable(AirQuality.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(VERSION);
            dest.writeInt(minTemp);
            dest.writeInt(maxTemp);
            dest.writeInt(conditionCode);
            dest.writeInt(humidity);
            dest.writeFloat(windSpeed);
            dest.writeInt(windDirection);
            dest.writeFloat(uvIndex);
            dest.writeInt(precipProbability);
            dest.writeInt(sunRise);
            dest.writeInt(sunSet);
            dest.writeInt(moonRise);
            dest.writeInt(moonSet);
            dest.writeInt(moonPhase);
            dest.writeParcelable(airQuality, 0);
        }

        public int windSpeedAsBeaufort() {
            return toBeaufort(this.windSpeed);
        }
    }

    public static class Hourly implements Parcelable, Serializable {
        public static final int VERSION = 1;
        private static final long serialVersionUID = VERSION;

        public static final Creator<Hourly> CREATOR = new Creator<Hourly>() {
            @Override
            public Hourly createFromParcel(final Parcel in) {
                return new Hourly(in);
            }

            @Override
            public Hourly[] newArray(final int size) {
                return new Hourly[size];
            }
        };

        public int timestamp; // unix epoch timestamp, in seconds
        public int temp; // Kelvin
        public int conditionCode; // OpenWeatherMap condition code
        public int humidity;
        public float windSpeed; // km per hour
        public int windDirection; // deg
        public float uvIndex;
        public int precipProbability; // %

        public Hourly() {
        }

        Hourly(final Parcel in) {
            in.readInt(); // version
            timestamp = in.readInt();
            temp = in.readInt();
            conditionCode = in.readInt();
            humidity = in.readInt();
            windSpeed = in.readFloat();
            windDirection = in.readInt();
            uvIndex = in.readFloat();
            precipProbability = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeInt(VERSION);
            dest.writeInt(timestamp);
            dest.writeInt(temp);
            dest.writeInt(conditionCode);
            dest.writeInt(humidity);
            dest.writeFloat(windSpeed);
            dest.writeInt(windDirection);
            dest.writeFloat(uvIndex);
            dest.writeInt(precipProbability);
        }

        public int windSpeedAsBeaufort() {
            return toBeaufort(this.windSpeed);
        }
    }
}
