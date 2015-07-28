package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

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
                    TimeZone tz = SimpleTimeZone.getDefault();
                    timestamp = (int) pair.second - (tz.getOffset(System.currentTimeMillis())) / 1000;
                    LOG.info("got timestamp " + timestamp);
                    break;
                case KEY_SAMPLES:
                    byte[] samples = (byte[]) pair.second;
                    ByteBuffer samplesBuffer = ByteBuffer.wrap(samples);
                    samplesBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    int samples_remaining = samples.length / 2;
                    LOG.info("got " + samples_remaining + " samples");
                    int offset_seconds = 0;
                    while (samples_remaining-- > 0) {
                        short sample = samplesBuffer.getShort();
                        byte type = (byte) ((sample & 0xe000) >>> 13);
                        byte intensity = (byte) ((sample & 0x1f80) >>> 7);
                        byte steps = (byte) (sample & 0x007f);
                        GBApplication.getActivityDatabaseHandler().addGBActivitySample(timestamp + offset_seconds, SampleProvider.PROVIDER_PEBBLE_GADGETBRIDGE, intensity, steps, type);
                        offset_seconds += 60;
                    }
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
