package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.util.protobuf.ProtobufUtils;

public class StringMessageField extends MessageField{
    String fieldValueString;

    public StringMessageField(int fieldNumber, String value) {
        super(fieldNumber, FieldType.LENGTH_DELIMITED);
        fieldValueString = value;
    }

    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {
        os.write(getStartBytes());

        os.write(ProtobufUtils.encode_varint(fieldValueString.length()));

        os.write(fieldValueString.getBytes());
    }
}
