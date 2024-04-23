package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.NotificationSubscriptionDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationSubscriptionStatusMessage;

public class NotificationSubscriptionMessage extends GFDIMessage {

    private final boolean enable;
    private final int unk;

    public NotificationSubscriptionMessage(GarminMessage garminMessage, boolean enable, int unk) {
        this.garminMessage = garminMessage;
        this.enable = enable;
        this.unk = unk;

        // We do not set the status message here so we can reply with the proper notifications status
        // from the device event
    }

    public static NotificationSubscriptionMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final boolean enable = reader.readByte() == 1;
        final int unk = reader.readByte();

        return new NotificationSubscriptionMessage(garminMessage, enable, unk);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        NotificationSubscriptionDeviceEvent notificationSubscriptionDeviceEvent = new NotificationSubscriptionDeviceEvent();
        notificationSubscriptionDeviceEvent.enable = this.enable;
        return Collections.singletonList(notificationSubscriptionDeviceEvent);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
