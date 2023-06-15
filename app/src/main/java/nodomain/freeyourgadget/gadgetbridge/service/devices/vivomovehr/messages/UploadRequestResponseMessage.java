package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class UploadRequestResponseMessage {
    public static final byte RESPONSE_UPLOAD_REQUEST_OKAY = 0;
    public static final byte RESPONSE_DATA_FILE_INDEX_DOES_NOT_EXIST = 1;
    public static final byte RESPONSE_DATA_FILE_INDEX_EXISTS_BUT_IS_NOT_WRITEABLE = 2;
    public static final byte RESPONSE_NOT_ENOUGH_SPACE_TO_COMPLETE_WRITE = 3;
    public static final byte RESPONSE_REQUEST_INVALID = 4;
    public static final byte RESPONSE_NOT_READY_TO_UPLOAD = 5;
    public static final byte RESPONSE_CRC_INCORRECT = 6;

    public final int status;
    public final int response;
    public final int dataOffset;
    public final int maxFileSize;
    public final int crcSeed;

    public UploadRequestResponseMessage(int status, int response, int dataOffset, int maxFileSize, int crcSeed) {
        this.status = status;
        this.response = response;
        this.dataOffset = dataOffset;
        this.maxFileSize = maxFileSize;
        this.crcSeed = crcSeed;
    }

    public static UploadRequestResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 6);
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int dataOffset = reader.readInt();
        final int maxFileSize = reader.readInt();
        final int crcSeed = reader.readInt();

        return new UploadRequestResponseMessage(status, response, dataOffset, maxFileSize, crcSeed);
    }
}
