package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util;

import java.util.Arrays;

public class ByteArrayWriter {
    public byte[] byteArray;
    private int bytesWritten;

    public ByteArrayWriter() {
        this.bytesWritten = 0;
    }

    private void addIntToValue(long n, IntFormat intFormat) {
        for (int i = 0; i < intFormat.bytesCount; ++i) {
            this.byteArray[this.bytesWritten++] = (byte) (n >> i * 8 & 0xFFL);
        }
    }

    public void appendUint16(int n) {
        this.appendValue(n, IntFormat.UINT16);
    }

    public void appendUint32(long n) {
        this.appendValue(n, IntFormat.UINT32);
    }

    public void appendUint8(int n) {
        this.appendValue(n, IntFormat.UINT8);
    }

    public void appendValue(long lng, IntFormat intFormat) {
        if (intFormat == null) {
            throw new IllegalArgumentException("wrong int format");
        }
        if (lng > intFormat.max || lng < intFormat.min) {
            throw new IllegalArgumentException("wrong value for intFormat. max: " + intFormat.max + " min: " + intFormat.min + " value: " + lng);
        }
        this.increaseByteArray(intFormat.bytesCount);
        long n = lng;
        if (intFormat.isSigned) {
            int n2 = intFormat.bytesCount * 8;
            n = lng;
            if (lng < 0L) {
                n = (1 << n2 - 1) + ((long) ((1 << n2 - 1) - 1) & lng);
            }
        }
        this.addIntToValue(n, intFormat);
    }

    public void increaseByteArray(int n) {
        if (this.byteArray == null) {
            this.byteArray = new byte[n];
            return;
        }
        this.byteArray = Arrays.copyOf(this.byteArray, this.byteArray.length + n);
    }

    public byte[] getByteArray() {
        return this.byteArray.clone();
    }
}
