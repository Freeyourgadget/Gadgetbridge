package nodomain.freeyourgadget.gadgetbridge.miband;

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

    // TODO: localization
    public String getStatus() {
        if (mData.length >= 10) {
            int value = mData[9];
            switch (value) {
                case 1:
                    return "low";
                case 2:
                    return "medium";
                case 3:
                    return "full";
                case 4:
                    return "not charging";
            }
        }
        return "(unknown)";
    }
}
