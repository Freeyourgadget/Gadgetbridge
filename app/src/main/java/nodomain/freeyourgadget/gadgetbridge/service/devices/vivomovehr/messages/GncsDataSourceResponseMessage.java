package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class GncsDataSourceResponseMessage {
    public static final int RESPONSE_TRANSFER_SUCCESSFUL = 0;
    public static final int RESPONSE_RESEND_LAST_DATA_PACKET = 1;
    public static final int RESPONSE_ABORT_REQUEST = 2;
    public static final int RESPONSE_ERROR_CRC_MISMATCH = 3;
    public static final int RESPONSE_ERROR_DATA_OFFSET_MISMATCH = 4;

    public final int status;
    public final int response;

    public GncsDataSourceResponseMessage(int status, int response) {
        this.status = status;
        this.response = response;
    }

    public static GncsDataSourceResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestMessageID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();

        return new GncsDataSourceResponseMessage(status, response);
    }
}
