package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import androidx.annotation.Nullable;

public class ProtobufStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final int requestId;
    private final int dataOffset;
    private final ProtobufStatusCode protobufStatus;
    private final ProtobufStatusCode error; //TODO: why is this duplicated?
    private final int messageType;
    private final boolean sendOutgoing;

    public ProtobufStatusMessage(int messageType, Status status, int requestId, int dataOffset, ProtobufStatusCode protobufStatus, ProtobufStatusCode error) {
        this(messageType, status, requestId, dataOffset, protobufStatus, error, true);
    }
    public ProtobufStatusMessage(int messageType, Status status, int requestId, int dataOffset, ProtobufStatusCode protobufStatus, ProtobufStatusCode error, boolean sendOutgoing) {
        this.messageType = messageType;
        this.status = status;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.protobufStatus = protobufStatus;
        this.error = error;
        this.sendOutgoing = sendOutgoing;
    }

    public static ProtobufStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final Status status = Status.fromCode(reader.readByte());
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final ProtobufStatusCode protobufStatus = ProtobufStatusCode.fromCode(reader.readByte());
        final ProtobufStatusCode error = ProtobufStatusCode.fromCode(reader.readByte());

        reader.warnIfLeftover();
        return new ProtobufStatusMessage(messageType, status, requestID, dataOffset, protobufStatus, error, false);
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public ProtobufStatusCode getProtobufStatus() {
        return protobufStatus;
    }

    public ProtobufStatusCode getError() {
        return error;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getRequestId() {
        return requestId;
    }

    public boolean isOK() {
        return this.status.equals(Status.ACK) &&
                this.protobufStatus.equals(ProtobufStatusCode.NO_ERROR) &&
                this.error.equals(ProtobufStatusCode.NO_ERROR);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType);
        writer.writeByte(status.ordinal());
        writer.writeShort(requestId);
        writer.writeInt(dataOffset);
        writer.writeByte(protobufStatus.code);
        writer.writeByte(error.code);
        return sendOutgoing;
    }

    protected Status getStatus() {
        return status;
    }

    public enum ProtobufStatusCode {
        NO_ERROR(0),
        UNKNOWN_ERROR(1),
        UNKNOWN_REQUEST_ID(100),
        DUPLICATE_PACKET(101),
        MISSING_PACKET(102),
        EXCEEDED_TOTAL_PROTOBUF_LENGTH(103),
        PROTOBUF_PARSE_ERROR(200),
        ;

        private final int code;

        ProtobufStatusCode(final int code) {
            this.code = code;
        }

        @Nullable
        public static ProtobufStatusCode fromCode(final int code) {
            for (final ProtobufStatusCode protobufStatusCode : ProtobufStatusCode.values()) {
                if (protobufStatusCode.getCode() == code) {
                    return protobufStatusCode;
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }
    }


}
