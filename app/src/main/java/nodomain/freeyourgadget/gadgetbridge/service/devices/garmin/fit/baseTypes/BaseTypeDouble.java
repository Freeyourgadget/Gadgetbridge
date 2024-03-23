package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeDouble implements BaseTypeInterface {
    private final int size = 8;
    private final double min;
    private final double max;
    private final double invalid;

    BaseTypeDouble() {
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
        this.invalid = Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL);
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, int scale, int offset) {
        return (byteBuffer.getDouble() + offset) / scale;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        double d = ((Number) o).doubleValue() * scale - offset;
        if (d < min || d > max) {
            byteBuffer.putDouble(invalid);
            return;
        }
        byteBuffer.putDouble(d);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putDouble(invalid);
    }
}
