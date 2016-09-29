package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;


/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_level.xml
 */
public enum AlertLevel {
    NoAlert(0),
    MildAlert(1),
    HighAlert(2);
    // 3-255 reserved

    private final int id;

    AlertLevel(int id) {
        this.id = id;
    }

    /**
     * The alert level ID
     * To be used as uint8 value
     * @return
     */
    public int getId() {
        return id;
    }
}
