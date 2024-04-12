package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class UploadRequestMessage extends GFDIMessage {

    private final int fileIndex;
    private final int size;
    private final boolean generateOutgoing;
    private final int dataOffset;
    private final int crcSeed;


    public UploadRequestMessage(GarminMessage garminMessage, int fileIndex, int size, int dataOffset, int crcSeed) {
        this.garminMessage = garminMessage;
        this.fileIndex = fileIndex;
        this.size = size;
        this.dataOffset = dataOffset;
        this.crcSeed = crcSeed;
        this.statusMessage = this.getStatusMessage();
        this.generateOutgoing = false;
    }

    public UploadRequestMessage(int fileIndex, int size) {
        this.garminMessage = GarminMessage.UPLOAD_REQUEST;
        this.fileIndex = fileIndex;
        this.size = size;
        this.dataOffset = 0;
        this.crcSeed = 0;
        this.statusMessage = this.getStatusMessage();
        this.generateOutgoing = true;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeShort(this.fileIndex);
        writer.writeInt(this.size);
        writer.writeInt(this.dataOffset);
        writer.writeShort(this.crcSeed);

        return generateOutgoing;
    }

}
