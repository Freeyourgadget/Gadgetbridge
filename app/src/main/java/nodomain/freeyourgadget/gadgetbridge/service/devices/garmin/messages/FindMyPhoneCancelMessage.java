package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;

public class FindMyPhoneCancelMessage extends GFDIMessage {
    public FindMyPhoneCancelMessage(GarminMessage garminMessage) {
        this.garminMessage = garminMessage;

        this.statusMessage = getStatusMessage();
    }

    public static FindMyPhoneCancelMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        return new FindMyPhoneCancelMessage(garminMessage);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
        return Collections.singletonList(findPhoneEvent);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
