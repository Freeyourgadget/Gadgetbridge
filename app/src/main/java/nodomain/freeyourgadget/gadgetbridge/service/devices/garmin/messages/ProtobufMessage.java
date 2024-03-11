package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.ProtobufStatusMessage.ProtobufStatusCode.NO_ERROR;

public class ProtobufMessage extends GFDIMessage {


    private final int requestId;
    private final int messageType;
    private final int dataOffset;
    private final int totalProtobufLength;
    private final int protobufDataLength;
    private final byte[] messageBytes;
    private final boolean sendOutgoing;

    public ProtobufMessage(int messageType, int requestId, int dataOffset, int totalProtobufLength, int protobufDataLength, byte[] messageBytes) {
        this(messageType, requestId, dataOffset, totalProtobufLength, protobufDataLength, messageBytes, true);
    }

    public ProtobufMessage(int messageType, int requestId, int dataOffset, int totalProtobufLength, int protobufDataLength, byte[] messageBytes, boolean sendOutgoing) {
        this.messageType = messageType;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.totalProtobufLength = totalProtobufLength;
        this.protobufDataLength = protobufDataLength;
        this.messageBytes = messageBytes;
        this.sendOutgoing = sendOutgoing;

        if (isComplete()) {
            this.statusMessage = new GenericStatusMessage(messageType, GFDIMessage.Status.ACK);
        } else {
            this.statusMessage = new ProtobufStatusMessage(messageType, GFDIMessage.Status.ACK, requestId, dataOffset, NO_ERROR, NO_ERROR);
        }
    }

    public static ProtobufMessage parseIncoming(MessageReader reader, int messageType) {
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final int totalProtobufLength = reader.readInt();
        final int protobufDataLength = reader.readInt();
        final byte[] messageBytes = reader.readBytes(protobufDataLength);

        reader.warnIfLeftover();
        return new ProtobufMessage(messageType, requestID, dataOffset, totalProtobufLength, protobufDataLength, messageBytes, false);
    }

    public int getRequestId() {
        return requestId;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getTotalProtobufLength() {
        return totalProtobufLength;
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    public boolean isChunked() {
        return (totalProtobufLength != protobufDataLength);
    }

    public boolean isComplete() {
        return (dataOffset == 0 && !isChunked());
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(messageType);
        writer.writeShort(requestId);
        writer.writeInt(dataOffset);
        writer.writeInt(totalProtobufLength);
        writer.writeInt(protobufDataLength);
        writer.writeBytes(messageBytes);
        return sendOutgoing;
    }

}
