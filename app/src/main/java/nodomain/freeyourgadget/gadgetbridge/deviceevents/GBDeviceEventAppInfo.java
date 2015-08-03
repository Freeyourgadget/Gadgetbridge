package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

public class GBDeviceEventAppInfo extends GBDeviceEvent {
    public GBDeviceApp apps[];
    public byte freeSlot = -1;

    public GBDeviceEventAppInfo() {
        eventClass = EventClass.APP_INFO;
    }
}
