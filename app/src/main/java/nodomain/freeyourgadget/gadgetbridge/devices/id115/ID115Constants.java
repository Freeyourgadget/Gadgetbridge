package nodomain.freeyourgadget.gadgetbridge.devices.id115;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class ID115Constants {
    public static final UUID UUID_SERVICE_ID115 = UUID.fromString(String.format(BASE_UUID, "0AF0"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF6"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_NORMAL = UUID.fromString(String.format(BASE_UUID, "0AF7"));
    public static final UUID UUID_CHARACTERISTIC_WRITE_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF1"));
    public static final UUID UUID_CHARACTERISTIC_NOTIFY_HEALTH = UUID.fromString(String.format(BASE_UUID, "0AF2"));

    public static final byte CMD_ID_WARE_UPDATE = 0x01;
    public static final byte CMD_ID_GET_INFO = 0x02;
    public static final byte CMD_ID_SETTINGS = 0x03;
    public static final byte CMD_ID_BIND_UNBIND = 0x04;
    public static final byte CMD_ID_NOTIFY = 0x05;
    public static final byte CMD_ID_APP_CONTROL = 0x06;
    public static final byte CMD_ID_BLE_CONTROL = 0x07;
    public static final byte CMD_ID_HEALTH_DATA = 0x08;
    public static final byte CMD_ID_DUMP_STACK = 0x20;
    public static final byte CMD_ID_LOG = 0x21;
    public static final byte CMD_ID_FACTORY = (byte)0xaa;
    public static final byte CMD_ID_DEVICE_RESTART = (byte)0xf0;

    // CMD_ID_SETTINGS
    public static final byte CMD_KEY_SET_TIME = 0x01;
    public static final byte CMD_KEY_SET_DISPLAY_MODE = 0x2B;

    // CMD_ID_DEVICE_RESTART
    public static final byte CMD_KEY_REBOOT = 0x01;
}
