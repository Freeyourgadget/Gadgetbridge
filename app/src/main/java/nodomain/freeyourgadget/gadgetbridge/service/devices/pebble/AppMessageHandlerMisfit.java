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
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.MisfitSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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

    @Override
    public boolean isEnabled() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean("pebble_sync_misfit", true);
    }

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

                    int totalSteps = 0;
                    PebbleActivitySample[] activitySamples = new PebbleActivitySample[samples];
                    try (DBHandler db = GBApplication.acquireDB()) {
                        Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                        Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                        for (int i = 0; i < samples; i++) {
                            short sample = buf.getShort();
                            int steps = 0;
                            int intensity = 0;
                            int activityKind = ActivityKind.TYPE_UNKNOWN;

                            if (((sample & 0x83ff) == 0x0001) && ((sample & 0xff00) <= 0x4800)) {
                                // sleep seems to be from 0x2401 to 0x4801  (0b0IIIII0000000001) where I = intensity ?
                                intensity = (sample & 0x7c00) >>> 10;
                                // 9-18 decimal after shift
                                if (intensity <= 13) {
                                    activityKind = ActivityKind.TYPE_DEEP_SLEEP;
                                } else {
                                    // FIXME: this leads to too much false positives, ignore for now
                                    //activityKind = ActivityKind.TYPE_LIGHT_SLEEP;
                                    //intensity *= 2; // better visual distinction
                                }
                            } else {
                                if ((sample & 0x0001) == 0) { // 16-??? steps encoded in bits 1-7
                                    steps = (sample & 0x00fe);
                                } else { // 0-14 steps encoded in bits 1-3, most of the time fc71 bits are set in that case
                                    steps = (sample & 0x000e);
                                }
                                intensity = steps;
                                activityKind = ActivityKind.TYPE_ACTIVITY;
                            }

                            totalSteps += steps;
                            LOG.info("got steps for sample " + i + " : " + steps + "(" + Integer.toHexString(sample & 0xffff) + ")");

                            activitySamples[i] = new PebbleActivitySample(null, timestamp + i * 60, intensity, steps, activityKind, userId, deviceId);
                        }
                        LOG.info("total steps for above period: " + totalSteps);

                        MisfitSampleProvider sampleProvider = new MisfitSampleProvider(db.getDaoSession());
                        sampleProvider.addGBActivitySamples(activitySamples);
                    } catch (Exception e) {
                        LOG.error("Error acquiring database", e);
                        return null;
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
