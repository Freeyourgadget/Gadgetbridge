package nodomain.freeyourgadget.gadgetbridge.devices.no1f1;

import java.util.UUID;

public final class No1F1Constants {

    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_NO1 = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb");

    public static final byte CMD_FIRMWARE_VERSION = (byte) 0xa1;
    public static final byte CMD_BATTERY = (byte) 0xa2;
    public static final byte CMD_DATETIME = (byte) 0xa3;
    public static final byte CMD_USER_DATA = (byte) 0xa9;
    public static final byte CMD_ALARM = (byte) 0xab;

}
