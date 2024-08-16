package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class BaseTypeLong implements BaseTypeInterface {
    private final int size = 8;
    private final BigInteger min;
    private final BigInteger max;
    private final long invalid;
    private final boolean unsigned;

    BaseTypeLong(boolean unsigned, long invalid) {
        if (unsigned) {
            this.min = BigInteger.valueOf(0);
            this.max = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
        } else {
            this.min = BigInteger.valueOf(Long.MIN_VALUE);
            this.max = BigInteger.valueOf(Long.MAX_VALUE);
        }
        this.invalid = invalid;
        this.unsigned = unsigned;
    }

    public int getByteSize() {
        return size;
    }

    @Override
    public Object decode(ByteBuffer byteBuffer, double scale, int offset) {
        BigInteger i = unsigned ? BigInteger.valueOf(byteBuffer.getLong() & 0xFFFFFFFFFFFFFFFFL) : BigInteger.valueOf(byteBuffer.getLong());
        if (!unsigned && (i.compareTo(min) < 0 || i.compareTo(max) > 0))
            return null;
        if (i.compareTo(BigInteger.valueOf(invalid)) == 0)
            return null;
        return new BigDecimal(i).divide(BigDecimal.valueOf(scale)).subtract(BigDecimal.valueOf(offset)).toBigInteger().longValue();
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        if (null == o) {
            invalidate(byteBuffer);
            return;
        }
        BigInteger i = new BigDecimal(((Number) o).longValue()).multiply(BigDecimal.valueOf(scale)).add(BigDecimal.valueOf(offset)).toBigInteger();
        if (!unsigned && (i.compareTo(min) < 0 || i.compareTo(max) > 0)) {
            invalidate(byteBuffer);
            return;
        }
        byteBuffer.putLong(i.longValue());
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        byteBuffer.putLong((long) invalid);
    }

}
