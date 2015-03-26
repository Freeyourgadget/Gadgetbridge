package nodomain.freeyourgadget.gadgetbridge.protocol;

public class GBDeviceCommandAppManagementResult extends GBDeviceCommand {
    public Result result = Result.UNKNOWN;
    public CommandType type = CommandType.UNKNOWN;

    public GBDeviceCommandAppManagementResult() {
        commandClass = CommandClass.APP_MANAGEMENT_RES;
    }

    public enum CommandType {
        UNKNOWN,
        DELETE,
    }

    public enum Result {
        UNKNOWN,
        SUCCESS,
        FAILURE,
    }
}
