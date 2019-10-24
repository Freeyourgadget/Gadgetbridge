package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;
import java.util.UUID;

public class EventStreamRequest extends Request {
    public EventStreamRequest(short handle) {
        super();
        ByteBuffer buffer = createBuffer();
        buffer.putShort(1, handle);
        this.data = buffer.array();
    }


    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0006-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1};
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }
}
