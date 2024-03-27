package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;

public class MusicControlMessage extends GFDIMessage {

    private static final MusicControlCapabilitiesMessage.GarminMusicControlCommand[] commands = MusicControlCapabilitiesMessage.GarminMusicControlCommand.values();
    private final GBDeviceEventMusicControl event;

    public MusicControlMessage(GarminMessage garminMessage, MusicControlCapabilitiesMessage.GarminMusicControlCommand command) {
        this.event = new GBDeviceEventMusicControl();
        this.garminMessage = garminMessage;
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

        this.statusMessage = this.getStatusMessage();
    }

    public static MusicControlMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        MusicControlCapabilitiesMessage.GarminMusicControlCommand command = commands[reader.readByte()];

        reader.warnIfLeftover();
        return new MusicControlMessage(garminMessage, command);
    }

    public GBDeviceEventMusicControl getGBDeviceEvent() {
        return event;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
