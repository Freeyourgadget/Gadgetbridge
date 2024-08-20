package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class MusicControlCapabilitiesMessage extends GFDIMessage {

    private final int supportedCapabilities;
    private final GarminMusicControlCommand[] commands = GarminMusicControlCommand.values();

    public MusicControlCapabilitiesMessage(GarminMessage garminMessage, int supportedCapabilities) {
        this.garminMessage = garminMessage;
        this.supportedCapabilities = supportedCapabilities;
        this.statusMessage = null; //our outgoing message is an ACK message
    }

    public static MusicControlCapabilitiesMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int supportedCapabilities = reader.readByte();

        return new MusicControlCapabilitiesMessage(garminMessage, supportedCapabilities);
    }

    @Override
    protected boolean generateOutgoing() {
        if (commands.length > 255)
            throw new IllegalArgumentException("Too many supported commands");

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(Status.ACK.ordinal());
        writer.writeByte(commands.length);
        for (GarminMusicControlCommand command : commands) {
            writer.writeByte(command.ordinal());
        }
        return true;
    }

    public enum GarminMusicControlCommand {
        TOGGLE_PLAY_PAUSE,
        SKIP_TO_NEXT_ITEM,
        SKIP_TO_PREVIOUS_ITEM,
        VOLUME_UP,
        VOLUME_DOWN,
        PLAY,
        PAUSE,
        SKIP_FORWARD,
        SKIP_BACKWARDS
    }
}
