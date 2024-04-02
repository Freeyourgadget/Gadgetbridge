package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;

public class FindMyPhoneRequestMessage extends GFDIMessage {
    private final int duration;

    public FindMyPhoneRequestMessage(GarminMessage garminMessage, int duration) {
        this.garminMessage = garminMessage;
        this.duration = duration;

        this.statusMessage = getStatusMessage();
    }

    public static FindMyPhoneRequestMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int duration = reader.readByte();

        return new FindMyPhoneRequestMessage(garminMessage, duration);
    }

    @Override
    public GBDeviceEvent getGBDeviceEvent() {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = garminMessage == GarminMessage.FIND_MY_PHONE ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.STOP;
        return findPhoneEvent;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
