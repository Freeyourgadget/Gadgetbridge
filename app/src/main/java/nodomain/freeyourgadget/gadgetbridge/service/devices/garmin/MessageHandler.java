package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;

public interface MessageHandler {
    public GFDIMessage handle(GFDIMessage message);
}
