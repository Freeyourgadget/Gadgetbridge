package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Base64;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppManagement;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleIconID;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PebbleProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleProtocol.class);

    static final short ENDPOINT_TIME = 11;
    static final short ENDPOINT_FIRMWAREVERSION = 16;
    public static final short ENDPOINT_PHONEVERSION = 17;
    static final short ENDPOINT_SYSTEMMESSAGE = 18;
    static final short ENDPOINT_MUSICCONTROL = 32;
    static final short ENDPOINT_PHONECONTROL = 33;
    static final short ENDPOINT_APPLICATIONMESSAGE = 48;
    static final short ENDPOINT_LAUNCHER = 49;
    static final short ENDPOINT_APPRUNSTATE = 52; // 3.x only
    static final short ENDPOINT_LOGS = 2000;
    static final short ENDPOINT_PING = 2001;
    static final short ENDPOINT_LOGDUMP = 2002;
    static final short ENDPOINT_RESET = 2003;
    static final short ENDPOINT_APP = 2004;
    static final short ENDPOINT_APPLOGS = 2006;
    static final short ENDPOINT_NOTIFICATION = 3000;
    static final short ENDPOINT_EXTENSIBLENOTIFS = 3010;
    static final short ENDPOINT_RESOURCE = 4000;
    static final short ENDPOINT_SYSREG = 5000;
    static final short ENDPOINT_FCTREG = 5001;
    static final short ENDPOINT_APPMANAGER = 6000;
    static final short ENDPOINT_APPFETCH = 6001; // 3.x only
    public static final short ENDPOINT_DATALOG = 6778;
    static final short ENDPOINT_RUNKEEPER = 7000;
    static final short ENDPOINT_SCREENSHOT = 8000;
    static final short ENDPOINT_NOTIFICATIONACTION = 11440; // 3.x only, TODO: find a better name
    static final short ENDPOINT_BLOBDB = (short) 45531;  // 3.x only
    static final short ENDPOINT_PUTBYTES = (short) 48879;

    static final byte APPRUNSTATE_START = 1;
    static final byte APPRUNSTATE_STOP = 2;

    static final byte BLOBDB_INSERT = 1;
    static final byte BLOBDB_DELETE = 4;
    static final byte BLOBDB_CLEAR = 5;

    static final byte BLOBDB_PIN = 1;
    static final byte BLOBDB_APP = 2;
    static final byte BLOBDB_REMINDER = 3;
    static final byte BLOBDB_NOTIFICATION = 4;

    static final byte BLOBDB_SUCCESS = 1;
    static final byte BLOBDB_GENERALFAILURE = 2;
    static final byte BLOBDB_INVALIDOPERATION = 3;
    static final byte BLOBDB_INVALIDDATABASEID = 4;
    static final byte BLOBDB_INVALIDDATA = 5;
    static final byte BLOBDB_KEYDOESNOTEXIST = 6;
    static final byte BLOBDB_DATABASEFULL = 7;
    static final byte BLOBDB_DATASTALE = 8;


    // This is not in the Pebble protocol
    static final byte NOTIFICATION_UNDEFINED = -1;

    static final byte NOTIFICATION_EMAIL = 0;
    static final byte NOTIFICATION_SMS = 1;
    static final byte NOTIFICATION_TWITTER = 2;
    static final byte NOTIFICATION_FACEBOOK = 3;

    static final byte PHONECONTROL_ANSWER = 1;
    static final byte PHONECONTROL_HANGUP = 2;
    static final byte PHONECONTROL_GETSTATE = 3;
    static final byte PHONECONTROL_INCOMINGCALL = 4;
    static final byte PHONECONTROL_OUTGOINGCALL = 5;
    static final byte PHONECONTROL_MISSEDCALL = 6;
    static final byte PHONECONTROL_RING = 7;
    static final byte PHONECONTROL_START = 8;
    static final byte PHONECONTROL_END = 9;

    static final byte MUSICCONTROL_SETMUSICINFO = 16;
    static final byte MUSICCONTROL_PLAYPAUSE = 1;
    static final byte MUSICCONTROL_PAUSE = 2;
    static final byte MUSICCONTROL_PLAY = 3;
    static final byte MUSICCONTROL_NEXT = 4;
    static final byte MUSICCONTROL_PREVIOUS = 5;
    static final byte MUSICCONTROL_VOLUMEUP = 6;
    static final byte MUSICCONTROL_VOLUMEDOWN = 7;
    static final byte MUSICCONTROL_GETNOWPLAYING = 7;

    static final byte NOTIFICATIONACTION_ACK = 0;
    static final byte NOTIFICATIONACTION_NACK = 1;
    static final byte NOTIFICATIONACTION_INVOKE = 0x02;
    static final byte NOTIFICATIONACTION_RESPONSE = 0x11;

    static final byte TIME_GETTIME = 0;
    static final byte TIME_SETTIME = 2;
    static final byte TIME_SETTIME_UTC = 3;

    static final byte FIRMWAREVERSION_GETVERSION = 0;

    static final byte APPMANAGER_GETAPPBANKSTATUS = 1;
    static final byte APPMANAGER_REMOVEAPP = 2;
    static final byte APPMANAGER_REFRESHAPP = 3;
    static final byte APPMANAGER_GETUUIDS = 5;

    static final int APPMANAGER_RES_SUCCESS = 1;

    static final byte APPLICATIONMESSAGE_PUSH = 1;
    static final byte APPLICATIONMESSAGE_REQUEST = 2;
    static final byte APPLICATIONMESSAGE_ACK = (byte) 0xff;
    static final byte APPLICATIONMESSAGE_NACK = (byte) 0x7f;

    static final byte DATALOG_OPENSESSION = 0x01;
    static final byte DATALOG_SENDDATA = 0x02;
    static final byte DATALOG_CLOSE = 0x03;
    static final byte DATALOG_TIMEOUT = 0x07;
    static final byte DATALOG_REPORTSESSIONS = (byte) 0x84;
    static final byte DATALOG_ACK = (byte) 0x85;
    static final byte DATALOG_NACK = (byte) 0x86;

    static final byte PING_PING = 0;
    static final byte PING_PONG = 1;

    static final byte PUTBYTES_INIT = 1;
    static final byte PUTBYTES_SEND = 2;
    static final byte PUTBYTES_COMMIT = 3;
    static final byte PUTBYTES_ABORT = 4;
    static final byte PUTBYTES_COMPLETE = 5;

    public static final byte PUTBYTES_TYPE_FIRMWARE = 1;
    public static final byte PUTBYTES_TYPE_RECOVERY = 2;
    public static final byte PUTBYTES_TYPE_SYSRESOURCES = 3;
    public static final byte PUTBYTES_TYPE_RESOURCES = 4;
    public static final byte PUTBYTES_TYPE_BINARY = 5;
    public static final byte PUTBYTES_TYPE_FILE = 6;
    public static final byte PUTBYTES_TYPE_WORKER = 7;

    static final byte RESET_REBOOT = 0;

    static final byte SCREENSHOT_TAKE = 0;

    static final byte SYSTEMMESSAGE_NEWFIRMWAREAVAILABLE = 0;
    static final byte SYSTEMMESSAGE_FIRMWARESTART = 1;
    static final byte SYSTEMMESSAGE_FIRMWARECOMPLETE = 2;
    static final byte SYSTEMMESSAGE_FIRMWAREFAIL = 3;
    static final byte SYSTEMMESSAGE_FIRMWARE_UPTODATE = 4;
    static final byte SYSTEMMESSAGE_FIRMWARE_OUTOFDATE = 5;
    static final byte SYSTEMMESSAGE_STOPRECONNECTING = 6;
    static final byte SYSTEMMESSAGE_STARTRECONNECTING = 7;

    static final byte PHONEVERSION_REQUEST = 0;
    static final byte PHONEVERSION_APPVERSION_MAGIC = 2; // increase this if pebble complains
    static final byte PHONEVERSION_APPVERSION_MAJOR = 2;
    static final byte PHONEVERSION_APPVERSION_MINOR = 3;
    static final byte PHONEVERSION_APPVERSION_PATCH = 0;


    static final int PHONEVERSION_SESSION_CAPS_GAMMARAY = 0x80000000;

    static final int PHONEVERSION_REMOTE_CAPS_TELEPHONY = 0x00000010;
    static final int PHONEVERSION_REMOTE_CAPS_SMS = 0x00000020;
    static final int PHONEVERSION_REMOTE_CAPS_GPS = 0x00000040;
    static final int PHONEVERSION_REMOTE_CAPS_BTLE = 0x00000080;
    static final int PHONEVERSION_REMOTE_CAPS_REARCAMERA = 0x00000100;
    static final int PHONEVERSION_REMOTE_CAPS_ACCEL = 0x00000200;
    static final int PHONEVERSION_REMOTE_CAPS_GYRO = 0x00000400;
    static final int PHONEVERSION_REMOTE_CAPS_COMPASS = 0x00000800;

    static final byte PHONEVERSION_REMOTE_OS_UNKNOWN = 0;
    static final byte PHONEVERSION_REMOTE_OS_IOS = 1;
    static final byte PHONEVERSION_REMOTE_OS_ANDROID = 2;
    static final byte PHONEVERSION_REMOTE_OS_OSX = 3;
    static final byte PHONEVERSION_REMOTE_OS_LINUX = 4;
    static final byte PHONEVERSION_REMOTE_OS_WINDOWS = 5;

    static final byte TYPE_BYTEARRAY = 0;
    static final byte TYPE_CSTRING = 1;
    static final byte TYPE_UINT = 2;
    static final byte TYPE_INT = 3;

    static final short LENGTH_PREFIX = 4;
    static final short LENGTH_SIMPLEMESSAGE = 1;

    static final short LENGTH_APPFETCH = 2;
    static final short LENGTH_APPRUNSTATE = 17;
    static final short LENGTH_BLOBDB = 21;
    static final short LENGTH_PING = 5;
    static final short LENGTH_PHONEVERSION = 17;
    static final short LENGTH_REMOVEAPP_2X = 17;
    static final short LENGTH_REFRESHAPP = 5;
    static final short LENGTH_SETTIME = 5;
    static final short LENGTH_SYSTEMMESSAGE = 2;
    static final short LENGTH_UPLOADSTART_2X = 7;
    static final short LENGTH_UPLOADSTART_3X = 10;
    static final short LENGTH_UPLOADCHUNK = 9;
    static final short LENGTH_UPLOADCOMMIT = 9;
    static final short LENGTH_UPLOADCOMPLETE = 5;
    static final short LENGTH_UPLOADCANCEL = 5;

    static final byte LENGTH_UUID = 16;

    // base is -5
    private static final String[] hwRevisions = {
            // Emulator
            "spalding_bb2", "snowy_bb2", "snowy_bb", "bb2", "bb",
            "unknown",
            // Pebble
            "ev1", "ev2", "ev2_3", "ev2_4", "v1_5", "v2_0",
            // Pebble Time
            "snowy_evt2", "snowy_dvt", "spalding_dvt", "snowy_s3", "spalding"
    };

    private static final Random mRandom = new Random();

    boolean isFw3x = false;
    boolean mForceProtocol = false;
    GBDeviceEventScreenshot mDevEventScreenshot = null;
    int mScreenshotRemaining = -1;

    //monochrome black + white
    static final byte[] clut_pebble = {
            0x00, 0x00, 0x00, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00
    };

    // linear BGR222 (6 bit, 64 entries)
    static final byte[] clut_pebbletime = new byte[]{
            0x00, 0x00, 0x00, 0x00,
            0x55, 0x00, 0x00, 0x00,
            (byte) 0xaa, 0x00, 0x00, 0x00,
            (byte) 0xff, 0x00, 0x00, 0x00,

            0x00, 0x55, 0x00, 0x00,
            0x55, 0x55, 0x00, 0x00,
            (byte) 0xaa, 0x55, 0x00, 0x00,
            (byte) 0xff, 0x55, 0x00, 0x00,

            0x00, (byte) 0xaa, 0x00, 0x00,
            0x55, (byte) 0xaa, 0x00, 0x00,
            (byte) 0xaa, (byte) 0xaa, 0x00, 0x00,
            (byte) 0xff, (byte) 0xaa, 0x00, 0x00,

            0x00, (byte) 0xff, 0x00, 0x00,
            0x55, (byte) 0xff, 0x00, 0x00,
            (byte) 0xaa, (byte) 0xff, 0x00, 0x00,
            (byte) 0xff, (byte) 0xff, 0x00, 0x00,

            0x00, 0x00, 0x55, 0x00,
            0x55, 0x00, 0x55, 0x00,
            (byte) 0xaa, 0x00, 0x55, 0x00,
            (byte) 0xff, 0x00, 0x55, 0x00,

            0x00, 0x55, 0x55, 0x00,
            0x55, 0x55, 0x55, 0x00,
            (byte) 0xaa, 0x55, 0x55, 0x00,
            (byte) 0xff, 0x55, 0x55, 0x00,

            0x00, (byte) 0xaa, 0x55, 0x00,
            0x55, (byte) 0xaa, 0x55, 0x00,
            (byte) 0xaa, (byte) 0xaa, 0x55, 0x00,
            (byte) 0xff, (byte) 0xaa, 0x55, 0x00,

            0x00, (byte) 0xff, 0x55, 0x00,
            0x55, (byte) 0xff, 0x55, 0x00,
            (byte) 0xaa, (byte) 0xff, 0x55, 0x00,
            (byte) 0xff, (byte) 0xff, 0x55, 0x00,

            0x00, 0x00, (byte) 0xaa, 0x00,
            0x55, 0x00, (byte) 0xaa, 0x00,
            (byte) 0xaa, 0x00, (byte) 0xaa, 0x00,
            (byte) 0xff, 0x00, (byte) 0xaa, 0x00,

            0x00, 0x55, (byte) 0xaa, 0x00,
            0x55, 0x55, (byte) 0xaa, 0x00,
            (byte) 0xaa, 0x55, (byte) 0xaa, 0x00,
            (byte) 0xff, 0x55, (byte) 0xaa, 0x00,

            0x00, (byte) 0xaa, (byte) 0xaa, 0x00,
            0x55, (byte) 0xaa, (byte) 0xaa, 0x00,
            (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, 0x00,
            (byte) 0xff, (byte) 0xaa, (byte) 0xaa, 0x00,

            0x00, (byte) 0xff, (byte) 0xaa, 0x00,
            0x55, (byte) 0xff, (byte) 0xaa, 0x00,
            (byte) 0xaa, (byte) 0xff, (byte) 0xaa, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xaa, 0x00,

            0x00, 0x00, (byte) 0xff, 0x00,
            0x55, 0x00, (byte) 0xff, 0x00,
            (byte) 0xaa, 0x00, (byte) 0xff, 0x00,
            (byte) 0xff, 0x00, (byte) 0xff, 0x00,

            0x00, 0x55, (byte) 0xff, 0x00,
            0x55, 0x55, (byte) 0xff, 0x00,
            (byte) 0xaa, 0x55, (byte) 0xff, 0x00,
            (byte) 0xff, 0x55, (byte) 0xff, 0x00,

            0x00, (byte) 0xaa, (byte) 0xff, 0x00,
            0x55, (byte) 0xaa, (byte) 0xff, 0x00,
            (byte) 0xaa, (byte) 0xaa, (byte) 0xff, 0x00,
            (byte) 0xff, (byte) 0xaa, (byte) 0xff, 0x00,

            0x00, (byte) 0xff, (byte) 0xff, 0x00,
            0x55, (byte) 0xff, (byte) 0xff, 0x00,
            (byte) 0xaa, (byte) 0xff, (byte) 0xff, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00,

    };


    byte last_id = -1;
    private final ArrayList<UUID> tmpUUIDS = new ArrayList<>();

    private static final UUID UUID_GBPEBBLE = UUID.fromString("61476764-7465-7262-6469-656775527a6c");
    private static final UUID UUID_MORPHEUZ = UUID.fromString("5be44f1d-d262-4ea6-aa30-ddbec1e3cab2");
    private static final UUID UUID_WHETHERNEAT = UUID.fromString("3684003b-a685-45f9-a713-abc6364ba051");
    private static final UUID UUID_MISFIT = UUID.fromString("0b73b76a-cd65-4dc2-9585-aaa213320858");
    private static final UUID UUID_PEBBLE_HEALTH = UUID.fromString("36d8c6ed-4c83-4fa1-a9e2-8f12dc941f8c");

    private static final Map<UUID, AppMessageHandler> mAppMessageHandlers = new HashMap<>();

    {
        mAppMessageHandlers.put(UUID_GBPEBBLE, new AppMessageHandlerGBPebble(UUID_GBPEBBLE, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_MORPHEUZ, new AppMessageHandlerMorpheuz(UUID_MORPHEUZ, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_WHETHERNEAT, new AppMessageHandlerWeatherNeat(UUID_WHETHERNEAT, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_MISFIT, new AppMessageHandlerMisfit(UUID_MISFIT, PebbleProtocol.this));
    }

    private static byte[] encodeSimpleMessage(short endpoint, byte command) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SIMPLEMESSAGE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SIMPLEMESSAGE);
        buf.putShort(endpoint);
        buf.put(command);

        return buf.array();
    }

    private static byte[] encodeMessage(short endpoint, byte type, int cookie, String[] parts) {
        // Calculate length first
        int length = LENGTH_PREFIX + 1;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    length++; // encode null or empty strings as 0x00 later
                    continue;
                }
                length += (1 + s.getBytes().length);
            }
        }
        if (endpoint == ENDPOINT_PHONECONTROL) {
            length += 4; //for cookie;
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length - LENGTH_PREFIX));
        buf.putShort(endpoint);
        buf.put(type);

        if (endpoint == ENDPOINT_PHONECONTROL) {
            buf.putInt(cookie);
        }
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    //buf.put((byte)0x01);
                    buf.put((byte) 0x00);
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put((byte) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }
        return buf.array();
    }

    @Override
    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        boolean hasHandle = notificationSpec.id != -1 && notificationSpec.phoneNumber == null;
        int id = notificationSpec.id != -1 ? notificationSpec.id : mRandom.nextInt();
        String title;
        String subtitle = null;

        // for SMS and EMAIL that came in though SMS or K9 receiver
        if (notificationSpec.sender != null) {
            title = notificationSpec.sender;
            subtitle = notificationSpec.subject;
        } else {
            title = notificationSpec.title;
        }

        Long ts = System.currentTimeMillis();
        if (!isFw3x) {
            ts += (SimpleTimeZone.getDefault().getOffset(ts));
        }
        ts /= 1000;

        if (isFw3x) {
            // 3.x notification
            //return encodeTimelinePin(id, (int) ((ts + 600) & 0xffffffffL), (short) 90, PebbleIconID.TIMELINE_CALENDAR, title); // really, this is just for testing
            return encodeBlobdbNotification(id, (int) (ts & 0xffffffffL), title, subtitle, notificationSpec.body, notificationSpec.sourceName, hasHandle, notificationSpec.type, notificationSpec.cannedReplies);
        } else if (mForceProtocol || notificationSpec.type != NotificationType.EMAIL) {
            // 2.x notification
            return encodeExtensibleNotification(id, (int) (ts & 0xffffffffL), title, subtitle, notificationSpec.body, notificationSpec.sourceName, hasHandle, notificationSpec.cannedReplies);
        } else {
            // 1.x notification on FW 2.X
            String[] parts = {title, notificationSpec.body, ts.toString(), subtitle};
            // be aware that type is at this point always NOTIFICATION_EMAIL
            return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_EMAIL, 0, parts);
        }
    }

    @Override
    public byte[] encodeSetTime() {
        long ts = System.currentTimeMillis();
        long ts_offset = (SimpleTimeZone.getDefault().getOffset(ts));
        ByteBuffer buf;
        if (isFw3x) {
            String timezone = SimpleTimeZone.getDefault().getID();
            short length = (short) (LENGTH_SETTIME + timezone.getBytes().length + 3);
            buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(length);
            buf.putShort(ENDPOINT_TIME);
            buf.put(TIME_SETTIME_UTC);
            buf.putInt((int) (ts / 1000));
            buf.putShort((short) (ts_offset / 60000));
            buf.put((byte) timezone.getBytes().length);
            buf.put(timezone.getBytes());
            LOG.info(timezone);
        } else {
            buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SETTIME);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(LENGTH_SETTIME);
            buf.putShort(ENDPOINT_TIME);
            buf.put(TIME_SETTIME);
            buf.putInt((int) ((ts + ts_offset) / 1000));
        }
        return buf.array();
    }

    @Override
    public byte[] encodeFindDevice(boolean start) {
        return encodeSetCallState("Where are you?", "Gadgetbridge", start ? ServiceCommand.CALL_INCOMING : ServiceCommand.CALL_END);
    }

    private static byte[] encodeExtensibleNotification(int id, int timestamp, String title, String subtitle, String body, String sourceName, boolean hasHandle, String[] cannedReplies) {
        final short ACTION_LENGTH_MIN = 10;

        String[] parts = {title, subtitle, body};

        // Calculate length first
        byte actions_count;
        short actions_length;
        String dismiss_string;
        String open_string = "Open on phone";
        String mute_string = "Mute";
        String reply_string = "Reply";
        if (sourceName != null) {
            mute_string += " " + sourceName;
        }

        byte dismiss_action_id;

        if (hasHandle) {
            actions_count = 3;
            dismiss_string = "Dismiss";
            dismiss_action_id = 0x02;
            actions_length = (short) (ACTION_LENGTH_MIN * actions_count + dismiss_string.getBytes().length + open_string.getBytes().length + mute_string.getBytes().length);
        } else {
            actions_count = 1;
            dismiss_string = "Dismiss all";
            dismiss_action_id = 0x03;
            actions_length = (short) (ACTION_LENGTH_MIN * actions_count + dismiss_string.getBytes().length);
        }

        int replies_length = -1;
        if (cannedReplies != null && cannedReplies.length > 0) {
            actions_count++;
            for (String reply : cannedReplies) {
                replies_length += reply.getBytes().length + 1;
            }
            actions_length += ACTION_LENGTH_MIN + reply_string.getBytes().length + replies_length + 3; // 3 = attribute id (byte) + length(short)
        }

        byte attributes_count = 0;

        int length = 21 + 10 + actions_length;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_count++;
                length += (3 + s.getBytes().length);
            }
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length + LENGTH_PREFIX);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length));
        buf.putShort(ENDPOINT_EXTENSIBLENOTIFS);

        buf.order(ByteOrder.LITTLE_ENDIAN); // !

        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x01); // add notifications
        buf.putInt(0x00000000); // flags - ?
        buf.putInt(id);
        buf.putInt(0x00000000); // ANCS id
        buf.putInt(timestamp);
        buf.put((byte) 0x01); // layout - ?
        buf.put(attributes_count);
        buf.put(actions_count);

        byte attribute_id = 0;
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                attribute_id++;
                if (s == null || s.equals("")) {
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put(attribute_id);
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }


        // dismiss action
        buf.put(dismiss_action_id);
        buf.put((byte) 0x04); // dismiss
        buf.put((byte) 0x01); // number attributes
        buf.put((byte) 0x01); // attribute id (title)
        buf.putShort((short) dismiss_string.getBytes().length);
        buf.put(dismiss_string.getBytes());

        // open and mute actions
        if (hasHandle) {
            buf.put((byte) 0x01);
            buf.put((byte) 0x02); // generic
            buf.put((byte) 0x01); // number attributes
            buf.put((byte) 0x01); // attribute id (title)
            buf.putShort((short) open_string.getBytes().length);
            buf.put(open_string.getBytes());

            buf.put((byte) 0x04);
            buf.put((byte) 0x02); // generic
            buf.put((byte) 0x01); // number attributes
            buf.put((byte) 0x01); // attribute id (title)
            buf.putShort((short) mute_string.getBytes().length);
            buf.put(mute_string.getBytes());

        }

        if (cannedReplies != null && replies_length > 0) {
            buf.put((byte) 0x05);
            buf.put((byte) 0x03); // reply action
            buf.put((byte) 0x02); // number attributes
            buf.put((byte) 0x01); // title
            buf.putShort((short) reply_string.getBytes().length);
            buf.put(reply_string.getBytes());
            buf.put((byte) 0x08); // canned replies
            buf.putShort((short) replies_length);
            for (int i = 0; i < cannedReplies.length - 1; i++) {
                buf.put(cannedReplies[i].getBytes());
                buf.put((byte) 0x00);
            }
            // last one must not be zero terminated, else we get an additional emply reply
            buf.put(cannedReplies[cannedReplies.length - 1].getBytes());
        }

        return buf.array();
    }

    private byte[] encodeBlobdb(UUID uuid, byte command, byte db, byte[] blob) {

        int length = LENGTH_BLOBDB;
        if (blob != null) {
            length += blob.length + 2;
        }

        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) length);
        buf.putShort(ENDPOINT_BLOBDB);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(command);
        buf.putShort((short) mRandom.nextInt()); // token
        buf.put(db);
        buf.put(LENGTH_UUID);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.order(ByteOrder.LITTLE_ENDIAN);

        if (blob != null) {
            buf.putShort((short) blob.length);
            buf.put(blob);
        }

        return buf.array();
    }

    private byte[] encodeBlobDBClear(byte database) {
        final short LENGTH_BLOBDB_CLEAR = 4;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_BLOBDB_CLEAR);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_BLOBDB_CLEAR);
        buf.putShort(ENDPOINT_BLOBDB);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(BLOBDB_CLEAR);
        buf.putShort((short) mRandom.nextInt()); // token
        buf.put(database);

        return buf.array();
    }

    private byte[] encodeTimelinePin(int id, int timestamp, short duration, int icon_id, String title, String subtitle) {
        final short TIMELINE_PIN_LENGTH = 46;

        icon_id |= 0x80000000;
        UUID uuid = new UUID(mRandom.nextLong(), ((long) mRandom.nextInt() << 32) | id);
        byte attributes_count = 2;
        byte actions_count = 0;

        int attributes_length = 10 + title.getBytes().length;
        if (subtitle != null && !subtitle.isEmpty()) {
            attributes_length += 3 + subtitle.getBytes().length;
            attributes_count += 1;
        }

        int pin_length = TIMELINE_PIN_LENGTH + attributes_length;
        ByteBuffer buf = ByteBuffer.allocate(pin_length);

        // pin - 46 bytes
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.putLong(0); // parent
        buf.putLong(0);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(timestamp); // 32-bit timestamp
        buf.putShort(duration);
        buf.put((byte) 0x02); // type (0x02 = pin)
        buf.putShort((short) 0x0001); // flags 0x0001 = ?
        buf.put((byte) 0x01); // layout was (0x02 = pin?), 0x01 needed for subtitle aber seems to do no harm if there isn't one

        buf.putShort((short) attributes_length); // total length of all attributes and actions in bytes
        buf.put(attributes_count);
        buf.put(actions_count);

        buf.put((byte) 4); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(icon_id);
        buf.put((byte) 1); // title
        buf.putShort((short) title.getBytes().length);
        buf.put(title.getBytes());
        if (subtitle != null && !subtitle.isEmpty()) {
            buf.put((byte) 2); //subtitle
            buf.putShort((short) subtitle.getBytes().length);
            buf.put(subtitle.getBytes());
        }


        return encodeBlobdb(uuid, BLOBDB_INSERT, BLOBDB_PIN, buf.array());
    }

    private byte[] encodeBlobdbNotification(int id, int timestamp, String title, String subtitle, String body, String sourceName, boolean hasHandle, NotificationType notificationType, String[] cannedReplies) {
        final short NOTIFICATION_PIN_LENGTH = 46;
        final short ACTION_LENGTH_MIN = 10;

        String[] parts = {title, subtitle, body};

        int icon_id;
        byte color_id;
        switch (notificationType) {
            case EMAIL:
                icon_id = PebbleIconID.GENERIC_EMAIL;
                color_id = PebbleColor.JaegerGreen;
                break;
            case SMS:
                icon_id = PebbleIconID.GENERIC_SMS;
                color_id = PebbleColor.VividViolet;
                break;
            default:
                switch (notificationType) {
                    case TWITTER:
                        icon_id = PebbleIconID.NOTIFICATION_TWITTER;
                        color_id = PebbleColor.BlueMoon;
                        break;
                    case EMAIL:
                        icon_id = PebbleIconID.GENERIC_EMAIL;
                        color_id = PebbleColor.JaegerGreen;
                        break;
                    case SMS:
                        icon_id = PebbleIconID.GENERIC_SMS;
                        color_id = PebbleColor.VividViolet;
                        break;
                    case FACEBOOK:
                        icon_id = PebbleIconID.NOTIFICATION_FACEBOOK;
                        color_id = PebbleColor.VeryLightBlue;
                        break;
                    case CHAT:
                        icon_id = PebbleIconID.NOTIFICATION_HIPCHAT;
                        color_id = PebbleColor.Inchworm;
                        break;
                    default:
                        icon_id = PebbleIconID.NOTIFICATION_GENERIC;
                        color_id = PebbleColor.Red;
                        break;
                }
                break;
        }
        // Calculate length first
        byte actions_count;
        short actions_length;
        String dismiss_string;
        String open_string = "Open on phone";
        String mute_string = "Mute";
        String reply_string = "Reply";
        if (sourceName != null) {
            mute_string += " " + sourceName;
        }

        byte dismiss_action_id;
        if (hasHandle) {
            actions_count = 3;
            dismiss_string = "Dismiss";
            dismiss_action_id = 0x02;
            actions_length = (short) (ACTION_LENGTH_MIN * actions_count + dismiss_string.getBytes().length + open_string.getBytes().length + mute_string.getBytes().length);
        } else {
            actions_count = 1;
            dismiss_string = "Dismiss all";
            dismiss_action_id = 0x03;
            actions_length = (short) (ACTION_LENGTH_MIN * actions_count + dismiss_string.getBytes().length);
        }

        int replies_length = -1;
        if (cannedReplies != null && cannedReplies.length > 0) {
            actions_count++;
            for (String reply : cannedReplies) {
                replies_length += reply.getBytes().length + 1;
            }
            actions_length += ACTION_LENGTH_MIN + reply_string.getBytes().length + replies_length + 3; // 3 = attribute id (byte) + length(short)
        }

        byte attributes_count = 2; // icon
        short attributes_length = (short) (11 + actions_length);
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_count++;
                attributes_length += (3 + s.getBytes().length);
            }
        }

        UUID uuid = UUID.randomUUID();
        short pin_length = (short) (NOTIFICATION_PIN_LENGTH + attributes_length);

        ByteBuffer buf = ByteBuffer.allocate(pin_length);

        // pin - 46 bytes
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putInt((int) (uuid.getLeastSignificantBits() >>> 32));
        buf.putInt(id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putInt((int) (uuid.getLeastSignificantBits() >>> 32));
        buf.putInt(id);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(timestamp); // 32-bit timestamp
        buf.putShort((short) 0); // duration
        buf.put((byte) 0x01); // type (0x01 = notification)
        buf.putShort((short) 0x0001); // flags 0x0001 = ?
        buf.put((byte) 0x04); // layout (0x04 = notification?)
        buf.putShort(attributes_length); // total length of all attributes and actions in bytes
        buf.put(attributes_count);
        buf.put(actions_count);

        byte attribute_id = 0;
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                attribute_id++;
                if (s == null || s.equals("")) {
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 512) partlength = 512;
                buf.put(attribute_id);
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }

        buf.put((byte) 4); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(0x80000000 | icon_id);

        buf.put((byte) 28); // background_color
        buf.putShort((short) 1); // length of int
        buf.put(color_id);

        // dismiss action
        buf.put(dismiss_action_id);
        buf.put((byte) 0x02); // generic action, dismiss did not do anything
        buf.put((byte) 0x01); // number attributes
        buf.put((byte) 0x01); // attribute id (title)
        buf.putShort((short) dismiss_string.getBytes().length);
        buf.put(dismiss_string.getBytes());

        // open and mute actions
        if (hasHandle) {
            buf.put((byte) 0x01);
            buf.put((byte) 0x02); // generic action
            buf.put((byte) 0x01); // number attributes
            buf.put((byte) 0x01); // attribute id (title)
            buf.putShort((short) open_string.getBytes().length);
            buf.put(open_string.getBytes());

            buf.put((byte) 0x04);
            buf.put((byte) 0x02); // generic action
            buf.put((byte) 0x01); // number attributes
            buf.put((byte) 0x01); // attribute id (title)
            buf.putShort((short) mute_string.getBytes().length);
            buf.put(mute_string.getBytes());
        }

        if (cannedReplies != null && replies_length > 0) {
            buf.put((byte) 0x05);
            buf.put((byte) 0x03); // reply action
            buf.put((byte) 0x02); // number attributes
            buf.put((byte) 0x01); // title
            buf.putShort((short) reply_string.getBytes().length);
            buf.put(reply_string.getBytes());
            buf.put((byte) 0x08); // canned replies
            buf.putShort((short) replies_length);
            for (int i = 0; i < cannedReplies.length - 1; i++) {
                buf.put(cannedReplies[i].getBytes());
                buf.put((byte) 0x00);
            }
            // last one must not be zero terminated, else we get an additional emply reply
            buf.put(cannedReplies[cannedReplies.length - 1].getBytes());
        }

        return encodeBlobdb(UUID.randomUUID(), BLOBDB_INSERT, BLOBDB_NOTIFICATION, buf.array());
    }

    public byte[] encodeActionResponse2x(int id, byte actionId, int iconId, String caption) {
        short length = (short) (18 + caption.getBytes().length);
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(length);
        buf.putShort(ENDPOINT_EXTENSIBLENOTIFS);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATIONACTION_RESPONSE);
        buf.putInt(id);
        buf.put(actionId);
        buf.put(NOTIFICATIONACTION_ACK);
        buf.put((byte) 2); //nr of attributes
        buf.put((byte) 6); // icon
        buf.putShort((short) 4); // length
        buf.putInt(iconId);
        buf.put((byte) 2); // title
        buf.putShort((short) caption.getBytes().length);
        buf.put(caption.getBytes());
        return buf.array();
    }

    public byte[] encodeActionResponse(UUID uuid, int iconId, String caption) {
        short length = (short) (29 + caption.getBytes().length);
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(length);
        buf.putShort(ENDPOINT_NOTIFICATIONACTION);
        buf.put(NOTIFICATIONACTION_RESPONSE);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATIONACTION_ACK);
        buf.put((byte) 2); //nr of attributes
        buf.put((byte) 6); // icon
        buf.putShort((short) 4); // length
        buf.putInt(0x80000000 | iconId);
        buf.put((byte) 2); // title
        buf.putShort((short) caption.getBytes().length);
        buf.put(caption.getBytes());
        return buf.array();
    }

    public byte[] encodeInstallMetadata(UUID uuid, String appName, short appVersion, short sdkVersion, int flags, int iconId) {
        final short METADATA_LENGTH = 126;

        byte[] name_buf = new byte[96];
        System.arraycopy(appName.getBytes(), 0, name_buf, 0, appName.getBytes().length);
        ByteBuffer buf = ByteBuffer.allocate(METADATA_LENGTH);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits()); // watchapp uuid
        buf.putLong(uuid.getLeastSignificantBits());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(flags);
        buf.putInt(iconId);
        buf.putShort(appVersion);
        buf.putShort(sdkVersion);
        buf.put((byte) 0); // app_face_bgcolor
        buf.put((byte) 0); // app_face_template_id
        buf.put(name_buf); // 96 bytes

        return encodeBlobdb(uuid, BLOBDB_INSERT, BLOBDB_APP, buf.array());
    }

    public byte[] encodeAppFetchAck() {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_APPFETCH);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_APPFETCH);
        buf.putShort(ENDPOINT_APPFETCH);
        buf.put((byte) 0x01);
        buf.put((byte) 0x01);

        return buf.array();
    }

    public byte[] encodeGetTime() {
        return encodeSimpleMessage(ENDPOINT_TIME, TIME_GETTIME);
    }

    @Override
    public byte[] encodeSetCallState(String number, String name, ServiceCommand command) {
        String[] parts = {number, name};
        byte pebbleCmd;
        switch (command) {
            case CALL_START:
                pebbleCmd = PHONECONTROL_START;
                break;
            case CALL_END:
                pebbleCmd = PHONECONTROL_END;
                break;
            case CALL_INCOMING:
                pebbleCmd = PHONECONTROL_INCOMINGCALL;
                break;
            case CALL_OUTGOING:
                // pebbleCmd = PHONECONTROL_OUTGOINGCALL;
                /*
                 *  HACK/WORKAROUND for non-working outgoing call display.
                 *  Just send a incoming call command immediately followed by a start call command
                 *  This prevents vibration of the Pebble.
                 */
                byte[] callmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_INCOMINGCALL, 0, parts);
                byte[] startmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_START, 0, parts);
                byte[] msg = new byte[callmsg.length + startmsg.length];
                System.arraycopy(callmsg, 0, msg, 0, callmsg.length);
                System.arraycopy(startmsg, 0, msg, startmsg.length, startmsg.length);
                return msg;
            // END HACK
            default:
                return null;
        }
        return encodeMessage(ENDPOINT_PHONECONTROL, pebbleCmd, 0, parts);
    }

    @Override
    public byte[] encodeSetMusicInfo(String artist, String album, String track) {
        String[] parts = {artist, album, track};
        return encodeMessage(ENDPOINT_MUSICCONTROL, MUSICCONTROL_SETMUSICINFO, 0, parts);
    }

    @Override
    public byte[] encodeFirmwareVersionReq() {
        return encodeSimpleMessage(ENDPOINT_FIRMWAREVERSION, FIRMWAREVERSION_GETVERSION);
    }

    @Override
    public byte[] encodeAppInfoReq() {
        if (isFw3x) {
            return null; // can't do this on 3.x :(
        }
        return encodeSimpleMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETUUIDS);
    }

    @Override
    public byte[] encodeAppStart(UUID uuid, boolean start) {
        if (isFw3x) {
            ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_APPRUNSTATE);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(LENGTH_APPRUNSTATE);
            buf.putShort(ENDPOINT_APPRUNSTATE);
            buf.put(start ? APPRUNSTATE_START : APPRUNSTATE_STOP);
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
            return buf.array();
        } else {
            ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
            int param = start ? 1 : 0;
            pairs.add(new Pair<>(1, (Object) param));
            return encodeApplicationMessagePush(ENDPOINT_LAUNCHER, uuid, pairs);
        }
    }

    @Override
    public byte[] encodeAppDelete(UUID uuid) {
        if (isFw3x) {
            return encodeBlobdb(uuid, BLOBDB_DELETE, BLOBDB_APP, null);
        } else {
            ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REMOVEAPP_2X);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(LENGTH_REMOVEAPP_2X);
            buf.putShort(ENDPOINT_APPMANAGER);
            buf.put(APPMANAGER_REMOVEAPP);
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
            return buf.array();
        }
    }

    private byte[] encodePhoneVersion2x(byte os) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PHONEVERSION);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PHONEVERSION);
        buf.putShort(ENDPOINT_PHONEVERSION);
        buf.put((byte) 0x01);
        buf.putInt(-1); //0xffffffff

        if (os == PHONEVERSION_REMOTE_OS_ANDROID) {
            buf.putInt(PHONEVERSION_SESSION_CAPS_GAMMARAY);
        } else {
            buf.putInt(0);
        }
        buf.putInt(PHONEVERSION_REMOTE_CAPS_SMS | PHONEVERSION_REMOTE_CAPS_TELEPHONY | os);

        buf.put(PHONEVERSION_APPVERSION_MAGIC);
        buf.put(PHONEVERSION_APPVERSION_MAJOR);
        buf.put(PHONEVERSION_APPVERSION_MINOR);
        buf.put(PHONEVERSION_APPVERSION_PATCH);

        return buf.array();
    }

    private byte[] encodePhoneVersion3x(byte os) {
        final short LENGTH_PHONEVERSION3X = 25;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PHONEVERSION3X);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PHONEVERSION3X);
        buf.putShort(ENDPOINT_PHONEVERSION);
        buf.put((byte) 0x01);
        buf.putInt(-1); //0xffffffff
        buf.putInt(0);

        buf.putInt(os);

        buf.put(PHONEVERSION_APPVERSION_MAGIC);
        buf.put((byte) 3); // major
        buf.put((byte) 8); // minor
        buf.put((byte) 1); // patch
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(0x0000000000000003); //flags

        return buf.array();
    }

    public byte[] encodePhoneVersion(byte os) {
        return encodePhoneVersion3x(os);
    }

    @Override
    public byte[] encodeReboot() {
        return encodeSimpleMessage(ENDPOINT_RESET, RESET_REBOOT);
    }

    @Override
    public byte[] encodeScreenshotReq() {
        return encodeSimpleMessage(ENDPOINT_SCREENSHOT, SCREENSHOT_TAKE);
    }

    /* pebble specific install methods */
    public byte[] encodeUploadStart(byte type, int app_id, int size, String filename) {
        short length;
        if (isFw3x && (type != PUTBYTES_TYPE_FILE)) {
            length = LENGTH_UPLOADSTART_3X;
            type |= 0b10000000;
        } else {
            length = LENGTH_UPLOADSTART_2X;
        }

        if (type == PUTBYTES_TYPE_FILE && filename != null) {
            length += filename.getBytes().length + 1;
        }

        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(length);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_INIT);
        buf.putInt(size);
        buf.put(type);

        if (isFw3x && (type != PUTBYTES_TYPE_FILE)) {
            buf.putInt(app_id);
        } else {
            // slot
            buf.put((byte) app_id);
        }

        if (type == PUTBYTES_TYPE_FILE && filename != null) {
            buf.put(filename.getBytes());
            buf.put((byte) 0);
        }

        return buf.array();
    }

    public byte[] encodeUploadChunk(int token, byte[] buffer, int size) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCHUNK + size);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (LENGTH_UPLOADCHUNK + size));
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_SEND);
        buf.putInt(token);
        buf.putInt(size);
        buf.put(buffer, 0, size);
        return buf.array();
    }

    public byte[] encodeUploadCommit(int token, int crc) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMMIT);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMMIT);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMMIT);
        buf.putInt(token);
        buf.putInt(crc);
        return buf.array();
    }

    public byte[] encodeUploadComplete(int token) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMPLETE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMPLETE);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMPLETE);
        buf.putInt(token);
        return buf.array();
    }

    public byte[] encodeUploadCancel(int token) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCANCEL);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCANCEL);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_ABORT);
        buf.putInt(token);
        return buf.array();
    }

    private byte[] encodeSystemMessage(byte systemMessage) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SYSTEMMESSAGE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SYSTEMMESSAGE);
        buf.putShort(ENDPOINT_SYSTEMMESSAGE);
        buf.put((byte) 0);
        buf.put(systemMessage);
        return buf.array();

    }

    public byte[] encodeInstallFirmwareStart() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARESTART);
    }

    public byte[] encodeInstallFirmwareComplete() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARECOMPLETE);
    }

    public byte[] encodeInstallFirmwareError() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWAREFAIL);
    }


    public byte[] encodeAppRefresh(int index) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REFRESHAPP);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_REFRESHAPP);
        buf.putShort(ENDPOINT_APPMANAGER);
        buf.put(APPMANAGER_REFRESHAPP);
        buf.putInt(index);

        return buf.array();
    }

    public byte[] encodeDatalog(byte handle, byte reply) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 2);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 2);
        buf.putShort(ENDPOINT_DATALOG);
        buf.put(reply);
        buf.put(handle);

        return buf.array();
    }

    byte[] encodeApplicationMessageAck(UUID uuid, byte id) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 18); // +ACK

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 18);
        buf.putShort(ENDPOINT_APPLICATIONMESSAGE);
        buf.put(APPLICATIONMESSAGE_ACK);
        buf.put(id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getMostSignificantBits());

        return buf.array();
    }

    private static byte[] encodePing(byte command, int cookie) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PING);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PING);
        buf.putShort(ENDPOINT_PING);
        buf.put(command);
        buf.putInt(cookie);

        return buf.array();
    }

    private ArrayList<Pair<Integer, Object>> decodeDict(ByteBuffer buf) {
        ArrayList<Pair<Integer, Object>> dict = new ArrayList<>();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte dictSize = buf.get();
        while (dictSize-- > 0) {
            Integer key = buf.getInt();
            byte type = buf.get();
            short length = buf.getShort();
            switch (type) {
                case TYPE_INT:
                case TYPE_UINT:
                    dict.add(new Pair<Integer, Object>(key, buf.getInt()));
                    break;
                case TYPE_CSTRING:
                case TYPE_BYTEARRAY:
                    byte[] bytes = new byte[length];
                    buf.get(bytes);
                    if (type == TYPE_BYTEARRAY) {
                        dict.add(new Pair<Integer, Object>(key, bytes));
                    } else {
                        dict.add(new Pair<Integer, Object>(key, new String(bytes)));
                    }
                    break;
                default:
            }
        }
        return dict;
    }

    private GBDeviceEvent[] decodeDictToJSONAppMessage(UUID uuid, ByteBuffer buf) throws JSONException {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte dictSize = buf.get();
        if (dictSize == 0) {
            LOG.info("dict size is 0, ignoring");
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        while (dictSize-- > 0) {
            JSONObject jsonObject = new JSONObject();
            Integer key = buf.getInt();
            byte type = buf.get();
            short length = buf.getShort();
            jsonObject.put("key", key);
            jsonObject.put("length", length);
            switch (type) {
                case TYPE_UINT:
                    jsonObject.put("type", "uint");
                    if (length == 1) {
                        jsonObject.put("value", buf.get() & 0xff);
                    } else if (length == 2) {
                        jsonObject.put("value", buf.getShort() & 0xffff);
                    } else {
                        jsonObject.put("value", buf.getInt() & 0xffffffffL);
                    }
                    break;
                case TYPE_INT:
                    jsonObject.put("type", "int");
                    if (length == 1) {
                        jsonObject.put("value", buf.get());
                    } else if (length == 2) {
                        jsonObject.put("value", buf.getShort());
                    } else {
                        jsonObject.put("value", buf.getInt());
                    }
                    break;
                case TYPE_BYTEARRAY:
                case TYPE_CSTRING:
                    byte[] bytes = new byte[length];
                    buf.get(bytes);
                    if (type == TYPE_BYTEARRAY) {
                        jsonObject.put("type", "bytes");
                        jsonObject.put("value", Base64.encode(bytes, Base64.NO_WRAP));
                    } else {
                        jsonObject.put("type", "string");
                        jsonObject.put("value", new String(bytes));
                    }
                    break;
                default:
                    LOG.info("unknown type in appmessage, ignoring");
                    return null;
            }
            jsonArray.put(jsonObject);
        }

        // this is a hack we send an ack to the Pebble immediately because we cannot map the transaction_id from the intent back to a uuid yet
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = encodeApplicationMessageAck(uuid, last_id);

        GBDeviceEventAppMessage appMessage = new GBDeviceEventAppMessage();
        appMessage.appUUID = uuid;
        appMessage.id = last_id & 0xff;
        appMessage.message = jsonArray.toString();
        return new GBDeviceEvent[]{appMessage, sendBytesAck};
    }

    byte[] encodeApplicationMessagePush(short endpoint, UUID uuid, ArrayList<Pair<Integer, Object>> pairs) {
        int length = LENGTH_UUID + 3; // UUID + (PUSH + id + length of dict)
        for (Pair<Integer, Object> pair : pairs) {
            length += 7; // key + type + length
            if (pair.second instanceof Integer) {
                length += 4;
            } else if (pair.second instanceof Short) {
                length += 2;
            } else if (pair.second instanceof Byte) {
                length += 1;
            } else if (pair.second instanceof String) {
                length += ((String) pair.second).getBytes().length + 1;
            } else if (pair.second instanceof byte[]) {
                length += ((byte[]) pair.second).length;
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) length);
        buf.putShort(endpoint); // 48 or 49
        buf.put(APPLICATIONMESSAGE_PUSH);
        buf.put(++last_id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.put((byte) pairs.size());

        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (Pair<Integer, Object> pair : pairs) {
            buf.putInt(pair.first);
            if (pair.second instanceof Integer) {
                buf.put(TYPE_INT);
                buf.putShort((short) 4); // length
                buf.putInt((int) pair.second);
            } else if (pair.second instanceof Short) {
                buf.put(TYPE_INT);
                buf.putShort((short) 2); // length
                buf.putShort((short) pair.second);
            } else if (pair.second instanceof Byte) {
                buf.put(TYPE_INT);
                buf.putShort((short) 1); // length
                buf.put((byte) pair.second);
            } else if (pair.second instanceof String) {
                String str = (String) pair.second;
                buf.put(TYPE_CSTRING);
                buf.putShort((short) (str.getBytes().length + 1));
                buf.put(str.getBytes());
                buf.put((byte) 0);
            } else if (pair.second instanceof byte[]) {
                byte[] bytes = (byte[]) pair.second;
                buf.put(TYPE_BYTEARRAY);
                buf.putShort((short) bytes.length);
                buf.put(bytes);
            }
        }

        return buf.array();
    }

    public byte[] encodeApplicationMessageFromJSON(UUID uuid, JSONArray jsonArray) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                String type = (String) jsonObject.get("type");
                int key = (int) jsonObject.get("key");
                int length = (int) jsonObject.get("length");
                switch (type) {
                    case "uint":
                    case "int":
                        if (length == 1) {
                            pairs.add(new Pair<>(key, (Object) (byte) jsonObject.getInt("value")));
                        } else if (length == 2) {
                            pairs.add(new Pair<>(key, (Object) (short) jsonObject.getInt("value")));
                        } else {
                            if (type.equals("uint")) {
                                pairs.add(new Pair<>(key, (Object) (int) (jsonObject.getInt("value") & 0xffffffffL)));
                            } else {
                                pairs.add(new Pair<>(key, (Object) jsonObject.getInt("value")));
                            }
                        }
                        break;
                    case "string":
                        pairs.add(new Pair<>(key, (Object) jsonObject.getString("value")));
                        break;
                    case "bytes":
                        byte[] bytes = Base64.decode(jsonObject.getString("value"), Base64.NO_WRAP);
                        pairs.add(new Pair<>(key, (Object) bytes));
                        break;
                }
            } catch (JSONException e) {
                return null;
            }
        }

        return encodeApplicationMessagePush(ENDPOINT_APPLICATIONMESSAGE, uuid, pairs);
    }

    private static byte reverseBits(byte in) {
        byte out = 0;
        for (int i = 0; i < 8; i++) {
            byte bit = (byte) (in & 1);
            out = (byte) ((out << 1) | bit);
            in = (byte) (in >> 1);
        }
        return out;
    }

    private GBDeviceEventScreenshot decodeScreenshot(ByteBuffer buf, int length) {
        if (mDevEventScreenshot == null) {
            byte result = buf.get();
            mDevEventScreenshot = new GBDeviceEventScreenshot();
            int version = buf.getInt();
            if (result != 0) {
                return null;
            }
            mDevEventScreenshot.width = buf.getInt();
            mDevEventScreenshot.height = buf.getInt();

            if (version == 1) {
                mDevEventScreenshot.bpp = 1;
                mDevEventScreenshot.clut = clut_pebble;
            } else {
                mDevEventScreenshot.bpp = 8;
                mDevEventScreenshot.clut = clut_pebbletime;
            }

            mScreenshotRemaining = (mDevEventScreenshot.width * mDevEventScreenshot.height * mDevEventScreenshot.bpp) / 8;

            mDevEventScreenshot.data = new byte[mScreenshotRemaining];
            length -= 13;
        }
        if (mScreenshotRemaining == -1) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            byte corrected = buf.get();
            if (mDevEventScreenshot.bpp == 1) {
                corrected = reverseBits(corrected);
            } else {
                corrected = (byte) (corrected & 0b00111111);
            }

            mDevEventScreenshot.data[mDevEventScreenshot.data.length - mScreenshotRemaining + i] = corrected;
        }
        mScreenshotRemaining -= length;
        LOG.info("Screenshot remaining bytes " + mScreenshotRemaining);
        if (mScreenshotRemaining == 0) {
            mScreenshotRemaining = -1;
            LOG.info("Got screenshot : " + mDevEventScreenshot.width + "x" + mDevEventScreenshot.height + "  " + "pixels");
            GBDeviceEventScreenshot devEventScreenshot = mDevEventScreenshot;
            mDevEventScreenshot = null;
            return devEventScreenshot;
        }
        return null;
    }

    private GBDeviceEvent[] decodeAction(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte command = buf.get();
        if (command == NOTIFICATIONACTION_INVOKE) {
            int id;
            long uuid_high = 0;
            long uuid_low = 0;
            if (isFw3x) {
                buf.order(ByteOrder.BIG_ENDIAN);
                uuid_high = buf.getLong();
                uuid_low = buf.getLong();
                buf.order(ByteOrder.LITTLE_ENDIAN);
                id = (int) (uuid_low & 0xffffffffL);
            } else {
                id = buf.getInt();
            }
            byte action = buf.get();
            if (action >= 0x01 && action <= 0x05) {
                GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
                devEvtNotificationControl.handle = id;
                String caption = "undefined";
                int icon_id = 1;
                boolean needsAck2x = true;
                switch (action) {
                    case 0x01:
                        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.OPEN;
                        caption = "Opened";
                        icon_id = PebbleIconID.DURING_PHONE_CALL;
                        break;
                    case 0x02:
                        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS;
                        caption = "Dismissed";
                        icon_id = PebbleIconID.RESULT_DISMISSED;
                        needsAck2x = false;
                        break;
                    case 0x03:
                        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS_ALL;
                        caption = "All dismissed";
                        icon_id = PebbleIconID.RESULT_DISMISSED;
                        needsAck2x = false;
                        break;
                    case 0x04:
                        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.MUTE;
                        caption = "Muted";
                        icon_id = PebbleIconID.RESULT_MUTE;
                        break;
                    case 0x05:
                        byte attribute_count = buf.get();
                        if (attribute_count > 0) {
                            byte attribute = buf.get();
                            if (attribute == 0x01) { // reply string is in attribute 0x01
                                short length = buf.getShort();
                                if (length > 64) length = 64;
                                byte[] reply = new byte[length];
                                buf.get(reply);
                                devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                                devEvtNotificationControl.reply = new String(reply);
                                caption = "SENT";
                                icon_id = PebbleIconID.RESULT_SENT;
                            } else {
                                devEvtNotificationControl = null; // error
                                caption = "FAILED";
                                icon_id = PebbleIconID.RESULT_FAILED;
                            }
                        } else {
                            caption = "FAILED";
                            icon_id = PebbleIconID.RESULT_FAILED;
                            devEvtNotificationControl = null; // error
                        }
                        break;
                }
                GBDeviceEventSendBytes sendBytesAck = null;
                if (isFw3x || needsAck2x) {
                    sendBytesAck = new GBDeviceEventSendBytes();
                    if (isFw3x) {
                        sendBytesAck.encodedBytes = encodeActionResponse(new UUID(uuid_high, uuid_low), icon_id, caption);
                    } else {
                        sendBytesAck.encodedBytes = encodeActionResponse2x(id, action, 6, caption);
                    }
                }
                return new GBDeviceEvent[]{sendBytesAck, devEvtNotificationControl};
            }
            LOG.info("unexpected action: " + action);
        }

        return null;
    }

    private GBDeviceEventSendBytes decodePing(ByteBuffer buf) {
        byte command = buf.get();
        if (command == PING_PING) {
            int cookie = buf.getInt();
            LOG.info("Received PING - will reply");
            GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
            sendBytes.encodedBytes = encodePing(PING_PONG, cookie);
            return sendBytes;
        }
        return null;
    }

    private GBDeviceEvent decodeSystemMessage(ByteBuffer buf) {
        buf.get(); // unknown;
        byte command = buf.get();
        final String ENDPOINT_NAME = "SYSTEM MESSAGE";
        switch (command) {
            case SYSTEMMESSAGE_STOPRECONNECTING:
                LOG.info(ENDPOINT_NAME + ": stop reconnecting");
                break;
            case SYSTEMMESSAGE_STARTRECONNECTING:
                LOG.info(ENDPOINT_NAME + ": start reconnecting");
                break;
            default:
                LOG.info(ENDPOINT_NAME + ": " + command);
                break;
        }
        return null;
    }

    private GBDeviceEvent decodeAppRunState(ByteBuffer buf) {
        byte command = buf.get();
        long uuid_high = buf.getLong();
        long uuid_low = buf.getLong();
        UUID uuid = new UUID(uuid_high, uuid_low);
        final String ENDPOINT_NAME = "APPRUNSTATE";
        switch (command) {
            case APPRUNSTATE_START:
                LOG.info(ENDPOINT_NAME + ": started " + uuid);
                break;
            case APPRUNSTATE_STOP:
                LOG.info(ENDPOINT_NAME + ": stopped " + uuid);
                break;
            default:
                LOG.info(ENDPOINT_NAME + ": (cmd:" + command + ")" + uuid);
                break;
        }
        return null;
    }

    private GBDeviceEvent decodeBlobDb(ByteBuffer buf) {
        final String ENDPOINT_NAME = "BLOBDB";
        final String statusString[] = {
                "unknown",
                "success",
                "general failure",
                "invalid operation",
                "invalid database id",
                "invalid data",
                "key does not exist",
                "database full",
                "data stale",
        };
        buf.order(ByteOrder.LITTLE_ENDIAN);
        short token = buf.getShort();
        byte status = buf.get();

        if (status >= 0 && status < statusString.length) {
            LOG.info(ENDPOINT_NAME + ": " + statusString[status] + " (token " + (token & 0xffff) + ")");
        } else {
            LOG.warn(ENDPOINT_NAME + ": unknown status " + status + " (token " + (token & 0xffff) + ")");
        }
        return null;
    }

    private GBDeviceEventAppManagement decodeAppFetch(ByteBuffer buf) {
        byte command = buf.get();
        if (command == 0x01) {
            long uuid_high = buf.getLong();
            long uuid_low = buf.getLong();
            UUID uuid = new UUID(uuid_high, uuid_low);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int app_id = buf.getInt();
            GBDeviceEventAppManagement fetchRequest = new GBDeviceEventAppManagement();
            fetchRequest.type = GBDeviceEventAppManagement.EventType.INSTALL;
            fetchRequest.event = GBDeviceEventAppManagement.Event.REQUEST;
            fetchRequest.token = app_id;
            fetchRequest.uuid = uuid;
            return fetchRequest;
        }
        return null;
    }

    private GBDeviceEventSendBytes decodeDatalog(ByteBuffer buf, short length) {
        byte command = buf.get();
        byte id = buf.get();
        switch (command) {
            case DATALOG_TIMEOUT:
                LOG.info("DATALOG TIMEOUT. id=" + (id & 0xff) + " - ignoring");
                return null;
            case DATALOG_SENDDATA:
                buf.order(ByteOrder.LITTLE_ENDIAN);
                int items_left = buf.getInt();
                int crc = buf.getInt();
                LOG.info("DATALOG SENDDATA. id=" + (id & 0xff) + ", items_left=" + items_left + ", total length=" + (length - 9));
                break;
            case DATALOG_OPENSESSION:
                buf.order(ByteOrder.BIG_ENDIAN);
                long uuid_high = buf.getLong();
                long uuid_low = buf.getLong();
                UUID uuid = new UUID(uuid_high, uuid_low);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                int timestamp = buf.getInt();
                int log_tag = buf.getInt();
                byte item_type = buf.get();
                short item_size = buf.get();
                LOG.info("DATALOG OPENSESSION. id=" + (id & 0xff) + ", App UUID=" + uuid.toString() + ", item_type=" + item_type + ", item_size=" + item_size);
                break;
            default:
                LOG.info("unknown DATALOG command: " + (command & 0xff));
                break;
        }
        LOG.info("sending ACK (0x85)");
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        sendBytes.encodedBytes = encodeDatalog(id, DATALOG_ACK);
        return sendBytes;
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        buf.order(ByteOrder.BIG_ENDIAN);
        short length = buf.getShort();
        short endpoint = buf.getShort();
        GBDeviceEvent devEvts[] = null;
        byte pebbleCmd = -1;
        switch (endpoint) {
            case ENDPOINT_MUSICCONTROL:
                pebbleCmd = buf.get();
                GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();
                switch (pebbleCmd) {
                    case MUSICCONTROL_NEXT:
                        musicCmd.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case MUSICCONTROL_PREVIOUS:
                        musicCmd.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case MUSICCONTROL_PLAY:
                        musicCmd.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case MUSICCONTROL_PAUSE:
                        musicCmd.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case MUSICCONTROL_PLAYPAUSE:
                        musicCmd.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                        break;
                    case MUSICCONTROL_VOLUMEUP:
                        musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case MUSICCONTROL_VOLUMEDOWN:
                        musicCmd.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    default:
                        break;
                }
                devEvts = new GBDeviceEvent[]{musicCmd};
                break;
            case ENDPOINT_PHONECONTROL:
                pebbleCmd = buf.get();
                GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();
                switch (pebbleCmd) {
                    case PHONECONTROL_HANGUP:
                        callCmd.event = GBDeviceEventCallControl.Event.END;
                        break;
                    default:
                        LOG.info("Unknown PHONECONTROL event" + pebbleCmd);
                        break;
                }
                devEvts = new GBDeviceEvent[]{callCmd};
                break;
            case ENDPOINT_FIRMWAREVERSION:
                pebbleCmd = buf.get();
                GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

                buf.getInt(); // skip
                byte[] tmp = new byte[32];
                buf.get(tmp, 0, 32);

                versionCmd.fwVersion = new String(tmp).trim();
                if (versionCmd.fwVersion.startsWith("v3")) {
                    isFw3x = true;
                }

                buf.get(tmp, 0, 9);
                int hwRev = buf.get() + 5;
                if (hwRev >= 0 && hwRev < hwRevisions.length) {
                    versionCmd.hwVersion = hwRevisions[hwRev];
                }
                devEvts = new GBDeviceEvent[]{versionCmd};
                break;
            case ENDPOINT_APPMANAGER:
                pebbleCmd = buf.get();
                switch (pebbleCmd) {
                    case APPMANAGER_GETAPPBANKSTATUS:
                        GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
                        int slotCount = buf.getInt();
                        int slotsUsed = buf.getInt();
                        byte[] appName = new byte[32];
                        byte[] appCreator = new byte[32];
                        appInfoCmd.apps = new GBDeviceApp[slotsUsed];
                        boolean[] slotInUse = new boolean[slotCount];

                        for (int i = 0; i < slotsUsed; i++) {
                            int id = buf.getInt();
                            int index = buf.getInt();
                            slotInUse[index] = true;
                            buf.get(appName, 0, 32);
                            buf.get(appCreator, 0, 32);
                            int flags = buf.getInt();

                            GBDeviceApp.Type appType;
                            if ((flags & 16) == 16) {  // FIXME: verify this assumption
                                appType = GBDeviceApp.Type.APP_ACTIVITYTRACKER;
                            } else if ((flags & 1) == 1) {  // FIXME: verify this assumption
                                appType = GBDeviceApp.Type.WATCHFACE;
                            } else {
                                appType = GBDeviceApp.Type.APP_GENERIC;
                            }
                            Short appVersion = buf.getShort();
                            appInfoCmd.apps[i] = new GBDeviceApp(tmpUUIDS.get(i), new String(appName).trim(), new String(appCreator).trim(), appVersion.toString(), appType);
                        }
                        for (int i = 0; i < slotCount; i++) {
                            if (!slotInUse[i]) {
                                appInfoCmd.freeSlot = (byte) i;
                                LOG.info("found free slot " + i);
                                break;
                            }
                        }
                        devEvts = new GBDeviceEvent[]{appInfoCmd};
                        break;
                    case APPMANAGER_GETUUIDS:
                        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
                        sendBytes.encodedBytes = encodeSimpleMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETAPPBANKSTATUS);
                        devEvts = new GBDeviceEvent[]{sendBytes};
                        tmpUUIDS.clear();
                        slotsUsed = buf.getInt();
                        for (int i = 0; i < slotsUsed; i++) {
                            long uuid_high = buf.getLong();
                            long uuid_low = buf.getLong();
                            UUID uuid = new UUID(uuid_high, uuid_low);
                            LOG.info("found uuid: " + uuid);
                            tmpUUIDS.add(uuid);
                        }
                        break;
                    case APPMANAGER_REMOVEAPP:
                        GBDeviceEventAppManagement deleteRes = new GBDeviceEventAppManagement();
                        deleteRes.type = GBDeviceEventAppManagement.EventType.DELETE;

                        int result = buf.getInt();
                        switch (result) {
                            case APPMANAGER_RES_SUCCESS:
                                deleteRes.event = GBDeviceEventAppManagement.Event.SUCCESS;
                                break;
                            default:
                                deleteRes.event = GBDeviceEventAppManagement.Event.FAILURE;
                                break;
                        }
                        devEvts = new GBDeviceEvent[]{deleteRes};
                        break;
                    default:
                        LOG.info("Unknown APPMANAGER event" + pebbleCmd);
                        break;
                }
                break;
            case ENDPOINT_PUTBYTES:
                pebbleCmd = buf.get();
                GBDeviceEventAppManagement installRes = new GBDeviceEventAppManagement();
                installRes.type = GBDeviceEventAppManagement.EventType.INSTALL;
                switch (pebbleCmd) {
                    case PUTBYTES_INIT:
                        installRes.token = buf.getInt();
                        installRes.event = GBDeviceEventAppManagement.Event.SUCCESS;
                        break;
                    default:
                        installRes.token = buf.getInt();
                        installRes.event = GBDeviceEventAppManagement.Event.FAILURE;
                        break;
                }
                devEvts = new GBDeviceEvent[]{installRes};
                break;
            case ENDPOINT_APPLICATIONMESSAGE:
            case ENDPOINT_LAUNCHER:
                pebbleCmd = buf.get();
                last_id = buf.get();
                long uuid_high = buf.getLong();
                long uuid_low = buf.getLong();

                switch (pebbleCmd) {
                    case APPLICATIONMESSAGE_PUSH:
                        UUID uuid = new UUID(uuid_high, uuid_low);

                        if (endpoint == ENDPOINT_LAUNCHER) {
                            LOG.info("got LAUNCHER PUSH from UUID " + uuid);
                            break;
                        }
                        LOG.info("got APPLICATIONMESSAGE PUSH from UUID " + uuid);

                        AppMessageHandler handler = mAppMessageHandlers.get(uuid);
                        if (handler != null) {
                            ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                            devEvts = handler.handleMessage(dict);
                        } else {
                            try {
                                devEvts = decodeDictToJSONAppMessage(uuid, buf);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                        break;
                    case APPLICATIONMESSAGE_ACK:
                        LOG.info("got APPLICATIONMESSAGE/LAUNCHER (EP " + endpoint + ")  ACK");
                        devEvts = new GBDeviceEvent[]{null};
                        break;
                    case APPLICATIONMESSAGE_NACK:
                        LOG.info("got APPLICATIONMESSAGE/LAUNCHER (EP " + endpoint + ")  NACK");
                        devEvts = new GBDeviceEvent[]{null};
                        break;
                    case APPLICATIONMESSAGE_REQUEST:
                        LOG.info("got APPLICATIONMESSAGE/LAUNCHER (EP " + endpoint + ")  REQUEST");
                        devEvts = new GBDeviceEvent[]{null};
                        break;
                    default:
                        break;
                }
                break;
            case ENDPOINT_PHONEVERSION:
                pebbleCmd = buf.get();
                switch (pebbleCmd) {
                    case PHONEVERSION_REQUEST:
                        LOG.info("Pebble asked for Phone/App Version - repLYING!");
                        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
                        sendBytes.encodedBytes = encodePhoneVersion(PHONEVERSION_REMOTE_OS_ANDROID);
                        devEvts = new GBDeviceEvent[]{sendBytes};
                        break;
                    default:
                        break;
                }
                break;
            case ENDPOINT_DATALOG:
                devEvts = new GBDeviceEvent[]{decodeDatalog(buf, length)};
                break;
            case ENDPOINT_SCREENSHOT:
                devEvts = new GBDeviceEvent[]{decodeScreenshot(buf, length)};
                break;
            case ENDPOINT_EXTENSIBLENOTIFS:
            case ENDPOINT_NOTIFICATIONACTION:
                devEvts = decodeAction(buf);
                break;
            case ENDPOINT_PING:
                devEvts = new GBDeviceEvent[]{decodePing(buf)};
                break;
            case ENDPOINT_APPFETCH:
                devEvts = new GBDeviceEvent[]{decodeAppFetch(buf)};
                break;
            case ENDPOINT_SYSTEMMESSAGE:
                devEvts = new GBDeviceEvent[]{decodeSystemMessage(buf)};
                break;
            case ENDPOINT_APPRUNSTATE:
                devEvts = new GBDeviceEvent[]{decodeAppRunState(buf)};
                break;
            case ENDPOINT_BLOBDB:
                devEvts = new GBDeviceEvent[]{decodeBlobDb(buf)};
                break;
            default:
                break;
        }

        return devEvts;
    }

    public void setForceProtocol(boolean force) {
        LOG.info("setting force protocol to " + force);
        mForceProtocol = force;
    }
}
