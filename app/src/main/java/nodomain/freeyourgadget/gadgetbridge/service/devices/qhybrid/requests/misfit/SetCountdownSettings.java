package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;

public class SetCountdownSettings extends Request {
    public SetCountdownSettings(int startTime, int endTime, short offset) {
        ByteBuffer buffer = createBuffer();

        buffer.putInt(startTime);
        buffer.putInt(endTime);
        buffer.putShort(offset);
        // buff

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 19, 1};
    }

    @Override
    public int getPayloadLength() {
        return 13;
    }
}
