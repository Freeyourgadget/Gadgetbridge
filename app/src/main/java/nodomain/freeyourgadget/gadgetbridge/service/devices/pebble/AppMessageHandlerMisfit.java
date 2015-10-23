package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.MisfitSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class AppMessageHandlerMisfit extends AppMessageHandler {

    public static final int KEY_SLEEPGOAL = 1;
    public static final int KEY_STEP_ROGRESS = 2;
    public static final int KEY_SLEEP_PROGRESS = 3;
    public static final int KEY_VERSION = 4;
    public static final int KEY_SYNC = 5;
    public static final int KEY_INCOMING_DATA_BEGIN = 6;
    public static final int KEY_INCOMING_DATA = 7;
    public static final int KEY_INCOMING_DATA_END = 8;
    public static final int KEY_SYNC_RESULT = 9;

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerMisfit.class);

    public AppMessageHandlerMisfit(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private final MisfitSampleProvider sampleProvider = new MisfitSampleProvider();

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_INCOMING_DATA_BEGIN:
                    LOG.info("incoming data start");
                    break;
                case KEY_INCOMING_DATA_END:
                    LOG.info("incoming data end");
                    break;
                case KEY_INCOMING_DATA:
                    byte[] data = (byte[]) pair.second;
                    ByteBuffer buf = ByteBuffer.wrap(data);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    int timestamp = buf.getInt();
                    int key = buf.getInt();
                    int samples = (data.length - 8) / 2;
                    if (samples <= 0) {
                        break;
                    }

                    if (!mPebbleProtocol.isFw3x) {
                        timestamp -= SimpleTimeZone.getDefault().getOffset(timestamp * 1000L) / 1000;
                    }
                    Date startDate = new Date((long) timestamp * 1000L);
                    Date endDate = new Date((long) (timestamp + samples * 60) * 1000L);
                    LOG.info("got data from " + startDate + " to " + endDate);

                    int steps = 0;
                    int totalSteps = 0;
                    GBActivitySample[] activitySamples = new GBActivitySample[samples];
                    for (int i = 0; i < samples; i++) {
                        short sample = buf.getShort();
                        if ((sample & 0x0001) == 0 || (sample & 0xff00) == 0) { // 16-??? steps encoded in bits 1-7
                            steps = (sample & 0x00fe);
                        } else if ((sample & 0x0001) == 0x0001) { // 0-14 steps encoded in bits 1-3, most of the time fc71 bits are set in that case
                            // 0040 also set always?
                            steps = (sample & 0x000e);
                        } else {
                            steps = 0;
                        }
                        totalSteps += steps;
                        LOG.info("got steps for sample " + i + " : " + steps + "(" + Integer.toHexString(sample & 0xffff) + ")");
                        byte activityKind = ActivityKind.TYPE_UNKNOWN;
                        if (steps > 0) {
                            activityKind = ActivityKind.TYPE_ACTIVITY;
                        }
                        activitySamples[i] = new GBActivitySample(sampleProvider, timestamp + i * 60, (short) steps, (short) steps, activityKind);
                    }
                    LOG.info("total steps for above period: " + totalSteps);

                    DBHandler db = null;
                    try {
                        db = GBApplication.acquireDB();
                        db.addGBActivitySamples(activitySamples);
                    } catch (GBException e) {
                        LOG.error("Error acquiring database", e);
                        return null;
                    } finally {
                        if (db != null) {
                            db.release();
                        }
                    }
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }

        // always ack
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        return new GBDeviceEvent[]{sendBytesAck};
    }
}
