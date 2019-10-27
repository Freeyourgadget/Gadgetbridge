package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class SetTimeRequest extends Request {
    public SetTimeRequest(int epochSeconds, short millis, short minutesOffset) {
        ByteBuffer buffer = createBuffer();
        buffer.putInt(epochSeconds);
        buffer.putShort(millis);
        buffer.putShort(minutesOffset);

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 4};
    }

    @Override
    public int getPayloadLength() {
        return 10;
    }
}
