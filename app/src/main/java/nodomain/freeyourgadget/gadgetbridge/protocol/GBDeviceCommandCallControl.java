package nodomain.freeyourgadget.gadgetbridge.protocol;


public class GBDeviceCommandCallControl extends GBDeviceCommand {
    public Command command = Command.UNKNOWN;

    public GBDeviceCommandCallControl() {
        commandClass = CommandClass.CALL_CONTROL;
    }

    public enum Command {
        UNKNOWN,
        ACCEPT,
        END,
        INCOMING,
        OUTGOING,
        REJECT,
        START,
    }
}
