package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate;

/**
 * The Body Sensor Location characteristic of the device is used to describe the intended location of the heart rate measurement for the device.
 *
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_sensor_location.xml
 */
public enum BodySensorLocation {
    Other(0),
    Chest(1),
    Wrist(2),
    Finger(3),
    Hand(4),
    EarLobe(5),
    Foot(6);
    // others are reserved

    private final int val;

    BodySensorLocation(int val) {
        this.val = val;
    }
}
