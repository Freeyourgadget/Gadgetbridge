package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeByte implements BaseTypeInterface {

    private final int min;
    private final int max;
    private final int invalid;
    private final boolean unsigned;
    private final int size = 1;

    BaseTypeByte(boolean unsigned, int invalid) {
        if (unsigned) {
            min = 0;
            max = 0xff;
        } else {
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }


    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, int scale, int offset) {
        int b = unsigned ? Byte.toUnsignedInt(byteBuffer.get()) : byteBuffer.get();
        if (b < min || b > max)
            return null;
        if (b == invalid)
            return null;
        return (b + offset) / scale;
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
        byteBuffer.put((byte) i);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.put((byte) invalid);
    }

}
