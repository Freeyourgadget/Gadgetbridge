package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class DownloadRequestMessage extends GFDIMessage {

    private final int fileIndex;
    private final REQUEST_TYPE requestType;
    private final int crcSeed;
    private final int dataSize;

    private final int dataOffset;

    public DownloadRequestMessage(GarminMessage garminMessage, int fileIndex, int size, REQUEST_TYPE requestType, int crcSeed, int dataSize, int dataOffset) {
        this.requestType = requestType;
        this.crcSeed = crcSeed;
        this.dataSize = dataSize;
        this.dataOffset = dataOffset;
        this.garminMessage = garminMessage;
        this.fileIndex = fileIndex;
        this.statusMessage = this.getStatusMessage();
    }

    public DownloadRequestMessage(int fileIndex, int dataSize, REQUEST_TYPE requestType, int crcSeed, int dataOffset) {
        this(GarminMessage.DOWNLOAD_REQUEST, fileIndex, dataSize, requestType, crcSeed, dataSize, dataOffset);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeShort(this.fileIndex);
        writer.writeInt(this.dataOffset);
        writer.writeByte(this.requestType.ordinal());
        writer.writeShort(this.crcSeed);
        writer.writeInt(this.dataSize);

        return true;
    }

    public enum REQUEST_TYPE {
        CONTINUE,
        NEW,
    }

}
