package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class FindMyPhoneRequestMessage {
    public final int duration;

    public FindMyPhoneRequestMessage(int duration) {
        this.duration = duration;
    }

    public static FindMyPhoneRequestMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int duration = reader.readByte();

        return new FindMyPhoneRequestMessage(duration);
    }
}
