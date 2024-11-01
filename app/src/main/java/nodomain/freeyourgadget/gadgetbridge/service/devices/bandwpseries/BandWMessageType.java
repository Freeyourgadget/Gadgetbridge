package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;
public enum BandWMessageType {
    REQUEST_WITH_PAYLOAD(0x920b, true),
    REQUEST_WITHOUT_PAYLOAD(0x120b, false),
    RESPONSE_WITH_PAYLOAD(0x920c, true),
    RESPONSE_WITHOUT_PAYLOAD(0x120c, false),
    NOTIFICATION_WITH_PAYLOAD(0x920d, true),
    NOTIFICATION_WITHOUT_PAYLOAD(0x120d, false);


    public final int value;
    public final boolean hasPayload;

    BandWMessageType(int mType, boolean hasPayload) {
        this.value = mType;
        this.hasPayload = hasPayload;
    }

    public static BandWMessageType getByType(int mType) {
        for (BandWMessageType t: values()) {
            if (t.value == mType) {
                return t;
            }
        }
        return null;
    }
}
