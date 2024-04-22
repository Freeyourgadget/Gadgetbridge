package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
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
            case VOLUME_UP:
                event.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                break;
            case VOLUME_DOWN:
                event.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                break;
        }

        this.statusMessage = this.getStatusMessage();
    }

    public static MusicControlMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        MusicControlCapabilitiesMessage.GarminMusicControlCommand command = commands[reader.readByte()];

        return new MusicControlMessage(garminMessage, command);
    }

    public List<GBDeviceEvent> getGBDeviceEvent() {
        return Collections.singletonList(event);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
