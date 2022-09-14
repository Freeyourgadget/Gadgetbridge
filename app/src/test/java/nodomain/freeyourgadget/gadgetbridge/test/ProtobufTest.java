package nodomain.freeyourgadget.gadgetbridge.test;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.NestedMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.RootMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.StringMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields.VarintMessageField;
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.ProtobufUtils;

public class ProtobufTest extends TestCase {
    private void assertEncodeVarint(byte[] expected, int valueToEncode){
        byte[] encoded = ProtobufUtils.encode_varint(valueToEncode);
        assertEquals(expected.length, encoded.length);

        for(int i = 0; i < expected.length; i++){
            assertEquals(expected[i], encoded[i]);
        }
    }

    public void testEncodeVarint(){
        assertEncodeVarint(new byte[]{0x00}, 0);
        assertEncodeVarint(new byte[]{0x01}, 1);
        assertEncodeVarint(new byte[]{0b1111111}, 0b1111111);
        assertEncodeVarint(new byte[]{(byte)0b11111111, 0b00000001}, 0b11111111);
        assertEncodeVarint(new byte[]{(byte)0xeb, (byte)0x86, (byte)0x4e}, 1278827);
        assertEncodeVarint(new byte[]{(byte)0xf9, (byte) 0xa5 , (byte) 0x3c}, 987897);
        assertEncodeVarint(new byte[]{(byte) 0xfd, (byte) 0xe5, (byte) 0x90, (byte) 0x01}, 2372349);
    }

    private void assertEncodeVarintMessageField(byte[] expected, int fieldId, int value) throws IOException {
        VarintMessageField field = new VarintMessageField(fieldId, value);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        field.encode(os);
        byte[] result = os.toByteArray();

        assertEquals(expected.length, result.length);

        for(int i = 0; i < expected.length; i++){
            assertEquals(expected[i], result[i]);
        }
    }

    public void testEncodeVarintMessageField() throws IOException {
        assertEncodeVarintMessageField(new byte[]{0b00001000, 0b00000000}, 1, 0);
        assertEncodeVarintMessageField(new byte[]{(byte) 0b10000000, 0b00000001, 0b01111111}, 16, 127);
        assertEncodeVarintMessageField(new byte[]{(byte) 0b10000000, 0b00000001, (byte) 0xfd, (byte) 0xe5, (byte) 0x90, (byte) 0x01}, 16, 2372349);
    }

    private void assertEncodeStringMessageField(byte[] expected, int fieldId, String value) throws IOException {
        StringMessageField field = new StringMessageField(fieldId, value);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        field.encode(os);
        byte[] result = os.toByteArray();

        assertEquals(expected.length, result.length);

        for(int i = 0; i < expected.length; i++){
            assertEquals(expected[i], result[i]);
        }
    }

    private void assertEncodeRootMessageField(byte[] expected, RootMessageField value) throws IOException {
        byte[] result = value.encodeToBytes();
        
        System.out.println(StringUtils.bytesToHex(result));

        assertEquals(expected.length, result.length);

        for(int i = 0; i < expected.length; i++){
            assertEquals(expected[i], result[i]);
        }
    }

    public void testEncodeStringMessageField() throws IOException {
        assertEncodeStringMessageField(new byte[]{0b00001010, 0b00000000}, 1, "");
        assertEncodeStringMessageField(new byte[]{0b01010010, 0x04, 0x74, 0x65, 0x73, 0x74}, 10, "test");
    }

    public void testNestedMessageField() throws IOException {
        RootMessageField startAppRequest = new RootMessageField(
                new VarintMessageField(1, 2),
                new NestedMessageField(16,
                        new StringMessageField(1, "Infrared"),
                        new StringMessageField(2, "RPC")
                )
        );
        assertEncodeRootMessageField(
                new byte[]{(byte) 0x14, (byte) 0x08, (byte) 0x02, (byte) 0x82, (byte) 0x01, (byte) 0x0f, (byte) 0x0a, (byte) 0x08, (byte) 0x49, (byte) 0x6e, (byte) 0x66, (byte) 0x72, (byte) 0x61, (byte) 0x72, (byte) 0x65, (byte) 0x64, (byte) 0x12, (byte) 0x03, (byte) 0x52, (byte) 0x50, (byte) 0x43},
                startAppRequest
        );

        RootMessageField loadFileRequest = new RootMessageField(
                new VarintMessageField(1, 3),
                new NestedMessageField(48,
                        new StringMessageField(1, "/any/infrared/Remote.ir")
                )
        );
        assertEncodeRootMessageField(
                new byte[]{(byte) 0x1e, (byte) 0x08, (byte) 0x03, (byte) 0x82, (byte) 0x03, (byte) 0x19, (byte) 0x0a, (byte) 0x17, (byte) 0x2f, (byte) 0x61, (byte) 0x6e, (byte) 0x79, (byte) 0x2f, (byte) 0x69, (byte) 0x6e, (byte) 0x66, (byte) 0x72, (byte) 0x61, (byte) 0x72, (byte) 0x65, (byte) 0x64, (byte) 0x2f, (byte) 0x52, (byte) 0x65, (byte) 0x6d, (byte) 0x6f, (byte) 0x74, (byte) 0x65, (byte) 0x2e, (byte) 0x69, (byte) 0x72},
                loadFileRequest
        );

        RootMessageField pressButtonRequest = new RootMessageField(
                new VarintMessageField(1, 4),
                new NestedMessageField(49,
                        new StringMessageField(1, "Pwr")
                )
        );
        assertEncodeRootMessageField(new byte[]{(byte) 0x0a, (byte) 0x08, (byte) 0x04, (byte) 0x8a, (byte) 0x03, (byte) 0x05, (byte) 0x0a, (byte) 0x03, (byte) 0x50, (byte) 0x77, (byte) 0x72}, pressButtonRequest);

        RootMessageField releaseButtonRequest = new RootMessageField(
                new VarintMessageField(1, 5),
                new NestedMessageField(50)
        );
        assertEncodeRootMessageField(new byte[]{(byte) 0x05, (byte) 0x08, (byte) 0x05, (byte) 0x92, (byte) 0x03, (byte) 0x00}, releaseButtonRequest);

        RootMessageField exitAppRequest = new RootMessageField(
                new VarintMessageField(1, 6),
                new NestedMessageField(47)
        );
        assertEncodeRootMessageField(new byte[]{(byte) 0x05, (byte) 0x08, (byte) 0x06, (byte) 0xfa, (byte) 0x02, (byte) 0x00}, exitAppRequest);

    }
}
