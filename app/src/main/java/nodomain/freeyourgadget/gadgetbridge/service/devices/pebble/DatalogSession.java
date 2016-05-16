package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import java.nio.ByteBuffer;
import java.util.UUID;

class DatalogSession {
    final byte id;
    final int tag;
    final UUID uuid;
    final byte itemType;
    final short itemSize;
    String taginfo = "(unknown)";

    DatalogSession(byte id, UUID uuid, int tag, byte itemType, short itemSize) {
        this.id = id;
        this.tag = tag;
        this.uuid = uuid;
        this.itemType = itemType;
        this.itemSize = itemSize;
    }

    boolean handleMessage(ByteBuffer buf, int length) {
        return true;
    }

    String getTaginfo() {
        return taginfo;
    }
}