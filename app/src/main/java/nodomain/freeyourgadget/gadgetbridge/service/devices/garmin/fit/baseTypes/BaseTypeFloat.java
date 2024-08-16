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
    public Object decode(ByteBuffer byteBuffer, double scale, int offset) {
        float f = byteBuffer.getFloat();
        if (f < min || f > max) {
            return null;
        }
        if (Float.isNaN(f) || f == invalid)
            return null;
        return (float) (f / scale) - offset;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        float f = (float) ((((Number) o).floatValue() + offset) * scale);
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
