package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class BatteryInfo extends AbstractInfo {
    public BatteryInfo(byte[] data) {
        super(data);
    }

    public int getLevelInPercent() {
        if (mData.length >= 1) {
            return mData[0];
        }
        return 50; // actually unknown
    }

    public String getStatus() {
        if (mData.length >= 10) {
            int value = mData[9];
            switch (value) {
                case 1:
                    return GBApplication.getContext().getString(R.string.battery_low);
                case 2:
                    return GBApplication.getContext().getString(R.string.battery_medium);
                case 3:
                    return GBApplication.getContext().getString(R.string.battery_full);
                case 4:
                    return GBApplication.getContext().getString(R.string.battery_not_charging);
            }
        }
        return GBApplication.getContext().getString(R.string._unknown_);
    }
}
