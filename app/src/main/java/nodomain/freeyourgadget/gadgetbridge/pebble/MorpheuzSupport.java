package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSleepMonitorResult;

public class MorpheuzSupport {

    public static final int KEY_POINT = 1;
    public static final int KEY_CTRL = 2;
    public static final int KEY_FROM = 3;
    public static final int KEY_TO = 4;
    public static final int KEY_BASE = 5;
    public static final int KEY_VERSION = 6;
    public static final int KEY_GONEOFF = 7;
    public static final int KEY_TRANSMIT = 8;

    public static final int CTRL_TRANSMIT_DONE = 1;
    public static final int CTRL_VERSION_DONE = 2;
    public static final int CTRL_GONEOFF_DONE = 4;
    public static final int CTRL_DO_NEXT = 8;
    public static final int CTRL_SET_LAST_SENT = 16;

    public static final UUID uuid = UUID.fromString("5be44f1d-d262-4ea6-aa30-ddbec1e3cab2");
    private final PebbleProtocol mPebbleProtocol;

    private boolean sent_to_gadgetbridge = false;
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
        byte[] ackMessage = mPebbleProtocol.encodeApplicationMessageAck(uuid, mPebbleProtocol.last_id);
        byte[] testMessage = mPebbleProtocol.encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs);

        ByteBuffer buf = ByteBuffer.allocate(ackMessage.length + testMessage.length);

        // encode ack and put in front of push message (hack for acknowledging the last message)
        buf.put(ackMessage);
        buf.put(testMessage);

        return buf.array();
    }

    public GBDeviceCommand handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        for (Pair<Integer, Object> pair : pairs) {
            int ctrl_message = 0;
            switch (pair.first) {
                case KEY_TRANSMIT:
                case KEY_GONEOFF:
                    if (pair.first == KEY_GONEOFF) {
                        alarm_gone_off = (int) pair.second;
                        LOG.info("got gone off: " + alarm_gone_off / 60 + ":" + alarm_gone_off % 60);
                    }
                    /* super-ugly hack: if if did not notice GadgetBridge yet, do so and delay confirmation so Morpheuz
                     * will resend gone off data. The second time, we acknowledge it.
                     *
                     * this can be fixed by allowing to return multiple GBDeviceCommands
                     */
                    if (sent_to_gadgetbridge) {
                        ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_GONEOFF_DONE | MorpheuzSupport.CTRL_TRANSMIT_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT;
                    } else {
                        GBDeviceCommandSleepMonitorResult sleepMonitorResult = new GBDeviceCommandSleepMonitorResult();
                        sleepMonitorResult.smartalarm_from = smartalarm_from;
                        sleepMonitorResult.smartalarm_to = smartalarm_to;
                        sleepMonitorResult.alarm_gone_off = alarm_gone_off;
                        sleepMonitorResult.recording_base_timestamp = recording_base_timestamp;
                        sent_to_gadgetbridge = true;
                        return sleepMonitorResult;
                    }
                    break;
                case KEY_POINT:
                    if (recording_base_timestamp == -1) {
                        // we have no base timestamp but received points, stop this
                        ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_GONEOFF_DONE | MorpheuzSupport.CTRL_TRANSMIT_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT;
                    } else {
                        short index = (short) ((int) pair.second >> 16);
                        short data = (short) ((int) pair.second & 0xffff);
                        LOG.info("got point:" + index + " " + data);
                        if (index >= 0 && index < 54) {
                            GBApplication.getActivityDatabaseHandler().addGBActivitySample(recording_base_timestamp + index * 600, GBActivitySample.PROVIDER_PEBBLE_MORPHEUZ, data, (byte) 0, GBActivitySample.TYPE_SLEEP);
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
                    sent_to_gadgetbridge = false;
                    break;
                case KEY_BASE:
                    // fix timestamp
                    TimeZone tz = SimpleTimeZone.getDefault();
                    recording_base_timestamp = (int) pair.second - (tz.getOffset(System.currentTimeMillis())) / 1000;
                    LOG.info("got base: " + recording_base_timestamp);
                    ctrl_message = MorpheuzSupport.CTRL_VERSION_DONE | MorpheuzSupport.CTRL_SET_LAST_SENT | MorpheuzSupport.CTRL_DO_NEXT;
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
            if (ctrl_message > 0) {
                GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
                sendBytes.encodedBytes = encodeMorpheuzMessage(MorpheuzSupport.KEY_CTRL, ctrl_message);
                return sendBytes;
            }
        }
        GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
        sendBytes.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(uuid, mPebbleProtocol.last_id);
        return sendBytes;
    }
}
