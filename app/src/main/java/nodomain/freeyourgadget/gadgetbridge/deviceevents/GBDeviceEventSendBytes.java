package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventSendBytes extends GBDeviceEvent {
    public byte[] encodedBytes;

    public GBDeviceEventSendBytes() {
        eventClass = EventClass.SEND_BYTES;
    }
}
