package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_status.xml
 * uint8 value (bitmask) of the given values
 */
public class AlertStatus {
    public static final int RINGER_ACTIVE_BIT = 1;
    public static final int VIBRATE_ACTIVE = 1 << 1;
    public static final int DISPLAY_ALERT_ACTIVE = 1 << 2;

    public static boolean isRingerActive(int status) {
        return (status & RINGER_ACTIVE_BIT) == RINGER_ACTIVE_BIT;
    }
    public static boolean isVibrateActive(int status) {
        return (status & VIBRATE_ACTIVE) == VIBRATE_ACTIVE;
    }
    public static boolean isDisplayAlertActive(int status) {
        return (status & DISPLAY_ALERT_ACTIVE) == DISPLAY_ALERT_ACTIVE;
    }
}
