package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;

public class SetStepGoalRequest extends Request {
    public SetStepGoalRequest(int goal){
        super();
        init(goal);
    }

    private void init(int goal) {
        ByteBuffer buffer = createBuffer();
        buffer.putInt(goal);
        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 6;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 16};
    }
}
