package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;

public class SetCurrentStepCountRequest extends Request {
    public SetCurrentStepCountRequest(int steps){
        super();
        ByteBuffer buffer = createBuffer();
        buffer.putInt(steps);
        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 6;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 17};
    }
}
