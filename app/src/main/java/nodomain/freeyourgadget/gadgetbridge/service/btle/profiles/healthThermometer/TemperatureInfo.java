/*  Copyright (C) 2023-2024 Alicia Hormann

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.healthThermometer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class TemperatureInfo implements Parcelable{

    private float temperature;
    private int temperatureType;
    private Date timestamp;

    public TemperatureInfo() {
    }

    protected TemperatureInfo(Parcel in) {
        timestamp = new Date(in.readLong());
        temperature = in.readFloat();
        temperatureType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp.getTime());
        dest.writeFloat(temperature);
        dest.writeInt(temperatureType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TemperatureInfo> CREATOR = new Creator<TemperatureInfo>() {
        @Override
        public TemperatureInfo createFromParcel(Parcel in) {
            return new TemperatureInfo(in);
        }

        @Override
        public TemperatureInfo[] newArray(int size) {
            return new TemperatureInfo[size];
        }
    };

    public float getTemperature() {
        return temperature;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date date) {
        timestamp = date;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getTemperatureType() {
        return temperatureType;
    }

    public void setTemperatureType(int temperatureType) {
        this.temperatureType = temperatureType;
    }
}
