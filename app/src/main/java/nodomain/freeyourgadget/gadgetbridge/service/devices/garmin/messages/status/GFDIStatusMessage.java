package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;


import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;

public abstract class GFDIStatusMessage extends GFDIMessage {
    Status status;

    public static GFDIStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final GarminMessage garminMessage = GFDIMessage.GarminMessage.fromId(reader.readShort());
        if (GarminMessage.PROTOBUF_REQUEST.equals(garminMessage) || GarminMessage.PROTOBUF_RESPONSE.equals(garminMessage)) {
            return ProtobufStatusMessage.parseIncoming(reader, messageType);
        } else {
            final Status status = Status.fromCode(reader.readByte());

            switch (status) {
                case ACK:
                    LOG.info("Received ACK for message {}", garminMessage.name());
                    break;
                default:
                    LOG.warn("Received {} for message {}", status, garminMessage.name());
            }

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
