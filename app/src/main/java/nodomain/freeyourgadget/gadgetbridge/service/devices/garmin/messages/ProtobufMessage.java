package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GenericStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage.ProtobufChunkStatus.KEPT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.ProtobufStatusMessage.ProtobufStatusCode.NO_ERROR;

public class ProtobufMessage extends GFDIMessage {


    private final int requestId;
    private final int dataOffset;
    private final int totalProtobufLength;
    private final int protobufDataLength;
    private final byte[] messageBytes;
    private final boolean sendOutgoing;

    public ProtobufMessage(GarminMessage garminMessage, int requestId, int dataOffset, int totalProtobufLength, int protobufDataLength, byte[] messageBytes, boolean sendOutgoing) {
        this.garminMessage = garminMessage;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.totalProtobufLength = totalProtobufLength;
        this.protobufDataLength = protobufDataLength;
        this.messageBytes = messageBytes;
        this.sendOutgoing = sendOutgoing;

        if (isComplete()) {
            this.statusMessage = new GenericStatusMessage(garminMessage, GFDIMessage.Status.ACK);
        } else {
            this.statusMessage = new ProtobufStatusMessage(garminMessage, GFDIMessage.Status.ACK, requestId, dataOffset, KEPT, NO_ERROR);
        }
    }

    public ProtobufMessage(GarminMessage garminMessage, int requestId, int dataOffset, int totalProtobufLength, int protobufDataLength, byte[] messageBytes) {
        this(garminMessage, requestId, dataOffset, totalProtobufLength, protobufDataLength, messageBytes, true);
    }

    public void setStatusMessage(ProtobufStatusMessage protobufStatusMessage) {
        this.statusMessage = protobufStatusMessage;
    }

    public static ProtobufMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final int totalProtobufLength = reader.readInt();
        final int protobufDataLength = reader.readInt();
        final byte[] messageBytes = reader.readBytes(protobufDataLength);

        return new ProtobufMessage(garminMessage, requestID, dataOffset, totalProtobufLength, protobufDataLength, messageBytes, false);
    }

    public int getRequestId() {
        return requestId;
    }

    public GarminMessage getMessageType() {
        return garminMessage;
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
        writer.writeShort(garminMessage.getId());
        writer.writeShort(requestId);
        writer.writeInt(dataOffset);
        writer.writeInt(totalProtobufLength);
        writer.writeInt(protobufDataLength);
        writer.writeBytes(messageBytes);
        return sendOutgoing;
    }

}
