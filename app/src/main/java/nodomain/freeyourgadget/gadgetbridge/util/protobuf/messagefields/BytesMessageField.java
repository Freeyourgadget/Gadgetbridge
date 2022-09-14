package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BytesMessageField extends MessageField{
    private byte[] fieldValueBytes;

    protected BytesMessageField(int fieldNumber, byte[] value) {
        super(fieldNumber, FieldType.LENGTH_DELIMITED);
        fieldValueBytes = value;
    }

    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {

    }
}
