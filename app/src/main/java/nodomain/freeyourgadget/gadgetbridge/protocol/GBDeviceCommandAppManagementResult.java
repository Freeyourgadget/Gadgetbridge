package nodomain.freeyourgadget.gadgetbridge.protocol;

public class GBDeviceCommandAppManagementResult extends GBDeviceCommand {
    public Result result = Result.UNKNOWN;
    public CommandType type = CommandType.UNKNOWN;
    public int token = -1;

    public GBDeviceCommandAppManagementResult() {
        commandClass = CommandClass.APP_MANAGEMENT_RES;
    }

    public enum CommandType {
        UNKNOWN,
        INSTALL,
        DELETE,
    }

    public enum Result {
        UNKNOWN,
        SUCCESS,
        ACKNOLEDGE,
        FAILURE,
    }
}
