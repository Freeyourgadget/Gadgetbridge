package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
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
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

class AppMessageHandlerMorpheuz extends AppMessageHandler {
    private Integer keyPoint;
    private Integer keyCtrl;
    private Integer keyFrom;
    private Integer keyTo;
    private Integer keyBase;
    private Integer keyVersion;
    private Integer keyGoneoff;
    private Integer keyTransmit;
    private Integer keyAutoReset;
    private Integer keySnoozes;
    private Integer keyFault;

    private static final int CTRL_TRANSMIT_DONE = 1;
    private static final int CTRL_VERSION_DONE = 2;
    private static final int CTRL_GONEOFF_DONE = 4;
    private static final int CTRL_DO_NEXT = 8;
    private static final int CTRL_SET_LAST_SENT = 16;
    private static final int CTRL_LAZARUS = 32;
    private static final int CTRL_SNOOZES_DONE = 64;

    // data received from Morpheuz in native format
    private int smartalarm_from = -1; // time in minutes relative from 0:00 for smart alarm (earliest)
    private int smartalarm_to = -1;// time in minutes relative from 0:00 for smart alarm (latest)
    private int recording_base_timestamp = -1; // timestamp for the first "point", all folowing are +10 minutes offset each
    private int alarm_gone_off = -1; // time in minutes relative from 0:00 when alarm gone off

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerMorpheuz.class);

    public AppMessageHandlerMorpheuz(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);

        try {
            JSONObject appKeys = getAppKeys();
            keyPoint = appKeys.getInt("keyPoint");
            keyCtrl = appKeys.getInt("keyCtrl");
            keyFrom = appKeys.getInt("keyFrom");
            keyTo = appKeys.getInt("keyTo");
            keyBase = appKeys.getInt("keyBase");
            keyVersion = appKeys.getInt("keyVersion");
            keyGoneoff = appKeys.getInt("keyGoneoff");
            keyTransmit = appKeys.getInt("keyTransmit");
            keyAutoReset = appKeys.getInt("keyAutoReset");
            keySnoozes = appKeys.getInt("keySnoozes");
            keyFault = appKeys.getInt("keyFault");
        } catch (JSONException e) {
            GB.toast("There was an error accessing the morpheuz watchapp configuration.", Toast.LENGTH_LONG, GB.ERROR);
        } catch (IOException ignore) {
        }
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
            if (Objects.equals(pair.first, keyTransmit)) {
                sleepMonitorResult = new GBDeviceEventSleepMonitorResult();
                sleepMonitorResult.smartalarm_from = smartalarm_from;
                sleepMonitorResult.smartalarm_to = smartalarm_to;
                sleepMonitorResult.alarm_gone_off = alarm_gone_off;
                sleepMonitorResult.recording_base_timestamp = recording_base_timestamp;
                ctrl_message |= CTRL_TRANSMIT_DONE;
            } else if (pair.first.equals(keyGoneoff)) {
                alarm_gone_off = (int) pair.second;
                LOG.info("got gone off: " + alarm_gone_off / 60 + ":" + alarm_gone_off % 60);
                ctrl_message |= CTRL_DO_NEXT | CTRL_GONEOFF_DONE;
            } else if (pair.first.equals(keyPoint)) {
                if (recording_base_timestamp == -1) {
                    // we have no base timestamp but received points, stop this
                    ctrl_message = CTRL_VERSION_DONE | CTRL_GONEOFF_DONE | CTRL_TRANSMIT_DONE | CTRL_SET_LAST_SENT;
                } else {
                    int index = ((int) pair.second >> 16);
                    int intensity = ((int) pair.second & 0xffff);
                    LOG.info("got point:" + index + " " + intensity);
                    if (index >= 0) {
                        try (DBHandler db = GBApplication.acquireDB()) {
                            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                            Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
                            PebbleMorpheuzSampleProvider sampleProvider = new PebbleMorpheuzSampleProvider(getDevice(), db.getDaoSession());
                            PebbleMorpheuzSample sample = new PebbleMorpheuzSample(recording_base_timestamp + index * 600, deviceId, userId, intensity);
                            sample.setProvider(sampleProvider);
                            sampleProvider.addGBActivitySample(sample);
                        } catch (Exception e) {
                            LOG.error("Error acquiring database", e);
                        }
                    }

                    ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
                }
            } else if (pair.first.equals(keyFrom)) {
                smartalarm_from = (int) pair.second;
                LOG.info("got from: " + smartalarm_from / 60 + ":" + smartalarm_from % 60);
                ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
            } else if (pair.first.equals(keyTo)) {
                smartalarm_to = (int) pair.second;
                LOG.info("got to: " + smartalarm_to / 60 + ":" + smartalarm_to % 60);
                ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
            } else if (pair.first.equals(keyVersion)) {
                int version = (int) pair.second;
                LOG.info("got version: " + ((float) version / 10.0f));
                ctrl_message |= CTRL_VERSION_DONE;
            } else if (pair.first.equals(keyBase)) {// fix timestamp
                TimeZone tz = SimpleTimeZone.getDefault();
                recording_base_timestamp = (int) pair.second - (tz.getOffset(System.currentTimeMillis())) / 1000;
                LOG.info("got base: " + recording_base_timestamp);
                ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
            } else if (pair.first.equals(keyAutoReset)) {
                ctrl_message |= CTRL_SET_LAST_SENT | CTRL_DO_NEXT;
            } else if (pair.first.equals(keySnoozes)) {
                ctrl_message |= CTRL_SNOOZES_DONE | CTRL_DO_NEXT;
            } else if (pair.first.equals(keyFault)) {
                LOG.info("fault code: " + (int) pair.second);
                ctrl_message |= CTRL_DO_NEXT;
            } else {
                LOG.info("unhandled key: " + pair.first);
            }
        }

        // always ack
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        // sometimes send control message
        GBDeviceEventSendBytes sendBytesCtrl = null;
        if (ctrl_message > 0) {
            sendBytesCtrl = new GBDeviceEventSendBytes();
            sendBytesCtrl.encodedBytes = encodeMorpheuzMessage(keyCtrl, ctrl_message);
        }

        // ctrl and sleep monitor might be null, thats okay
        return new GBDeviceEvent[]{sendBytesAck, sendBytesCtrl, sleepMonitorResult};
    }
}
