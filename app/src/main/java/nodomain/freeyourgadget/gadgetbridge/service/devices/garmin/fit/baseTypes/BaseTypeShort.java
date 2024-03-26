package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeShort implements BaseTypeInterface {
    private final int min;
    private final int max;
    private final int invalid;
    private final boolean unsigned;
    private final int size = 2;

    BaseTypeShort(boolean unsigned, int invalid) {
        if (unsigned) {
            this.min = 0;
            this.max = 0xffff;
        } else {
            this.min = Short.MIN_VALUE;
            this.max = Short.MAX_VALUE;
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, int scale, int offset) {
        int s = unsigned ? Short.toUnsignedInt(byteBuffer.getShort()) : byteBuffer.getShort();
        if (s < min || s > max)
            return null;
        if (s == invalid)
            return null;
        return (s + offset) / scale;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        int i = ((Number) o).intValue() * scale - offset;
        if (i < min || i > max) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putShort((short) i);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putShort((short) invalid);
    }
}
