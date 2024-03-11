package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;

public class MusicControlMessage extends GFDIMessage {

    private static final MusicControlCapabilitiesMessage.GarminMusicControlCommand[] commands = MusicControlCapabilitiesMessage.GarminMusicControlCommand.values();
    final int messageType;
    private final GBDeviceEventMusicControl event;

    public MusicControlMessage(int messageType, MusicControlCapabilitiesMessage.GarminMusicControlCommand command) {
        this.event = new GBDeviceEventMusicControl();
        this.messageType = messageType;
        switch (command) {
            case TOGGLE_PLAY_PAUSE:
                event.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            case SKIP_TO_NEXT_ITEM:
                event.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case SKIP_TO_PREVIOUS_ITEM:
                event.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
        }

        this.statusMessage = this.getStatusMessage(messageType);
    }

    public static MusicControlMessage parseIncoming(MessageReader reader, int messageType) {
        MusicControlCapabilitiesMessage.GarminMusicControlCommand command = commands[reader.readByte()];

        reader.warnIfLeftover();
        return new MusicControlMessage(messageType, command);
    }

    public GBDeviceEventMusicControl getGBDeviceEvent() {
        return event;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
