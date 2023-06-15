package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class CreateFileResponseMessage {
    public static final byte RESPONSE_FILE_CREATED_SUCCESSFULLY = 0;
    public static final byte RESPONSE_FILE_ALREADY_EXISTS = 1;
    public static final byte RESPONSE_NOT_ENOUGH_SPACE = 2;
    public static final byte RESPONSE_NOT_SUPPORTED = 3;
    public static final byte RESPONSE_NO_SLOTS_AVAILABLE_FOR_FILE_TYPE = 4;
    public static final byte RESPONSE_NOT_ENOUGH_SPACE_FOR_FILE_TYPE = 5;

    public final int status;
    public final int response;
    public final int fileIndex;
    public final int dataType;
    public final int subType;
    public final int fileNumber;

    public CreateFileResponseMessage(int status, int response, int fileIndex, int dataType, int subType, int fileNumber) {
        this.status = status;
        this.response = response;
        this.fileIndex = fileIndex;
        this.dataType = dataType;
        this.subType = subType;
        this.fileNumber = fileNumber;
    }

    public static CreateFileResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 6);
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int fileIndex = reader.readShort();
        final int dataType = reader.readByte();
        final int subType = reader.readByte();
        final int fileNumber = reader.readShort();

        return new CreateFileResponseMessage(status, response, fileIndex, dataType, subType, fileNumber);
    }
}
