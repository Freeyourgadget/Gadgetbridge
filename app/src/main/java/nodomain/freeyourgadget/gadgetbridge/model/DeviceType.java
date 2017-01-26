package nodomain.freeyourgadget.gadgetbridge.model;

/**
 * For every supported device, a device type constant must exist.
 *
 * Note: they key of every constant is stored in the DB, so it is fixed forever,
 * and may not be changed.
 */
public enum DeviceType {
    UNKNOWN(-1),
    PEBBLE(1),
    MIBAND(10),
    MIBAND2(11),
    VIBRATISSIMO(20),
    LIVEVIEW(30),
    HPLUS(40),
    MAKIBESF68(50),
    TEST(1000);

    private final int key;

    DeviceType(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    public static DeviceType fromKey(int key) {
        for (DeviceType type : values()) {
            if (type.key == key) {
                return type;
            }
        }
        return DeviceType.UNKNOWN;
    }
}
