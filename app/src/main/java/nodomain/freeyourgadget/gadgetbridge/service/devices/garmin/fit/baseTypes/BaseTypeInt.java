package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeInt implements BaseTypeInterface {
    private final long min;
    private final long max;
    private final long invalid;
    private final boolean unsigned;
    private final int size = 4;

    BaseTypeInt(boolean unsigned, long invalid) {
        if (unsigned) {
            this.min = 0;
            this.max = 0xffffffffL;
        } else {
            this.min = Integer.MIN_VALUE;
            this.max = Integer.MAX_VALUE;
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, double scale, int offset) {
        long i = unsigned ? Integer.toUnsignedLong(byteBuffer.getInt()) : byteBuffer.getInt();
        if (i < min || i > max)
            return null;
        if (i == invalid)
            return null;
        return (Math.round(i / scale) - offset);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        long l = (long) ((((Number) o).longValue() + offset) * scale);
        if (l < min || l > max) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putInt((int) l);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putInt((int) invalid);
    }

}
