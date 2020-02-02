package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class PlayCallNotificationRequest extends PlayNotificationRequest {
    public PlayCallNotificationRequest(String number, boolean callStart, FossilWatchAdapter adapter) {
        super(callStart ? 1 : 7, callStart ? 8 : 2, "generic", number, "Incoming Call", adapter);
    }
}
