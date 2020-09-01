package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util;

public class ByteArrayReader {
    public final byte[] byteArray;
    public int bytesRead;

    public ByteArrayReader(byte[] array) {
        this.bytesRead = 0;
        if (array == null || array.length <= 0) {
            throw new IllegalArgumentException("wrong byte array");
        }
        this.byteArray = array.clone();
    }

    public int getBytesLeft() {
        return this.byteArray.length - this.bytesRead;
    }

    public long readInt(IntFormat intFormat) {
        if (intFormat == null) {
            throw new IllegalArgumentException("wrong intFormat");
        }
        int i = 0;
        long n = 0L;
        try {
            while (i < intFormat.bytesCount) {
                long n2 = this.byteArray[this.bytesRead++] & 0xFF;
                int n3 = i + 1;
                n += n2 << i * 8;
                i = n3;
            }
            long n4 = n;
            if (intFormat.isSigned) {
                int n5 = intFormat.bytesCount * 8;
                n4 = n;
                if (((long) (1 << n5 - 1) & n) != 0x0L) {
                    n4 = ((1 << n5 - 1) - (n & (long) ((1 << n5 - 1) - 1))) * -1L;
                }
            }
            return n4;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new RuntimeException("reading outside of byte array", ex.getCause());
        }
    }

    public int readUint16() {
        return (int) this.readInt(IntFormat.UINT16);
    }

    public int readUint8() {
        return (int) this.readInt(IntFormat.UINT8);
    }
}
