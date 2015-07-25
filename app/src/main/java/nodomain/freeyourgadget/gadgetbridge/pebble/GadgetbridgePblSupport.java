package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

public class GadgetbridgePblSupport {

    public static final int KEY_TIMESTAMP = 1;
    public static final int KEY_SAMPLES = 2;

    public static final UUID uuid = UUID.fromString("61476764-7465-7262-6469-656775527a6c");
    private final PebbleProtocol mPebbleProtocol;

    private static final Logger LOG = LoggerFactory.getLogger(GadgetbridgePblSupport.class);

    public GadgetbridgePblSupport(PebbleProtocol pebbleProtocol) {
        mPebbleProtocol = pebbleProtocol;
    }

    public GBDeviceEvent handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        int timestamp = 0;
        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_TIMESTAMP:
                    timestamp = (int) pair.second;
                    LOG.info("got timestamp " + timestamp);
                    break;
                case KEY_SAMPLES:
                    byte[] samples = (byte[]) pair.second;
                    LOG.info("got " + samples.length / 2 + " samples");
                    ByteBuffer samplesBuffer = ByteBuffer.wrap(samples);
                    // TODO: read samples and put into database
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(uuid, mPebbleProtocol.last_id);
        return sendBytes;
    }
}
