package nodomain.freeyourgadget.gadgetbridge.protocol;


public abstract class GBDeviceCommand {
    public CommandClass commandClass = CommandClass.UNKNOWN;

    public enum CommandClass {
        UNKNOWN,
        MUSIC_CONTROL,
        CALL_CONTROL,
        APP_INFO,
        VERSION_INFO
    }
}

