package nodomain.freeyourgadget.gadgetbridge.miband;

import java.util.Locale;

public class DeviceInfo extends AbstractInfo {
    public DeviceInfo(byte[] data) {
        super(data);
    }

    public String getFirmwareVersion() {
        if (mData.length == 16) {
            int last = 15;
            return String.format(Locale.US, "%d.%d.%d.%d", mData[last], mData[last - 1], mData[last - 2], mData[last - 3]);
        }
        return "(unknown)"; // TODO: localization
    }
}
