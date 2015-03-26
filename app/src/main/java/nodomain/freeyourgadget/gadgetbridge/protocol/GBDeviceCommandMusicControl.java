package nodomain.freeyourgadget.gadgetbridge.protocol;


public class GBDeviceCommandMusicControl extends GBDeviceCommand {
    public Command command = Command.UNKNOWN;

    public GBDeviceCommandMusicControl() {
        commandClass = CommandClass.MUSIC_CONTROL;
    }

    public enum Command {
        UNKNOWN,
        PLAY,
        PAUSE,
        PLAYPAUSE,
        NEXT,
        PREVIOUS,
    }
}
