package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class MusicControlCapabilitiesMessage {
    public final int supportedCapabilities;

    public MusicControlCapabilitiesMessage(int supportedCapabilities) {
        this.supportedCapabilities = supportedCapabilities;
    }

    public static MusicControlCapabilitiesMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int supportedCapabilities = reader.readByte();

        return new MusicControlCapabilitiesMessage(supportedCapabilities);
    }
}
