package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


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
    public GBDeviceEvent getGBDeviceEvent() {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
        return findPhoneEvent;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
