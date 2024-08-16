package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

public interface BaseTypeInterface {
    int getByteSize();

    Object decode(ByteBuffer byteBuffer, double scale, int offset);

    void encode(ByteBuffer byteBuffer, Object o, double scale, int offset);

    void invalidate(ByteBuffer byteBuffer);
}
