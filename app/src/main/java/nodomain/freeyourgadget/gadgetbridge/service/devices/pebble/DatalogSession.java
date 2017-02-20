package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble.GBDeviceEventDataLogging;

class DatalogSession {
    private static final Logger LOG = LoggerFactory.getLogger(DatalogSession.class);

    final byte id;
    final int tag;
    final UUID uuid;
    final byte itemType;
    final short itemSize;
    final int timestamp;
    String taginfo = "(unknown)";

    DatalogSession(byte id, UUID uuid, int timestamp, int tag, byte itemType, short itemSize) {
        this.id = id;
        this.tag = tag;
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.itemType = itemType;
        this.itemSize = itemSize;
    }

    boolean handleMessage(ByteBuffer buf, int length) {
        return true;
    }

    String getTaginfo() {
        return taginfo;
    }

    GBDeviceEventDataLogging handleMessageForPebbleKit(ByteBuffer buf, int length) {
        if (0 != (length % itemSize)) {
            LOG.warn("invalid length");
            return null;
        }
        int packetCount = length / itemSize;

        if (packetCount <= 0) {
            LOG.warn("invalid number of datalog elements");
            return null;
        }

        GBDeviceEventDataLogging dataLogging = new GBDeviceEventDataLogging();
        dataLogging.command = GBDeviceEventDataLogging.COMMAND_RECEIVE_DATA;
        dataLogging.appUUID = uuid;
        dataLogging.timestamp = timestamp & 0xffffffffL;
        dataLogging.tag = tag;
        dataLogging.pebbleDataType = itemType;
        dataLogging.data = new Object[packetCount];

        for (int i = 0; i < packetCount; i++) {
            switch (itemType) {
                case PebbleProtocol.TYPE_BYTEARRAY:
                    byte[] itemData = new byte[itemSize];
                    buf.get(itemData);
                    dataLogging.data[i] = itemData;
                    break;

                case PebbleProtocol.TYPE_UINT:
                    dataLogging.data[i] = buf.getInt() & 0xffffffffL;
                    break;

                case PebbleProtocol.TYPE_INT:
                    dataLogging.data[i] = buf.getInt();
                    break;
            }
        }
        return dataLogging;
    }
}