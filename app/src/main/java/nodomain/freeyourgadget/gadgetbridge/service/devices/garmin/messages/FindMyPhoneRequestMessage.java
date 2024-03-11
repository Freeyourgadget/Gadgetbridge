package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;

public class FindMyPhoneRequestMessage extends GFDIMessage {
    private final int duration;
    private final int messageType;

    public FindMyPhoneRequestMessage(int messageType, int duration) {
        this.messageType = messageType;
        this.duration = duration;
    }

    public static FindMyPhoneRequestMessage parseIncoming(MessageReader reader, int messageType) {
        final int duration = reader.readByte();

        reader.warnIfLeftover();
        return new FindMyPhoneRequestMessage(messageType, duration);
    }

    @Override
    public GBDeviceEvent getGBDeviceEvent() {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = messageType == GarminMessage.FIND_MY_PHONE.getId() ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.STOP;
        return findPhoneEvent;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
