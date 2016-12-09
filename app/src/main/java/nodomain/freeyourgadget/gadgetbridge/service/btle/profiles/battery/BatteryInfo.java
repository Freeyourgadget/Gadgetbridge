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
