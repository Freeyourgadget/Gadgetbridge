package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;

/**
 * An adapter class for the media commands sent by AsteroidOS
 */
public class AsteroidOSMediaCommand {
    public static final byte COMMAND_PREVIOUS = 0x0;
    public static final byte COMMAND_NEXT = 0x1;
    public static final byte COMMAND_PLAY = 0x2;
    public static final byte COMMAND_PAUSE = 0x3;
    public static final byte COMMAND_VOLUME = 0x4;

    public byte command;
    public AsteroidOSMediaCommand(byte value) {
        command = value;
    }

    /**
     * Convert the MediaCommand to a music control event
     * @return the matching music control event
     */
    public GBDeviceEventMusicControl toMusicControlEvent() {
        GBDeviceEventMusicControl event = new GBDeviceEventMusicControl();
        switch (command) {
            case COMMAND_PREVIOUS:
                event.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            case COMMAND_NEXT:
                event.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case COMMAND_PLAY:
                event.event = GBDeviceEventMusicControl.Event.PLAY;
                break;
            case COMMAND_PAUSE:
                event.event = GBDeviceEventMusicControl.Event.PAUSE;
                break;
            case COMMAND_VOLUME:
            default:
                event.event = GBDeviceEventMusicControl.Event.UNKNOWN;
        }
        return event;
    }
}
