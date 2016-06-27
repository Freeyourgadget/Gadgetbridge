package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

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
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleGadgetBridgeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class AppMessageHandlerGBPebble extends AppMessageHandler {

    public static final int KEY_TIMESTAMP = 1;
    public static final int KEY_SAMPLES = 2;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerGBPebble.class);

    AppMessageHandlerGBPebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
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
                    try (DBHandler db = GBApplication.acquireDB()) {
                        User user = DBHelper.getUser(db.getDaoSession());
                        Device device = DBHelper.getDevice(getDevice(), db.getDaoSession());
                        PebbleGadgetBridgeSampleProvider sampleProvider = new PebbleGadgetBridgeSampleProvider(getDevice(), db.getDaoSession());
                        PebbleActivitySample[] activitySamples = new PebbleActivitySample[samples_remaining];
                        int i = 0;
                        while (samples_remaining-- > 0) {
                            short sample = samplesBuffer.getShort();
                            int type = ((sample & 0xe000) >>> 13);
                            int intensity = ((sample & 0x1f80) >>> 7);
                            int steps = (sample & 0x007f);
                            activitySamples[i++] = createSample(timestamp + offset_seconds, intensity, steps, type, user, device);
                            offset_seconds += 60;
                        }
                        sampleProvider.addGBActivitySamples(activitySamples);
                    } catch (Exception e) {
                        LOG.error("Error acquiring database", e);
                    }
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);
        return new GBDeviceEvent[]{sendBytes};
    }
}
