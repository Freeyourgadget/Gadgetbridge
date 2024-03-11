package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


public abstract class GFDIStatusMessage extends GFDIMessage {
    Status status;

    public static GFDIStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final int requestMessageType = reader.readShort();
        if (GarminMessage.PROTOBUF_REQUEST.getId() == requestMessageType || GarminMessage.PROTOBUF_RESPONSE.getId() == requestMessageType) {
            return ProtobufStatusMessage.parseIncoming(reader, messageType);
        } else {
            final Status status = Status.fromCode(reader.readByte());

            reader.warnIfLeftover();
            return new GenericStatusMessage(messageType, status);
        }
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

    protected Status getStatus() {
        return status;
    }

}
