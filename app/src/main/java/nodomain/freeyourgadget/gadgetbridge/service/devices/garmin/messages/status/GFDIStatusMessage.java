package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;


import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;

public abstract class GFDIStatusMessage extends GFDIMessage {
    private Status status;

    public static GFDIStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        int originalMessageType = reader.readShort();
        final GarminMessage originalGarminMessage = GFDIMessage.GarminMessage.fromId(originalMessageType);
        if (GarminMessage.PROTOBUF_REQUEST.equals(originalGarminMessage) || GarminMessage.PROTOBUF_RESPONSE.equals(originalGarminMessage)) {
            return ProtobufStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.FIT_DEFINITION.equals(originalGarminMessage)) {
            return FitDefinitionStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.FIT_DATA.equals(originalGarminMessage)) {
            return FitDataStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else {
            final Status status = Status.fromCode(reader.readByte());

            if (Status.ACK == status) {
                LOG.info("Received ACK for message {}", originalGarminMessage.name());
            } else {
                LOG.warn("Received {} for message {}", status, (null == originalGarminMessage) ? originalMessageType : originalGarminMessage.name());
            }

            return new GenericStatusMessage(garminMessage, status);
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
