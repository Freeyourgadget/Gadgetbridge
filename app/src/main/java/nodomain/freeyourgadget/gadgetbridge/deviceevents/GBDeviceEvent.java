package nodomain.freeyourgadget.gadgetbridge.deviceevents;


public abstract class GBDeviceEvent {
    public EventClass eventClass = EventClass.UNKNOWN;

    public enum EventClass {
        UNKNOWN,
        MUSIC_CONTROL,
        CALL_CONTROL,
        APP_INFO,
        VERSION_INFO,
        APP_MANAGEMENT_RES,
        SEND_BYTES,
        SLEEP_MONITOR_RES,
        SCREENSHOT,
    }
}

