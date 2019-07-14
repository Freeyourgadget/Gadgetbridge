package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

public class AnimationRequest extends Request{
    @Override
    public byte[] getStartSequence() {
        return new byte[]{(byte)2, (byte) -15, (byte)5};
    }
}
