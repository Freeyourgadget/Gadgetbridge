package nodomain.freeyourgadget.gadgetbridge.devices.lenovo;

public enum DataType {
    STEPS(new byte[]{0x00, 0x00}),
    SLEEP(new byte[]{0x00, 0x01}),
    HEART_RATE(new byte[]{0x00, 0x02}),
    BLOOD_PRESSURE(new byte[]{0x00, 0x06}),
    INFRARED_TEMPERATURE(new byte[]{0x00, 0x08}),
    ENVIRONMENT_TEMPERATURE(new byte[]{0x00, 0x09}),
    AIR_PRESSURE(new byte[]{0x00, 0x0A});

    private final byte[] value;

    DataType(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public static DataType getType(int value) {
        for(DataType type : values()) {
            int intVal = (type.getValue()[1] & 0xff) | ((type.getValue()[0] & 0xff) << 8);
            if(intVal == value) {
                return type;
            }
        }
        throw new RuntimeException("No value defined for " + value);
    }
}
