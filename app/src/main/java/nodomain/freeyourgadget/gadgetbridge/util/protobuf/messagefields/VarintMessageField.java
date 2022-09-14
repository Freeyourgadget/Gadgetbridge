package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.util.protobuf.ProtobufUtils;

public class VarintMessageField extends MessageField{
    int fieldValueInt;

    public VarintMessageField(int fieldNumber, int value){
        super(fieldNumber, FieldType.VARINT);
        this.fieldValueInt = value;
    }



    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {
        os.write(getStartBytes());

        os.write(ProtobufUtils.encode_varint(fieldValueInt));
    }
}
