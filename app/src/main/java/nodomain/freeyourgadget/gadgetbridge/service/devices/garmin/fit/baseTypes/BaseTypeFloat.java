package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeFloat implements BaseTypeInterface {
    private final int size = 4;
    private final double min;
    private final double max;
    private final double invalid;

    BaseTypeFloat() {
        this.min = -Float.MAX_VALUE;
        this.max = Float.MAX_VALUE;
        this.invalid = Float.intBitsToFloat(0xFFFFFFFF);
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(ByteBuffer byteBuffer, int scale, int offset) {
        float f = byteBuffer.getFloat();
        if (f < min || f > max) {
            return null;
        }
        if (Float.isNaN(f) || f == invalid)
            return null;
        return (f + offset) / scale;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        float f = ((Number) o).floatValue() * scale - offset;
        if (f < min || f > max) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putFloat((float) f);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putFloat((float) invalid);
    }

}
