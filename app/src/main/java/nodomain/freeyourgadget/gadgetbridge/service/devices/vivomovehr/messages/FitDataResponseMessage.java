package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class FitDataResponseMessage {
    public final int requestID;
    public final int status;
    public final int fitResponse;

    public FitDataResponseMessage(int requestID, int status, int fitResponse) {
        this.requestID = requestID;
        this.status = status;
        this.fitResponse = fitResponse;
    }

    public static FitDataResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int fitResponse = reader.readByte();

        return new FitDataResponseMessage(requestID, status, fitResponse);
    }

    public static final int RESPONSE_APPLIED = 0;
    public static final int RESPONSE_NO_DEFINITION = 1;
    public static final int RESPONSE_MISMATCH = 2;
    public static final int RESPONSE_NOT_READY = 3;
}
