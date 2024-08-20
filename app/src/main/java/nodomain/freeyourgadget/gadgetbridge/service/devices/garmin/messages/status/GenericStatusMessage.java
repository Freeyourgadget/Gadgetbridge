package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class GenericStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private int messageType; // for unsupported message types
    private boolean sendOutgoing;

    public GenericStatusMessage(GarminMessage originalMessage, Status status) {
        this.garminMessage = originalMessage;
        this.status = status;
        this.sendOutgoing = true;
    }

    public GenericStatusMessage(GarminMessage originalMessage, Status status, boolean sendOutgoing) {
        this.garminMessage = originalMessage;
        this.status = status;
        this.sendOutgoing = sendOutgoing;
    }

    public GenericStatusMessage(int messageType, Status status) {
        this.messageType = messageType;
        this.status = status;
        this.sendOutgoing = false;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType != 0 ? messageType : garminMessage.getId());
        writer.writeByte(status.ordinal());
        return sendOutgoing;
    }

}
