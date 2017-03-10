/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery;

import android.os.Parcel;
import android.os.Parcelable;

public class BatteryInfo implements Parcelable{

    private int percentCharged;

    public BatteryInfo() {
    }

    protected BatteryInfo(Parcel in) {
        percentCharged = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(percentCharged);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BatteryInfo> CREATOR = new Creator<BatteryInfo>() {
        @Override
        public BatteryInfo createFromParcel(Parcel in) {
            return new BatteryInfo(in);
        }

        @Override
        public BatteryInfo[] newArray(int size) {
            return new BatteryInfo[size];
        }
    };

    public int getPercentCharged() {
        return percentCharged;
    }

    public void setPercentCharged(int percentCharged) {
        this.percentCharged = percentCharged;
    }
}
