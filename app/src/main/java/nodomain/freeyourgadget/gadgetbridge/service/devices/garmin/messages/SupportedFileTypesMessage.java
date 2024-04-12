package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class SupportedFileTypesMessage extends GFDIMessage {

    public SupportedFileTypesMessage() {
        this.garminMessage = GarminMessage.SUPPORTED_FILE_TYPES_REQUEST;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        return true;
    }
}
