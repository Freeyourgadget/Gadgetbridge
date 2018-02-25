package nodomain.freeyourgadget.gadgetbridge.devices.xwatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XWatchService {
    public static final UUID UUID_NOTIFY = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_WRITE = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");

    public static final byte COMMAND_CONNECTED = 0x01;
    public static final byte COMMAND_ACTION_BUTTON = 0x4c;
    public static final byte COMMAND_ACTIVITY_DATA = 0x43;
    public static final byte COMMAND_ACTIVITY_TOTALS = 0x46;

    private static final Map<UUID, String> XWATCH_DEBUG;

    static {
        XWATCH_DEBUG = new HashMap<>();

        XWATCH_DEBUG.put(UUID_NOTIFY, "Read data");
        XWATCH_DEBUG.put(UUID_WRITE, "Write data");
        XWATCH_DEBUG.put(UUID_SERVICE, "Get service");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = XWATCH_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }
}
