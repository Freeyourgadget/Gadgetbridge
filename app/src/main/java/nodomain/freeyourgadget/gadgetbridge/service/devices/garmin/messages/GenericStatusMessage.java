package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class GenericStatusMessage extends GFDIStatusMessage {

    private final int messageType;
    private final Status status;

    public GenericStatusMessage(int originalRequestID, Status status) {
        this.messageType = originalRequestID;
        this.status = status;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType);
        writer.writeByte(status.ordinal());
        return true;
    }

}
