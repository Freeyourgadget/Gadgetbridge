package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.util.protobuf.ProtobufUtils;

public class NestedMessageField extends MessageField{
    MessageField[] children;

    public NestedMessageField(int fieldNumber, MessageField... children) {
        super(fieldNumber, FieldType.LENGTH_DELIMITED);
        this.children = children;
    }

    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {
        ByteArrayOutputStream childStream = new ByteArrayOutputStream();
        for(MessageField child : children){
            child.encode(childStream);
        }

        os.write(getStartBytes());
        os.write(ProtobufUtils.encode_varint(childStream.size()));
        os.write(childStream.toByteArray());
    }
}
