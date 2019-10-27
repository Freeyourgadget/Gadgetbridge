package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class SetCurrentTimeServiceRequest extends Request {
    public SetCurrentTimeServiceRequest(int timeStampSecs, short millis, short offsetInMins){
        super();
        init(timeStampSecs, millis, offsetInMins);
    }
    private void init(int timeStampSecs, short millis, short offsetInMins){
        ByteBuffer buffer = createBuffer();
        buffer.putInt(timeStampSecs);
        buffer.putShort(millis);
        buffer.putShort(offsetInMins);
        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 11;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 18, 2};
    }
}
