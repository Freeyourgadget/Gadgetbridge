package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;

public interface MessageHandler {
    GFDIMessage handle(GFDIMessage message);
}
