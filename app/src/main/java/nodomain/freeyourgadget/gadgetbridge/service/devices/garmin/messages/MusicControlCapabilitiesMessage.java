package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class MusicControlCapabilitiesMessage extends GFDIMessage {

    private final int supportedCapabilities;
    private final GarminMusicControlCommand[] commands = GarminMusicControlCommand.values();
    private final int messageType;

    public MusicControlCapabilitiesMessage(int messageType, int supportedCapabilities) {
        this.messageType = messageType;
        this.supportedCapabilities = supportedCapabilities;
        this.statusMessage = this.getStatusMessage(messageType);
    }

    public static MusicControlCapabilitiesMessage parseIncoming(MessageReader reader, int messageType) {
        final int supportedCapabilities = reader.readByte();

        reader.warnIfLeftover();
        return new MusicControlCapabilitiesMessage(messageType, supportedCapabilities);
    }

    @Override
    protected boolean generateOutgoing() {
        if (commands.length > 255)
            throw new IllegalArgumentException("Too many supported commands");

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(messageType);
        writer.writeByte(Status.ACK.ordinal());
        writer.writeByte(commands.length);
        for (GarminMusicControlCommand command : commands) {
            writer.writeByte(command.ordinal());
        }
        return true;
    }

    enum GarminMusicControlCommand {
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
