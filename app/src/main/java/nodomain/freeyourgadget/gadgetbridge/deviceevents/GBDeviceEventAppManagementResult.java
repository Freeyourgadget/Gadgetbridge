package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventAppManagementResult extends GBDeviceEvent {
    public Result result = Result.UNKNOWN;
    public EventType type = EventType.UNKNOWN;
    public int token = -1;

    public GBDeviceEventAppManagementResult() {
        eventClass = EventClass.APP_MANAGEMENT_RES;
    }

    public enum EventType {
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
