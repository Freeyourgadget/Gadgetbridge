package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepMonitorResult;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleMorpheuzSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSample;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class AppMessageHandlerMorpheuz extends AppMessageHandler {

    public static final int KEY_POINT = 1;
    public static final int KEY_POINT_46 = 10000;
    public static final int KEY_CTRL = 2;
    public static final int KEY_CTRL_46 = 10001;
    public static final int KEY_FROM = 3;
    public static final int KEY_FROM_46 = 10002;
    public static final int KEY_TO = 4;
    public static final int KEY_TO_46 = 10003;
    public static final int KEY_BASE = 5;
    public static final int KEY_BASE_46 = 10004;
    public static final int KEY_VERSION = 6;
    public static final int KEY_VERSION_46 = 10005;
    public static final int KEY_GONEOFF = 7;
    public static final int KEY_GONEOFF_46 = 10006;
    public static final int KEY_TRANSMIT = 8;
    public static final int KEY_TRANSMIT_46 = 10007;
    public static final int KEY_AUTO_RESET = 9;
    public static final int KEY_AUTO_RESET_46 = 10008;
    public static final int KEY_SNOOZES = 10;
    public static final int KEY_SNOOZES_46 = 10009;
    public static final int KEY_FAULT_46 = 10010;

    public static final int CTRL_TRANSMIT_DONE = 1;
    public static final int CTRL_VERSION_DONE = 2;
    public static final int CTRL_GONEOFF_DONE = 4;
    public static final int CTRL_DO_NEXT = 8;
    public static final int CTRL_SET_LAST_SENT = 16;
    public static final int CTRL_LAZARUS = 32;
    public static final int CTRL_SNOOZES_DONE = 64;

    // data received from Morpheuz in native format
    private int version = 0;
    private int smartalarm_from = -1; // time in minutes relative from 0:00 for smart alarm (earliest)
    private int smartalarm_to = -1;// time in minutes relative from 0:00 for smart alarm (latest)
    private int recording_base_timestamp = -1; // timestamp for the first "point", all folowing are +10 minutes offset each
    private int alarm_gone_off = -1; // time in minutes relative from 0:00 when alarm gone off

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerMorpheuz.class);

    public AppMessageHandlerMorpheuz(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    private byte[] encodeMorpheuzMessage(int key, int value) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<Integer, Object>(key, value));

        return mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, mUUID, pairs);
    }

    @Override
    public boolean isEnabled() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean("pebble_sync_morpheuz", true);
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        int ctrl_message = 0;
        GBDeviceEventSleepMonitorResult sleepMonitorResult = null;

        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_TRANSMIT:
                case KEY_TRANSMIT_46:
                    sleepMonitorResult = new GBDeviceEventSleepMonitorResult();
                    sleepMonitorResult.smartalarm_from = smartalarm_from;
                    sleepMonitorResult.smartalarm_to = smartalarm_to;
                    sleepMonitorResult.alarm_gone_off = alarm_gone_off;
                    sleepMonitorResult.recording_base_timestamp = recording_base_timestamp;
                    ctrl_message |= CTRL_TRANSMIT_DONE;
                    break;
                case KEY_GONEOFF:
                case KEY_GONEOFF_46:
                    alarm_gone_off = (int) pair.second;
                    LOG.info("got gone off: " + alarm_gone_off / 60 + ":" + alarm_gone_off % 60);
                    ctrl_message |= CTRL_DO_NEXT | CTRL_GONEOFF_DONE;
                    break;
                case KEY_POINT:
                case KEY_POINT_46:
                    if (recording_base_timestamp == -1) {
                        // we have no base timestamp but received points, stop this
                        ctrl_message = CTRL_VERSION_DONE | CTRL_GONEOFF_DONE | CTRL_TRANSMIT_DONE | CTRL_SET_LAST_SENT;
                    } else {
                        int index = ((int) pair.second >> 16);
                        int intensity = ((int) pair.second & 0xffff);
                        LOG.info("got point:" + index + " " + intensity);
                        int type = PebbleMorpheuzSampleProvider.TYPE_ACTIVITY;
                        if (intensity <= 120) {
                            type = PebbleMorpheuzSampleProvider.TYPE_DEEP_SLEEP;
                        } else if (intensity <= 1000) {
                            type = PebbleMorpheuzSampleProvider.TYPE_LIGHT_SLEEP;
                        }
                        if (index >= 0) {
                            try (DBHandler db = GBApplication.acquireDB()) {
                                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                                Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                                PebbleMorpheuzSampleProvider sampleProvider = new PebbleMorpheuzSampleProvider(getDevice(), db.getDaoSession());
                                PebbleMorpheuzSample sample = new PebbleMorpheuzSample(null, recording_base_timestamp + index * 600, intensity, type, userId, deviceId);
                                sample.setProvider(sampleProvider);
                                sampleProvider.addGBActivitySample(sample);
                            } catch (Exception e) {
                                LOG.error("Error acquiring database", e);
                            }
                        }

                        ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                    }
                    break;
                case KEY_FROM:
                case KEY_FROM_46:
                    smartalarm_from = (int) pair.second;
                    LOG.info("got from: " + smartalarm_from / 60 + ":" + smartalarm_from % 60);
                    ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                    break;
                case KEY_TO:
                case KEY_TO_46:
                    smartalarm_to = (int) pair.second;
                    LOG.info("got to: " + smartalarm_to / 60 + ":" + smartalarm_to % 60);
                    ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                    break;
                case KEY_VERSION:
                case KEY_VERSION_46:
                    version = (int) pair.second;
                    LOG.info("got version: " + ((float) version / 10.0f));
                    ctrl_message |= CTRL_VERSION_DONE;
                    break;
                case KEY_BASE:
                case KEY_BASE_46:
                    // fix timestamp
                    TimeZone tz = SimpleTimeZone.getDefault();
                    recording_base_timestamp = (int) pair.second - (tz.getOffset(System.currentTimeMillis())) / 1000;
                    LOG.info("got base: " + recording_base_timestamp);
                    ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                    break;
                case KEY_AUTO_RESET:
                case KEY_AUTO_RESET_46:
                    ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                    break;
                case KEY_SNOOZES:
                case KEY_SNOOZES_46:
                    ctrl_message |= CTRL_SNOOZES_DONE | CTRL_DO_NEXT;
                    break;
                case KEY_FAULT_46:
                    LOG.info("fault code: " + (int) pair.second);
                    ctrl_message |= CTRL_DO_NEXT;
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }

        // always ack
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        // sometimes send control message
        GBDeviceEventSendBytes sendBytesCtrl = null;
        if (ctrl_message > 0) {
            sendBytesCtrl = new GBDeviceEventSendBytes();
            int ctrlkey = KEY_CTRL;
            if (version >= 46) {
                ctrlkey = KEY_CTRL_46;
            }
            sendBytesCtrl.encodedBytes = encodeMorpheuzMessage(ctrlkey, ctrl_message);
        }

        // ctrl and sleep monitor might be null, thats okay
        return new GBDeviceEvent[]{sendBytesAck, sendBytesCtrl, sleepMonitorResult};
    }
}
