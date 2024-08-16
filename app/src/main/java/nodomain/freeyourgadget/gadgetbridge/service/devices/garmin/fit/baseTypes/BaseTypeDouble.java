package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeDouble implements BaseTypeInterface {
    private final int size = 8;
    private final double min;
    private final double max;
    private final double invalid;

    BaseTypeDouble() {
        this.min = -Double.MAX_VALUE;
        this.max = Double.MAX_VALUE;
        this.invalid = Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL);
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer, double scale, int offset) {
        double d = byteBuffer.getDouble();
        if (d < min || d > max) {
            return null;
        }
        if (Double.isNaN(d) || d == invalid)
            return null;
        return (d / scale) - offset;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        double d = (((Number) o).doubleValue() + offset) * scale;
        if (d < min || d > max) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putDouble(d);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putDouble(invalid);
    }
}
