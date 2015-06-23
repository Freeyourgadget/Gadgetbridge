package nodomain.freeyourgadget.gadgetbridge.deviceevents;


public class GBDeviceEventCallControl extends GBDeviceEvent {
    public Event event = Event.UNKNOWN;

    public GBDeviceEventCallControl() {
        eventClass = EventClass.CALL_CONTROL;
    }

    public enum Event {
        UNKNOWN,
        ACCEPT,
        END,
        INCOMING,
        OUTGOING,
        REJECT,
        START,
    }
}
