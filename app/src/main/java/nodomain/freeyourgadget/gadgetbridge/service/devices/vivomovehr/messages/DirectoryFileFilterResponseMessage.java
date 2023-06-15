package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class DirectoryFileFilterResponseMessage {
    public final int status;
    public final int response;

    public static final int RESPONSE_DIRECTORY_FILTER_APPLIED = 0;
    public static final int RESPONSE_FAILED_TO_APPLY_DIRECTORY_FILTER = 1;

    public DirectoryFileFilterResponseMessage(int status, int response) {
        this.status = status;
        this.response = response;
    }

    public static DirectoryFileFilterResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();

        return new DirectoryFileFilterResponseMessage(status, response);
    }
}
