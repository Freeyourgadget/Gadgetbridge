package nodomain.freeyourgadget.gadgetbridge.util.protobuf;

import java.io.ByteArrayOutputStream;

public class ProtobufUtils {
    public static byte[] encode_varint(int value){
        if(value == 0){
            return new byte[]{0x00};
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4);

        while(value > 0){
            byte newByte = (byte)(value & 0b01111111);
            value >>= 7;
            if(value != 0){
                newByte |= 0b10000000;
            }
            bytes.write(newByte);
        }

        return bytes.toByteArray();
    }
}
