package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.MorpheuzSampleProvider;

public class MorpheuzSupport {

    public static final int KEY_POINT = 1;
    public static final int KEY_CTRL = 2;
    public static final int KEY_FROM = 3;
    public static final int KEY_TO = 4;
    public static final int KEY_BASE = 5;
    public static final int KEY_VERSION = 6;
    public static final int KEY_GONEOFF = 7;
    public static final int KEY_TRANSMIT = 8;
    public static final int KEY_AUTO_RESET = 9;

    public static final int CTRL_TRANSMIT_DONE = 1;
    public static final int CTRL_VERSION_DONE = 2;
    public static final int CTRL_GONEOFF_DONE = 4;
    public static final int CTRL_DO_NEXT = 8;
    public static final int CTRL_SET_LAST_SENT = 16;

    public static final UUID uuid = UUID.fromString("5be44f1d-d262-4ea6-aa30-ddbec1e3cab2");
    private final PebbleProtocol mPebbleProtocol;

    // data received from Morpheuz in native format
    private int smartalarm_from = -1; // time in minutes relative from 0:00 for smart alarm (earliest)
    private int smartalarm_to = -1;// time in minutes relative from 0:00 for smart alarm (latest)
    private int recording_base_timestamp = -1; // timestamp for the first "point", all folowing are +10 minutes offset each
    private int alarm_gone_off = -1; // time in minutes relative from 0:00 when alarm gone off

    private static final Logger LOG = LoggerFactory.getLogger(MorpheuzSupport.class);

    public MorpheuzSupport(PebbleProtocol pebbleProtocol) {
        mPebbleProtocol = pebbleProtocol;
    }

    private byte[] encodeMorpheuzMessage(int key, int value) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<Integer, Object>(key, value));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs);
    }

    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        int ctrl_message = 0;
        GBDeviceEventSleepMonitorResult sleepMonitorResult = null;

        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_TRANSMIT:
                case KEY_GONEOFF:
                    if (pair.first == KEY_GONEOFF) {
                        alarm_gone_off = (int) pair.second;
                        LOG.info("got gone off: " + alarm_gone_off / 60 + ":" + alarm_gone_off % 60);
                    }
                    sleepMonitorResult = new GBDeviceEventSleepMonitorResult();
                    sleepMonitorResult.smartalarm_from = smartalarm_from;
                    sleepMonitorResult.smartalarm_to = smartalarm_to;
                    sleepMonitorResult.alarm_gone_off = alarm_gone_off;
                    sleepMonitorResult.recording_base_timestamp = recording_base_timestamp;
                    break;
                case KEY_POINT:
                    if (recording_base_timestamp == -1) {
                        // we have no base timestamp but received points, stop this
                        ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_GONEOFF_DONE | MorpheuzSupport.CTRL_TRANSMIT_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT;
                    } else {
                        short index = (short) ((int) pair.second >> 16);
                        short intensity = (short) ((int) pair.second & 0xffff);
                        LOG.info("got point:" + index + " " + intensity);
                        byte type = MorpheuzSampleProvider.TYPE_UNKNOWN;
                        if (intensity <= 120) {
                            type = MorpheuzSampleProvider.TYPE_DEEP_SLEEP;
                        } else if (intensity <= 1000) {
                            type = MorpheuzSampleProvider.TYPE_LIGHT_SLEEP;
                        }
                        if (index >= 0) {
                            DBHandler db = null;
                            try {
                                db = GBApplication.acquireDB();
                                db.addGBActivitySample(recording_base_timestamp + index * 600, SampleProvider.PROVIDER_PEBBLE_MORPHEUZ, intensity, (byte) 0, type);
                            } catch (GBException e) {
                                LOG.error("Error acquiring database", e);
                            } finally {
                                if (db != null) {
                                    db.release();
                                }
                            }
                        }

                        ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    }
                    break;
                case KEY_FROM:
                    smartalarm_from = (int) pair.second;
                    LOG.info("got from: " + smartalarm_from / 60 + ":" + smartalarm_from % 60);
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    break;
                case KEY_TO:
                    smartalarm_to = (int) pair.second;
                    LOG.info("got from: " + smartalarm_to / 60 + ":" + smartalarm_to % 60);
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    break;
                case KEY_VERSION:
                    LOG.info("got version: " + ((float) ((int) pair.second) / 10.0f));
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT;
                    break;
                case KEY_BASE:
                    // fix timestamp
                    TimeZone tz = SimpleTimeZone.getDefault();
                    recording_base_timestamp = (int) pair.second - (tz.getOffset(System.currentTimeMillis())) / 1000;
                    LOG.info("got base: " + recording_base_timestamp);
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    break;
                case KEY_AUTO_RESET:
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }

        // always ack
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(uuid, mPebbleProtocol.last_id);

        // sometimes send control message
        GBDeviceEventSendBytes sendBytesCtrl = null;
        if (ctrl_message > 0) {
            sendBytesCtrl = new GBDeviceEventSendBytes();
            sendBytesCtrl.encodedBytes = encodeMorpheuzMessage(MorpheuzSupport.KEY_CTRL, ctrl_message);
        }

        // ctrl and sleep monitor might be null, thats okay
        return new GBDeviceEvent[]{sendBytesAck, sendBytesCtrl, sleepMonitorResult};
    }
}
