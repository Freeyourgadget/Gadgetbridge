/*
 * Copyright (C) 2016 The CyanongenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lineageos.weather;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import lineageos.os.Build;
import lineageos.os.Concierge;
import lineageos.os.Concierge.ParcelInfo;
import lineageos.providers.WeatherContract;
import lineageos.weatherservice.ServiceRequest;
import lineageos.weatherservice.ServiceRequestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class represents the weather information that a
 * {@link lineageos.weatherservice.WeatherProviderService} will use to update the weather content
 * provider. A weather provider service will be called by the system to process an update
 * request at any time. If the service successfully processes the request, then the weather provider
 * service is responsible of calling
 * {@link ServiceRequest#complete(ServiceRequestResult)} to notify the
 * system that the request was completed and that the weather content provider should be updated
 * with the supplied weather information.
 */
public final class WeatherInfo implements Parcelable {

    private String mCity;
    private int mConditionCode;
    private double mTemperature;
    private int mTempUnit;
    private double mTodaysHighTemp;
    private double mTodaysLowTemp;
    private double mHumidity;
    private double mWindSpeed;
    private double mWindDirection;
    private int mWindSpeedUnit;
    private long mTimestamp;
    private List<DayForecast> mForecastList;
    private String mKey;

    private WeatherInfo() {}

    /**
     * Builder class for {@link WeatherInfo}
     */
    public static class Builder {
        private String mCity;
        private int mConditionCode = WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
        private double mTemperature;
        private int mTempUnit;
        private double mTodaysHighTemp = Double.NaN;
        private double mTodaysLowTemp = Double.NaN;
        private double mHumidity = Double.NaN;
        private double mWindSpeed = Double.NaN;
        private double mWindDirection = Double.NaN;
        private int mWindSpeedUnit = WeatherContract.WeatherColumns.WindSpeedUnit.MPH;
        private long mTimestamp = -1;
        private List<DayForecast> mForecastList = new ArrayList<>(0);

        /**
         * @param cityName A valid city name. Attempting to pass null will get you an
         *                 IllegalArgumentException
         * @param temperature A valid temperature value. Attempting pass an invalid double value,
         *                    will get you an IllegalArgumentException
         * @param tempUnit A valid temperature unit value. See
         *                 {@link lineageos.providers.WeatherContract.WeatherColumns.TempUnit} for
         *                 valid values. Attempting to pass an invalid temperature unit will get you
         *                 an IllegalArgumentException
         */
        public Builder(@NonNull String cityName, double temperature, int tempUnit) {
            if (cityName == null) {
                throw new IllegalArgumentException("City name can't be null");
            }
            if (Double.isNaN(temperature)) {
                throw new IllegalArgumentException("Invalid temperature");
            }
            if (!isValidTempUnit(tempUnit)) {
                throw new IllegalArgumentException("Invalid temperature unit");
            }
            this.mCity = cityName;
            this.mTemperature = temperature;
            this.mTempUnit = tempUnit;
        }

        /**
         * @param timeStamp A timestamp indicating when this data was generated. If timestamps is
         *                  not set, then the builder will set it to the time of object creation
         * @return The {@link Builder} instance
         */
        public Builder setTimestamp(long timeStamp) {
            mTimestamp = timeStamp;
            return this;
        }

        /**
         * @param humidity The weather humidity. Attempting to pass an invalid double value will get
         *                 you an IllegalArgumentException
         * @return The {@link Builder} instance
         */
        public Builder setHumidity(double humidity) {
            if (Double.isNaN(humidity)) {
                throw new IllegalArgumentException("Invalid humidity value");
            }

            mHumidity = humidity;
            return this;
        }

        /**
         * @param windSpeed The wind speed. Attempting to pass an invalid double value will get you
         *                  an IllegalArgumentException
         * @param windDirection The wind direction. Attempting to pass an invalid double value will
         *                      get you an IllegalArgumentException
         * @param windSpeedUnit A valid wind speed direction unit. See
         *                      {@link lineageos.providers.WeatherContract.WeatherColumns.WindSpeedUnit}
         *                      for valid values. Attempting to pass an invalid speed unit will get
         *                      you an IllegalArgumentException
         * @return The {@link Builder} instance
         */
        public Builder setWind(double windSpeed, double windDirection, int windSpeedUnit) {
            if (Double.isNaN(windSpeed)) {
                throw new IllegalArgumentException("Invalid wind speed value");
            }
            if (Double.isNaN(windDirection)) {
                throw new IllegalArgumentException("Invalid wind direction value");
            }
            if (!isValidWindSpeedUnit(windSpeedUnit)) {
                throw new IllegalArgumentException("Invalid speed unit");
            }
            mWindSpeed = windSpeed;
            mWindSpeedUnit = windSpeedUnit;
            mWindDirection = windDirection;
            return this;
        }

        /**
         * @param conditionCode A valid weather condition code. See
         *                      {@link lineageos.providers.WeatherContract.WeatherColumns.WeatherCode}
         *                      for valid codes. Attempting to pass an invalid code will get you an
         *                      IllegalArgumentException.
         * @return The {@link Builder} instance
         */
        public Builder setWeatherCondition(int conditionCode) {
            if (!isValidWeatherCode(conditionCode)) {
                throw new IllegalArgumentException("Invalid weather condition code");
            }
            mConditionCode = conditionCode;
            return this;
        }

        /**
         * @param forecasts A valid array list of {@link DayForecast} objects. Attempting to pass
         *                  null will get you an IllegalArgumentException'
         * @return The {@link Builder} instance
         */
        public Builder setForecast(@NonNull List<DayForecast> forecasts) {
            if (forecasts == null) {
                throw new IllegalArgumentException("Forecast list can't be null");
            }
            mForecastList = forecasts;
            return this;
        }

        /**
         *
         * @param todaysHigh Today's high temperature. Attempting to pass an invalid double value
         *                   will get you an IllegalArgumentException
         * @return The {@link Builder} instance
         */
        public Builder setTodaysHigh(double todaysHigh) {
            if (Double.isNaN(todaysHigh)) {
                throw new IllegalArgumentException("Invalid temperature value");
            }
            mTodaysHighTemp = todaysHigh;
            return this;
        }

        /**
         * @param todaysLow Today's low temperature. Attempting to pass an invalid double value will
         *                  get you an IllegalArgumentException
         * @return
         */
        public Builder setTodaysLow(double todaysLow) {
            if (Double.isNaN(todaysLow)) {
                throw new IllegalArgumentException("Invalid temperature value");
            }
            mTodaysLowTemp = todaysLow;
            return this;
        }

        /**
         * Combine all of the options that have been set and return a new {@link WeatherInfo} object
         * @return {@link WeatherInfo}
         */
        public WeatherInfo build() {
            WeatherInfo info = new WeatherInfo();
            info.mCity = this.mCity;
            info.mConditionCode = this.mConditionCode;
            info.mTemperature = this.mTemperature;
            info.mTempUnit = this.mTempUnit;
            info.mHumidity = this.mHumidity;
            info.mWindSpeed = this.mWindSpeed;
            info.mWindDirection = this.mWindDirection;
            info.mWindSpeedUnit = this.mWindSpeedUnit;
            info.mTimestamp = this.mTimestamp == -1 ? System.currentTimeMillis() : this.mTimestamp;
            info.mForecastList = this.mForecastList;
            info.mTodaysHighTemp = this.mTodaysHighTemp;
            info.mTodaysLowTemp = this.mTodaysLowTemp;
            info.mKey = UUID.randomUUID().toString();
            return info;
        }

        private boolean isValidTempUnit(int unit) {
            switch (unit) {
                case WeatherContract.WeatherColumns.TempUnit.CELSIUS:
                case WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT:
                    return true;
                default:
                    return false;
            }
        }

        private boolean isValidWindSpeedUnit(int unit) {
            switch (unit) {
                case WeatherContract.WeatherColumns.WindSpeedUnit.KPH:
                case WeatherContract.WeatherColumns.WindSpeedUnit.MPH:
                    return true;
                default:
                    return false;
            }
        }
    }


    private static boolean isValidWeatherCode(int code) {
        if (code < WeatherContract.WeatherColumns.WeatherCode.WEATHER_CODE_MIN
                || code > WeatherContract.WeatherColumns.WeatherCode.WEATHER_CODE_MAX) {
            if (code != WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return city name
     */
    public String getCity() {
        return mCity;
    }

    /**
     * @return An implementation specific weather condition code
     */
    public int getConditionCode() {
        return mConditionCode;
    }

    /**
     * @return humidity
     */
    public double getHumidity() {
        return mHumidity;
    }

    /**
     * @return time stamp when the request was processed
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * @return wind direction (degrees)
     */
    public double getWindDirection() {
        return mWindDirection;
    }

    /**
     * @return wind speed
     */
    public double getWindSpeed() {
        return mWindSpeed;
    }

    /**
     * @return wind speed unit
     */
    public int getWindSpeedUnit() {
        return mWindSpeedUnit;
    }

    /**
     * @return current temperature
     */
    public double getTemperature() {
        return mTemperature;
    }

    /**
     * @return temperature unit
     */
    public int getTemperatureUnit() {
        return mTempUnit;
    }

    /**
     * @return today's high temperature
     */
    public double getTodaysHigh() {
        return mTodaysHighTemp;
    }

    /**
     * @return today's low temperature
     */
    public double getTodaysLow() {
        return mTodaysLowTemp;
    }

    /**
     * @return List of {@link lineageos.weather.WeatherInfo.DayForecast}. This list will contain
     * the forecast weather for the upcoming days. If you want to know today's high and low
     * temperatures, use {@link WeatherInfo#getTodaysHigh()} and {@link WeatherInfo#getTodaysLow()}
     */
    public List<DayForecast> getForecasts() {
        return new ArrayList<>(mForecastList);
    }

    private WeatherInfo(Parcel parcel) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
        int parcelableVersion = parcelInfo.getParcelVersion();

        if (parcelableVersion >= Build.LINEAGE_VERSION_CODES.ELDERBERRY) {
            mKey = parcel.readString();
            mCity = parcel.readString();
            mConditionCode = parcel.readInt();
            mTemperature = parcel.readDouble();
            mTempUnit = parcel.readInt();
            mHumidity = parcel.readDouble();
            mWindSpeed = parcel.readDouble();
            mWindDirection = parcel.readDouble();
            mWindSpeedUnit = parcel.readInt();
            mTodaysHighTemp = parcel.readDouble();
            mTodaysLowTemp = parcel.readDouble();
            mTimestamp = parcel.readLong();
            int forecastListSize = parcel.readInt();
            mForecastList = new ArrayList<>();
            while (forecastListSize > 0) {
                mForecastList.add(DayForecast.CREATOR.createFromParcel(parcel));
                forecastListSize--;
            }
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Tell the concierge to prepare the parcel
        ParcelInfo parcelInfo = Concierge.prepareParcel(dest);

        // ==== ELDERBERRY =====
        dest.writeString(mKey);
        dest.writeString(mCity);
        dest.writeInt(mConditionCode);
        dest.writeDouble(mTemperature);
        dest.writeInt(mTempUnit);
        dest.writeDouble(mHumidity);
        dest.writeDouble(mWindSpeed);
        dest.writeDouble(mWindDirection);
        dest.writeInt(mWindSpeedUnit);
        dest.writeDouble(mTodaysHighTemp);
        dest.writeDouble(mTodaysLowTemp);
        dest.writeLong(mTimestamp);
        dest.writeInt(mForecastList.size());
        for (DayForecast dayForecast : mForecastList) {
            dayForecast.writeToParcel(dest, 0);
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    public static final Parcelable.Creator<WeatherInfo> CREATOR =
            new Parcelable.Creator<WeatherInfo>() {

                @Override
                public WeatherInfo createFromParcel(Parcel source) {
                    return new WeatherInfo(source);
                }

                @Override
                public WeatherInfo[] newArray(int size) {
                    return new WeatherInfo[size];
                }
            };

    /**
     * This class represents the weather forecast for a given day. Do not add low and high
     * temperatures for the current day in this list. Use
     * {@link WeatherInfo.Builder#setTodaysHigh(double)} and
     * {@link WeatherInfo.Builder#setTodaysLow(double)} instead.
     */
    public static class DayForecast implements Parcelable{
        double mLow;
        double mHigh;
        int mConditionCode;
        String mKey;

        private DayForecast() {}

        /**
         * Builder class for {@link DayForecast}
         */
        public static class Builder {
            double mLow = Double.NaN;
            double mHigh = Double.NaN;
            int mConditionCode;

            /**
             * @param conditionCode A valid weather condition code. See
             * {@link lineageos.providers.WeatherContract.WeatherColumns.WeatherCode} for valid
             *                      values. Attempting to pass an invalid code will get you an
             *                      IllegalArgumentException
             */
            public Builder(int conditionCode) {
                if (!isValidWeatherCode(conditionCode)) {
                    throw new IllegalArgumentException("Invalid weather condition code");
                }
                mConditionCode = conditionCode;
            }

            /**
             * @param high Forecast high temperature for this day. Attempting to pass an invalid
             *             double value will get you an IllegalArgumentException
             * @return The {@link Builder} instance
             */
            public Builder setHigh(double high) {
                if (Double.isNaN(high)) {
                    throw new IllegalArgumentException("Invalid high forecast temperature");
                }
                mHigh = high;
                return this;
            }

            /**
             * @param low Forecast low temperate for this day. Attempting to pass an invalid double
             *            value will get you an IllegalArgumentException
             * @return The {@link Builder} instance
             */
            public Builder setLow(double low) {
                if (Double.isNaN(low)) {
                    throw new IllegalArgumentException("Invalid low forecast temperature");
                }
                mLow = low;
                return this;
            }


            /**
             * Combine all of the options that have been set and return a new {@link DayForecast}
             * object
             * @return {@link DayForecast}
             */
            public DayForecast build() {
                DayForecast forecast = new DayForecast();
                forecast.mLow = this.mLow;
                forecast.mHigh = this.mHigh;
                forecast.mConditionCode = this.mConditionCode;
                forecast.mKey = UUID.randomUUID().toString();
                return forecast;
            }
        }

        /**
         * @return forecasted low temperature
         */
        public double getLow() {
            return mLow;
        }

        /**
         * @return not what you think. Returns the forecasted high temperature
         */
        public double getHigh() {
            return mHigh;
        }

        /**
         * @return forecasted weather condition code. Implementation specific
         */
        public int getConditionCode() {
            return mConditionCode;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // Tell the concierge to prepare the parcel
            ParcelInfo parcelInfo = Concierge.prepareParcel(dest);

            // ==== ELDERBERRY =====
            dest.writeString(mKey);
            dest.writeDouble(mLow);
            dest.writeDouble(mHigh);
            dest.writeInt(mConditionCode);

            // Complete parcel info for the concierge
            parcelInfo.complete();
        }

        public static final Parcelable.Creator<DayForecast> CREATOR =
                new Parcelable.Creator<DayForecast>() {
                    @Override
                    public DayForecast createFromParcel(Parcel source) {
                        return new DayForecast(source);
                    }

                    @Override
                    public DayForecast[] newArray(int size) {
                        return new DayForecast[size];
                    }
                };

        private DayForecast(Parcel parcel) {
            // Read parcelable version via the Concierge
            ParcelInfo parcelInfo = Concierge.receiveParcel(parcel);
            int parcelableVersion = parcelInfo.getParcelVersion();

            if (parcelableVersion >= Build.LINEAGE_VERSION_CODES.ELDERBERRY) {
                mKey = parcel.readString();
                mLow = parcel.readDouble();
                mHigh = parcel.readDouble();
                mConditionCode = parcel.readInt();
            }

            // Complete parcel info for the concierge
            parcelInfo.complete();
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("{Low temp: ").append(mLow)
                    .append(" High temp: ").append(mHigh)
                    .append(" Condition code: ").append(mConditionCode)
                    .append("}").toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mKey != null) ? mKey.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;

            if (getClass() == obj.getClass()) {
                DayForecast forecast = (DayForecast) obj;
                return (TextUtils.equals(mKey, forecast.mKey));
            }
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append(" City Name: ").append(mCity)
            .append(" Condition Code: ").append(mConditionCode)
            .append(" Temperature: ").append(mTemperature)
            .append(" Temperature Unit: ").append(mTempUnit)
            .append(" Humidity: ").append(mHumidity)
            .append(" Wind speed: ").append(mWindSpeed)
            .append(" Wind direction: ").append(mWindDirection)
            .append(" Wind Speed Unit: ").append(mWindSpeedUnit)
            .append(" Today's high temp: ").append(mTodaysHighTemp)
            .append(" Today's low temp: ").append(mTodaysLowTemp)
            .append(" Timestamp: ").append(mTimestamp).append(" Forecasts: [");
        for (DayForecast dayForecast : mForecastList) {
            builder.append(dayForecast.toString());
        }
        return builder.append("]}").toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mKey != null) ? mKey.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (getClass() == obj.getClass()) {
            WeatherInfo info = (WeatherInfo) obj;
            return (TextUtils.equals(mKey, info.mKey));
        }
        return false;
    }
}