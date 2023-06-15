package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class SystemEventResponseMessage {
    public final int status;
    public final int response;

    public SystemEventResponseMessage(int status, int response) {
        this.status = status;
        this.response = response;
    }

    public static SystemEventResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();

        return new SystemEventResponseMessage(status, response);
    }
}
