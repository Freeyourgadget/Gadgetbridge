package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;

public interface FieldInterface {
    Object decode(ByteBuffer byteBuffer);

    void encode(ByteBuffer byteBuffer, Object o);

    void invalidate(ByteBuffer byteBuffer);

}
