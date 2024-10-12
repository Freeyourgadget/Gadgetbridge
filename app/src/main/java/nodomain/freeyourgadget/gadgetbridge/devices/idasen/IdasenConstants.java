package nodomain.freeyourgadget.gadgetbridge.devices.idasen;

import java.util.UUID;

public class IdasenConstants {
    public static final UUID CHARACTERISTIC_SVC_HEIGHT = UUID.fromString("99fa0020-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_SVC_COMMAND = UUID.fromString("99fa0001-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_COMMAND = UUID.fromString("99fa0002-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_SVC_REF_HEIGHT = UUID.fromString("99fa0030-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_SVC_DPG = UUID.fromString("99fa0010-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_REF_HEIGHT = UUID.fromString("99fa0031-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_HEIGHT = UUID.fromString("99fa0021-338a-1024-8a49-009c0215f78a");
    public static final UUID CHARACTERISTIC_DPG = UUID.fromString("99fa0011-338a-1024-8a49-009c0215f78a");
    public static final String ACTION_REALTIME_DESK_VALUES = ".action.realtime_desk_values";
    public static final String EXTRA_DESK_HEIGHT = "EXTRA_DESK_HEIGHT";
    public static final String EXTRA_DESK_SPEED = "EXTRA_DESK_SPEED";

    public static final double MIN_HEIGHT = 0.62;
    public static final double MAX_HEIGHT = 1.27;

    public static final byte[] CMD_UP = new byte[]{0x47, 0x00};
    public static final byte[] CMD_DOWN = new byte[]{0x46, 0x00};
    public static final byte[] CMD_STOP = new byte[]{(byte)0xFF, 0x00};
    public static final byte[] CMD_WAKEUP = new byte[]{(byte)0xFE, 0x00};
    public static final byte[] CMD_REF_INPUT_STOP = new byte[]{0x01, (byte)0x80};
    public static final byte[] CMD_DPG_WAKEUP_PREP = new byte[]{0x7F, (byte)0x86, 0x00};
    public static final byte[] CMD_DPG_WAKEUP = new byte[]{0x7F, (byte)0x86, (byte)0x80, 0x01,
        0x02, 0x03, 0x04, 0x05, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,
        0x0E, 0x0F, 0x10, 0x11};
}
