package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventNotificationControl extends GBDeviceEvent {
    public int handle;

    public Event event = Event.UNKNOWN;

    public enum Event {
        UNKNOWN,
        DISMISS,
        DISMISS_ALL,
        OPEN,
        REPLY
    }
}
