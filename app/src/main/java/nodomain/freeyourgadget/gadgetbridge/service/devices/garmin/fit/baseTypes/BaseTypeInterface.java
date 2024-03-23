package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public interface BaseTypeInterface {
    int getByteSize();

    Object decode(ByteBuffer byteBuffer, int scale, int offset);

    void encode(ByteBuffer byteBuffer, Object o, int scale, int offset);

    void invalidate(ByteBuffer byteBuffer);
}
