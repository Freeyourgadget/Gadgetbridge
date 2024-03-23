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
        int i = (byteBuffer.get() + offset) / scale;
        if (i < min || i > max)
            return invalid;
        return i;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        int i = ((Number) o).intValue() * scale - offset;
        if (!unsigned && (i < min || i > max)) {
            byteBuffer.put((byte) invalid);
            return;
        }
        byteBuffer.put((byte) i);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.put((byte) invalid);
    }

}
