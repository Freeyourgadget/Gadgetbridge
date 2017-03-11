package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_notification_control_point.xml
 */
public enum Command {
    EnableNewIncomingAlertNotification(0),
    EnableUnreadCategoryStatusNotification(1),
    DisableNewIncomingAlertNotification(2),
    DisbleUnreadCategoryStatusNotification(3),
    NotifyNewIncomingAlertImmediately(4),
    NotifyUnreadCategoryStatusImmediately(5),;
    // 6-255 reserved for future use

    private final int id;

    Command(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
