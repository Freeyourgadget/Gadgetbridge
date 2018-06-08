package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

/**
 * Created by lightforce on 08.06.18.
 */

import java.util.UUID;

public class ZeTimeConstants {
    public static final UUID UUID_WRITE_CHARACTERISTIC = UUID.fromString("00008001-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_ACK_CHARACTERISTIC = UUID.fromString("00008002-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_REPLY_CHARACTERISTIC = UUID.fromString("00008003-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NOTIFY_CHARACTERISTIC = UUID.fromString("00008004-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_8005 = UUID.fromString("00008005-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_BASE = UUID.fromString("00006006-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_EXTEND = UUID.fromString("00007006-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

    public static final byte CMD_PREAMBLE = (byte) 0x6f;
    public static final byte CMD_GET_AVAIABLE_DATE = (byte) 0x52;
    public static final byte CMD_REQUEST = (byte) 0x70;
    public static final byte CMD_END = (byte) 0x8f;
    public static final byte CMD_ACK_WRITE = (byte) 0x03;
}
