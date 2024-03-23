package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeLong implements BaseTypeInterface {
    private final int size = 8;
    private final double min;
    private final double max;
    private final double invalid;
    private final boolean unsigned;

    BaseTypeLong(boolean unsigned, long invalid) {
        if (unsigned) {
            this.min = 0;
            this.max = 0xFFFFFFFFFFFFFFFFL;
        } else {
            this.min = Long.MIN_VALUE;
            this.max = Long.MAX_VALUE;
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(ByteBuffer byteBuffer, int scale, int offset) {
        if (unsigned) {
            return ((byteBuffer.getLong() & 0xFFFFFFFFFFFFFFFFL + offset) / scale);
        } else {
            long l = (byteBuffer.getLong() + offset) / scale;
            if (l < min || l > max)
                return invalid;
            return l;
        }
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        long l = ((Number) o).longValue() * scale - offset;
        if (!unsigned && (l < min || l > max)) {
            byteBuffer.putLong((long) invalid);
            return;
        }
        byteBuffer.putLong(l);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putLong((long) invalid);
    }

}
