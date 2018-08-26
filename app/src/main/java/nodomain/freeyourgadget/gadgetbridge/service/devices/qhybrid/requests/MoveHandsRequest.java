package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;


import java.nio.ByteBuffer;

public class MoveHandsRequest extends Request {
    public MoveHandsRequest(boolean moveRelative, short degreesMin, short degreesHour, short degreesSub){
        init(moveRelative, degreesMin, degreesHour, degreesSub);
    }

    private void init(boolean moveRelative, short degreesMin, short degreesHour, short degreesSub) {
        int count = 0;
        if(degreesHour != -1) count++;
        if(degreesMin != -1) count++;
        if(degreesSub != -1) count++;

        ByteBuffer buffer = createBuffer(count * 5 + 5);
        buffer.put(moveRelative ? 1 : (byte)2);
        buffer.put((byte)count);

        if(degreesHour > -1){
            buffer.put((byte)1);
            buffer.putShort(degreesHour);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }
        if(degreesMin > -1){
            buffer.put((byte)2);
            buffer.putShort(degreesMin);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }
        if(degreesSub > -1){
            buffer.put((byte)3);
            buffer.putShort(degreesSub);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 3};
    }
}
