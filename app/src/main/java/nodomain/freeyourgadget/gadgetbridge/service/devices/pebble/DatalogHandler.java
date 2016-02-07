package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import java.nio.ByteBuffer;

public class DatalogHandler {
    protected final PebbleProtocol mPebbleProtocol;
    protected final int mTag;

    DatalogHandler(int tag, PebbleProtocol pebbleProtocol) {
        mTag = tag;
        mPebbleProtocol = pebbleProtocol;
    }

    public int getTag() {
        return mTag;
    }

    public String getTagInfo() { return null; }

    public boolean handleMessage(ByteBuffer datalogMessage, int length) {
        return true;//ack the datalog transmission
    }

}