package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class CurrentTimeRequestMessage {
    public final int referenceID;

    public CurrentTimeRequestMessage(int referenceID) {
        this.referenceID = referenceID;
    }

    public static CurrentTimeRequestMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int referenceID = reader.readInt();

        return new CurrentTimeRequestMessage(referenceID);
    }
}
