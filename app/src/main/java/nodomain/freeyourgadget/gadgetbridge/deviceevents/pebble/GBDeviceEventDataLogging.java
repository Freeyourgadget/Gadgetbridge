package nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;

public class GBDeviceEventDataLogging extends GBDeviceEvent {
    public static final int COMMAND_RECEIVE_DATA = 1;
    public static final int COMMAND_FINISH_SESSION = 2;

    public int command;
    public UUID appUUID;
    public long timestamp;
    public long tag;
    public byte pebbleDataType;
    public Object data;
}
