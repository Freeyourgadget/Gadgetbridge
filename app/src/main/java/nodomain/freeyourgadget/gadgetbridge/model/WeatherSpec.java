/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

// FIXME: document me and my fields, including units
public class WeatherSpec implements Parcelable {
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
    public int timestamp;
    public String location;
    public int currentTemp;
    public int currentConditionCode;
    public String currentCondition;
    public int todayMaxTemp;
    public int todayMinTemp;
    public ArrayList<Forecast> forecasts = new ArrayList<>();

    public WeatherSpec() {

    }

    protected WeatherSpec(Parcel in) {
        timestamp = in.readInt();
        location = in.readString();
        currentTemp = in.readInt();
        currentConditionCode = in.readInt();
        currentCondition = in.readString();
        todayMaxTemp = in.readInt();
        todayMinTemp = in.readInt();
        in.readList(forecasts, Forecast.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(timestamp);
        dest.writeString(location);
        dest.writeInt(currentTemp);
        dest.writeInt(currentConditionCode);
        dest.writeString(currentCondition);
        dest.writeInt(todayMaxTemp);
        dest.writeInt(todayMinTemp);
        dest.writeList(forecasts);
    }

    public static class Forecast implements Parcelable {
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
        public int minTemp;
        public int maxTemp;
        public int conditionCode;

        public Forecast() {
        }

        public Forecast(int minTemp, int maxTemp, int conditionCode) {
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.conditionCode = conditionCode;
        }

        Forecast(Parcel in) {
            minTemp = in.readInt();
            maxTemp = in.readInt();
            conditionCode = in.readInt();
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
        }
    }
}
