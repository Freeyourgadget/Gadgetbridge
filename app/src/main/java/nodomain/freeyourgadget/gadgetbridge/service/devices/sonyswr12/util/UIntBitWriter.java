package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util;

public class UIntBitWriter {
    private long value;
    private long offset;

    public UIntBitWriter(int offset) {
        this.value = 0L;
        this.offset = offset;
    }

    public void append(int offset, int value) {
        if (value < 0 || value > (1 << offset) - 1) {
            throw new IllegalArgumentException("value is out of range: " + value);
        }
        this.offset -= offset;
        if (this.offset < 0L) {
            throw new IllegalArgumentException("Write offset out of range");
        }
        this.value |= (long) value << (int) this.offset;
    }

    public void appendBoolean(boolean b) {
        if (b) {
            this.append(1, 1);
            return;
        }
        this.append(1, 0);
    }

    public long getValue() {
        if (this.offset != 0L) {
            throw new IllegalStateException("value is not complete yet: " + this.offset);
        }
        return this.value;
    }
}