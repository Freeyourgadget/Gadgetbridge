package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RequestHandControlRequest extends Request {
    public RequestHandControlRequest(byte priority, boolean moveCompleteNotify, boolean controlLostNOtify){
        super();
        init(priority, moveCompleteNotify, controlLostNOtify);
    }

    public RequestHandControlRequest(){
        super();
        init((byte)1, false, false);
    }

    private void init(byte priority, boolean moveCompleteNotify, boolean controlLostNOtify) {
        ByteBuffer buffer = createBuffer();
        buffer.put(priority);
        buffer.put(moveCompleteNotify ? (byte)1 : 0);
        buffer.put(controlLostNOtify ? (byte)1 : 0);
        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 1};
    }

    @Override
    public int getPayloadLength() {
        return 6;
    }
}
