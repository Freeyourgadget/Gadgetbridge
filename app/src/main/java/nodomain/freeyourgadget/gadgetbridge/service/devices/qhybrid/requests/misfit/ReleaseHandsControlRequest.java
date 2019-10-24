package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;


import java.nio.ByteBuffer;

public class ReleaseHandsControlRequest extends Request {
    public ReleaseHandsControlRequest(){
        super();
        init((short)0);
    }

    private void init(short delayBeforeRelease) {
        ByteBuffer buffer = createBuffer();
        buffer.putShort(3, delayBeforeRelease);
        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 2};
    }

    @Override
    public int getPayloadLength() {
        return 5;
    }
}
