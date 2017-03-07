package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2;

public enum FirmwareType {
    FIRMWARE((byte) 0),
    FONT((byte) 1),
    UNKNOWN1((byte) 2),
    UNKNOWN2((byte) 3),
    INVALID(Byte.MIN_VALUE);

    private final byte value;

    FirmwareType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
