package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;


import java.nio.ByteBuffer;

public class VibrateRequest extends Request {

    public VibrateRequest(boolean longVibration, short repeats, short millisBetween){
        ByteBuffer buffer = createBuffer();

        buffer.put(longVibration ? (byte)1 : 0);
        buffer.put((byte) repeats);
        buffer.putShort(millisBetween);
        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 7;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 15, 5};
    }
}
