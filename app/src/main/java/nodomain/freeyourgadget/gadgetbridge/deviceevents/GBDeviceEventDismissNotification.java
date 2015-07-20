package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventDismissNotification extends GBDeviceEvent {
    public int notificationID;

    public GBDeviceEventDismissNotification() {
        eventClass = EventClass.DISMISS_NOTIFICATION;
    }
}
