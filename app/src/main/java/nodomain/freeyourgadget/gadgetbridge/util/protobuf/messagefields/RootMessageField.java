package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

public class RootMessageField extends NestedMessageField{
    public RootMessageField(MessageField... children) {
        super(0, children);
    }

    @Override
    protected byte[] getStartBytes() {
        return new byte[0];
    }

    /*
    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {
        ByteArrayOutputStream childrenOs = new ByteArrayOutputStream();
        super.encode(childrenOs);

        os.write(utils.encode_varint(childrenOs.size()));
        os.write(childrenOs.toByteArray());
    }
    */
}
