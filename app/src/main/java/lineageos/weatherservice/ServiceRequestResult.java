/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package lineageos.weatherservice;

import android.os.Parcel;
import android.os.Parcelable;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import lineageos.os.Build;
import lineageos.os.Concierge;
import lineageos.os.Concierge.ParcelInfo;
import lineageos.weather.WeatherLocation;
import lineageos.weather.WeatherInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use this class to build a request result.
 */
public final class ServiceRequestResult implements Parcelable {

    private WeatherInfo mWeatherInfo;
    private List<WeatherLocation> mLocationLookupList;
    private String mKey;

    private ServiceRequestResult() {}

    private ServiceRequestResult(Parcel in) {
        // Read parcelable version via the Concierge
        ParcelInfo parcelInfo = Concierge.receiveParcel(in);
        int parcelableVersion = parcelInfo.getParcelVersion();

        if (parcelableVersion >= Build.LINEAGE_VERSION_CODES.ELDERBERRY) {
            mKey = in.readString();
            int hasWeatherInfo = in.readInt();
            if (hasWeatherInfo == 1) {
                mWeatherInfo = WeatherInfo.CREATOR.createFromParcel(in);
            }
            int hasLocationLookupList = in.readInt();
            if (hasLocationLookupList == 1) {
                mLocationLookupList = new ArrayList<>();
                int listSize = in.readInt();
                while (listSize > 0) {
                    mLocationLookupList.add(WeatherLocation.CREATOR.createFromParcel(in));
                    listSize--;
                }
            }
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    public static final Creator<ServiceRequestResult> CREATOR
            = new Creator<ServiceRequestResult>() {
        @Override
        public ServiceRequestResult createFromParcel(Parcel in) {
            return new ServiceRequestResult(in);
        }

        @Override
        public ServiceRequestResult[] newArray(int size) {
            return new ServiceRequestResult[size];
        }
    };

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
        if (mWeatherInfo != null) {
            dest.writeInt(1);
            mWeatherInfo.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (mLocationLookupList != null) {
            dest.writeInt(1);
            dest.writeInt(mLocationLookupList.size());
            for (WeatherLocation lookup : mLocationLookupList) {
                lookup.writeToParcel(dest, 0);
            }
        } else {
            dest.writeInt(0);
        }

        // Complete parcel info for the concierge
        parcelInfo.complete();
    }

    /**
     * Builder class for {@link ServiceRequestResult}
     */
    public static class Builder {
        private WeatherInfo mWeatherInfo;
        private List<WeatherLocation> mLocationLookupList;
        public Builder() {
            this.mWeatherInfo = null;
            this.mLocationLookupList = null;
        }

        /**
         * @param weatherInfo The WeatherInfo object holding the data that will be used to update
         *                    the weather content provider
         */
        public Builder(@NonNull WeatherInfo weatherInfo) {
            if (weatherInfo == null) {
                throw new IllegalArgumentException("WeatherInfo can't be null");
            }

            mWeatherInfo = weatherInfo;
        }

        /**
         * @param locations The list of WeatherLocation objects. The list should not be null
         */
        public Builder(@NonNull List<WeatherLocation> locations) {
            if (locations == null) {
                throw new IllegalArgumentException("Weather location list can't be null");
            }
            mLocationLookupList = locations;
        }

        /**
         * Creates a {@link ServiceRequestResult} with the arguments
         * supplied to this builder
         * @return {@link ServiceRequestResult}
         */
        public ServiceRequestResult build() {
            ServiceRequestResult result = new ServiceRequestResult();
            result.mWeatherInfo = this.mWeatherInfo;
            result.mLocationLookupList = this.mLocationLookupList;
            result.mKey = UUID.randomUUID().toString();
            return result;
        }
    }

    /**
     * @return The WeatherInfo object supplied by the weather provider service
     */
    public WeatherInfo getWeatherInfo() {
        return mWeatherInfo;
    }

    /**
     * @return The list of WeatherLocation objects supplied by the weather provider service
     */
    public List<WeatherLocation> getLocationLookupList() {
        return new ArrayList<>(mLocationLookupList);
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
            ServiceRequestResult request = (ServiceRequestResult) obj;
            return (TextUtils.equals(mKey, request.mKey));
        }
        return false;
    }
}
