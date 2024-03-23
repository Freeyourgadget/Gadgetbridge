package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public class BaseTypeFloat implements BaseTypeInterface {
    private final int size = 4;
    private final double min;
    private final double max;
    private final double invalid;
    private final boolean unsigned;

    BaseTypeFloat() {
        this.min = -Float.MAX_VALUE;
        this.max = Float.MAX_VALUE;
        this.invalid = Float.intBitsToFloat(0xFFFFFFFF);
        this.unsigned = false;
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(ByteBuffer byteBuffer, int scale, int offset) {
        return (byteBuffer.getFloat() + offset) / scale;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        float f = ((Number) o).floatValue() * scale - offset;
        if (!unsigned && (f < min || f > max)) {
            byteBuffer.putFloat((float) invalid);
            return;
        }
        byteBuffer.putFloat((float) f);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putFloat((float) invalid);
    }

}
