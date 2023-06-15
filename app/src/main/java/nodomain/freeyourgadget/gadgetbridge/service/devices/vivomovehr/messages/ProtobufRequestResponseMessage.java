package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class ProtobufRequestResponseMessage {
    public static final int NO_ERROR = 0;
    public static final int UNKNOWN_REQUEST_ID = 100;
    public static final int DUPLICATE_PACKET = 101;
    public static final int MISSING_PACKET = 102;
    public static final int EXCEEDED_TOTAL_PROTOBUF_LENGTH = 103;
    public static final int PROTOBUF_PARSE_ERROR = 200;
    public static final int UNKNOWN_PROTOBUF_MESSAGE = 201;

    public final int status;
    public final int requestId;
    public final int dataOffset;
    public final int protobufStatus;
    public final int error;

    public ProtobufRequestResponseMessage(int status, int requestId, int dataOffset, int protobufStatus, int error) {
        this.status = status;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.protobufStatus = protobufStatus;
        this.error = error;
    }

    public static ProtobufRequestResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestMessageID = reader.readShort();
        final int status = reader.readByte();
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final int protobufStatus = reader.readByte();
        final int error = reader.readByte();

        return new ProtobufRequestResponseMessage(status, requestID, dataOffset, protobufStatus, error);
    }
}
