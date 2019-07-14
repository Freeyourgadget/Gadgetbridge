package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import java.nio.ByteBuffer;

public class PlayNotificationRequest extends Request {

    public enum VibrationType{
        SINGLE_SHORT(3),
        DOUBLE_SHORT(2),
        TRIPLE_SHORT(1),
        SINGLE_NORMAL(5),
        DOUBLE_NORMAL(6),
        TRIPLE_NORMAL(7),
        SINGLE_LONG(8),
        NO_VIBE(9);

        public byte value;

        VibrationType(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return value;
        }
    }

    public PlayNotificationRequest(int vibrationType, int degreesHour, int degreesMins){
        int length = 0;
        if(degreesHour > -1) length++;
        if(degreesMins > -1) length++;
        ByteBuffer buffer = createBuffer(length * 2 + 10);
        buffer.put((byte)vibrationType);
        buffer.put((byte)5);
        buffer.put((byte)(length * 2 + 2));
        buffer.putShort((short)0);
        if(degreesHour > -1){
            buffer.putShort((short) ((degreesHour % 360) | (1 << 12)));
        }
        if(degreesMins > -1){
            buffer.putShort((short)((degreesMins % 360) | (2 << 12)));
        }
        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 7, 15, 10, 1};
    }
}
