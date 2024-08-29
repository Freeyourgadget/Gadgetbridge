package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

enum TapAction {
    SINGLE_TAP((byte) 0x02),
    DOUBLE_TAP((byte) 0x00),
    TRIPLE_TAP((byte) 0x05),
    LONG_PRESS((byte) 0x01)
    ;

    private final byte code;

    TapAction(final byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
