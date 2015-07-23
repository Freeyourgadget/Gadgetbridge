package nodomain.freeyourgadget.gadgetbridge.miband;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class DeviceInfo extends AbstractInfo {
    public DeviceInfo(byte[] data) {
        super(data);
    }

    public String getHumanFirmwareVersion() {
        if (mData.length == 16) {
            int last = 15;
            return String.format(Locale.US, "%d.%d.%d.%d", mData[last], mData[last - 1], mData[last - 2], mData[last - 3]);
        }
        return GBApplication.getContext().getString(R.string._unknown_);
    }

    public int getFirmwareVersion() {
        if (mData.length == 16) {
            int last = 15;
            return (mData[last] << 24) | (mData[last - 1] << 16) | (mData[last - 2] << 8) | mData[last - 3];
        }
        return -1;
    }

}
