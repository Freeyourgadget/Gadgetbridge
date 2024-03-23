package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeInt implements BaseTypeInterface {
    private final int min;
    private final int max;
    private final int invalid;
    private final boolean unsigned;
    private final int size = 4;

    BaseTypeInt(boolean unsigned, int invalid) {
        if (unsigned) {
            this.min = 0;
            this.max = 0xffffffff;
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
    public Object decode(final ByteBuffer byteBuffer, int scale, int offset) {
        if (unsigned) {
            long i = ((byteBuffer.getInt() & 0xffffffffL) + offset) / scale;
            return i;
        } else {
            int i = (byteBuffer.getInt() + offset) / scale;
            if (i < min || i > max)
                return invalid;
            return i;
        }

    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        long l = ((Number) o).longValue() * scale - offset;
        if (!unsigned && (l < min || l > max)) {
            byteBuffer.putInt((int) invalid);
            return;
        }
        byteBuffer.putInt((int) l);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putInt((int) invalid);
    }

}
