package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol;

public enum MessageType {
    PHONE_REQUEST(0xC4),
    RESPONSE(0x04),
    EARBUDS_REQUEST(0xC0),
    EARBUDS_NOTIFY(0xC7),
    UNKNOWN(0xFF);

    private final byte code;
    private final boolean isRequest;

    MessageType(final int code) {
        this.code = (byte) code;
        this.isRequest = (this.code & 0x40) != 0;
    }

    public byte getCode() {
        return this.code;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public static MessageType fromCode(final byte code) {
        for (final MessageType messageType : values()) {
            if (messageType.code == code) {
                return messageType;
            }
        }

        return MessageType.UNKNOWN;
    }
}
