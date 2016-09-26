package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;

public class AlertNotificationProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    public AlertNotificationProfile(T support) {
        super(support);
    }
}
