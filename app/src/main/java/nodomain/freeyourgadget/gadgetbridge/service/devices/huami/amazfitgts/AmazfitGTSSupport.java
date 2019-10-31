package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitgts;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;

public class AmazfitGTSSupport extends AmazfitBipSupport {

    @Override
    public byte getCryptFlags() {
        return (byte) 0x80;
    }

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.sendNotificationNew(notificationSpec, true);
    }

}
