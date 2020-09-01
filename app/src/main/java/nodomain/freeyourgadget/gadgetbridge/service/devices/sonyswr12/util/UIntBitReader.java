package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util;

public class UIntBitReader {
    private final long value;
    private int offset;

    public UIntBitReader(long value, int offset) {
        this.value = value;
        this.offset = offset;
    }

    public int read(int offset) {
        this.offset -= offset;
        if (this.offset < 0) {
            throw new IllegalArgumentException("Read out of range");
        }
        return (int) ((long) ((1 << offset) - 1) & this.value >>> this.offset);
    }

    public boolean readBoolean() {
        boolean b = true;
        if (this.read(1) == 0) {
            b = false;
        }
        return b;
    }
}
