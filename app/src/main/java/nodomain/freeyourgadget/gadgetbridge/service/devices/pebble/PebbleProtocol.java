package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppManagement;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDismissNotification;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
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

    static final byte BLOBDB_INSERT = 1;
    static final byte BLOBDB_DELETE = 4;
    static final byte BLOBDB_CLEAR = 5;

    static final byte BLOBDB_PIN = 1;
    static final byte BLOBDB_APP = 2;
    static final byte BLOBDB_REMINDER = 3;
    static final byte BLOBDB_NOTIFICATION = 4;

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
    static final byte PUTBYTES_TYPE_FILE = 6;
    public static final byte PUTBYTES_TYPE_WORKER = 7;

    static final byte RESET_REBOOT = 0;

    static final byte SCREENSHOT_TAKE = 0;

    static final byte SYSTEMMESSAGE_FIRMWARESTART = 1;
    static final byte SYSTEMMESSAGE_FIRMWARECOMPLETE = 2;
    static final byte SYSTEMMESSAGE_FIRMWAREFAIL = 3;

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
    static final byte TYPE_UINT32 = 2;
    static final byte TYPE_INT32 = 3;

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

    private static final String[] hwRevisions = {"unknown", "ev1", "ev2", "ev2_3", "ev2_4", "v1_5", "v2_0", "evt2", "dvt"};
    private static Random mRandom = new Random();

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
    private ArrayList<UUID> tmpUUIDS = new ArrayList<>();

    private MorpheuzSupport mMorpheuzSupport = new MorpheuzSupport(PebbleProtocol.this);
    private WeatherNeatSupport mWeatherNeatSupport = new WeatherNeatSupport(PebbleProtocol.this);
    private GadgetbridgePblSupport mGadgetbridgePblSupport = new GadgetbridgePblSupport(PebbleProtocol.this);

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

    private byte[] encodeNotification(int id, String title, String subtitle, String body, byte type) {
        Long ts = System.currentTimeMillis();
        if (!isFw3x) {
            ts += (SimpleTimeZone.getDefault().getOffset(ts));
        }
        ts /= 1000;

        if (isFw3x) {
            // 3.x notification
            return encodeBlobdbNotification((int) (ts & 0xffffffff), title, subtitle, body, type);
        } else if (mForceProtocol || type != NOTIFICATION_EMAIL) {
            // 2.x notification
            return encodeExtensibleNotification(id, (int) (ts & 0xffffffff), title, subtitle, body, type);
        } else {
            // 1.x notification on FW 2.X
            String[] parts = {title, body, ts.toString(), subtitle};
            return encodeMessage(ENDPOINT_NOTIFICATION, type, 0, parts);
        }
    }

    @Override
    public byte[] encodeSMS(String from, String body) {
        return encodeNotification(mRandom.nextInt(), from, null, body, NOTIFICATION_SMS);
    }

    @Override
    public byte[] encodeEmail(String from, String subject, String body) {
        return encodeNotification(mRandom.nextInt(), from, subject, body, NOTIFICATION_EMAIL);
    }

    @Override
    public byte[] encodeGenericNotification(String title, String details) {
        return encodeNotification(mRandom.nextInt(), title, null, details, NOTIFICATION_SMS);
    }

    @Override
    public byte[] encodeSetTime() {
        long ts = System.currentTimeMillis();
        long ts_offset = (SimpleTimeZone.getDefault().getOffset(ts));
        ByteBuffer buf;
        if (isFw3x) {
            String timezone = SimpleTimeZone.getDefault().getDisplayName(false, SimpleTimeZone.SHORT);
            short length = (short) (LENGTH_SETTIME + timezone.length() + 3);
            buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(length);
            buf.putShort(ENDPOINT_TIME);
            buf.put(TIME_SETTIME_UTC);
            buf.putInt((int) (ts / 1000));
            buf.putShort((short) (ts_offset / 60000));
            buf.put((byte) timezone.length());
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

    private static byte[] encodeExtensibleNotification(int id, int timestamp, String title, String subtitle, String body, byte type) {
        String[] parts = {title, subtitle, body};

        // Calculate length first
        byte attributes_count = 0;

        int length = 21 + 17;
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
        buf.putInt(0x00000002); // flags - ?
        buf.putInt(id);
        buf.putInt(0x00000000); // ANCS id
        buf.putInt(timestamp);
        buf.put((byte) 0x01); // layout - ?
        buf.put(attributes_count); // length attributes
        buf.put((byte) 1); // len actions - only dismiss

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

        // ACTION
        buf.put((byte) 0x01); // id
        buf.put((byte) 0x04); // dismiss action
        buf.put((byte) 0x01); // number attributes
        buf.put((byte) 0x01); // attribute id (title)
        String actionstring = "dismiss all";
        buf.putShort((short) actionstring.length());
        buf.put(actionstring.getBytes());
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

    private byte[] encodeBlobdbNotification(int timestamp, String title, String subtitle, String body, byte type) {
        String[] parts = {title, subtitle, body};

        int icon_id = 1;
        switch (type) {
            case NOTIFICATION_EMAIL:
                icon_id = 19;
                break;
            case NOTIFICATION_SMS:
                icon_id = 45;
        }
        // Calculate length first
        final short NOTIFICATION_PIN_LENGTH = 46;
        final short ACTIONS_LENGTH = 17;

        byte attributes_count = 1; // icon

        short attributes_length = 7; // icon
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_count++;
                attributes_length += (3 + s.getBytes().length);
            }
        }

        byte actions_count = 0;

        if (mForceProtocol) {
            actions_count = 1;
            attributes_length += ACTIONS_LENGTH;
        }

        UUID uuid = UUID.randomUUID();
        short pin_length = (short) (NOTIFICATION_PIN_LENGTH + attributes_length);

        ByteBuffer buf = ByteBuffer.allocate(pin_length);

        // pin - 46 bytes
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
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
                if (partlength > 255) partlength = 255;
                buf.put(attribute_id);
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }

        buf.put((byte) 4); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(icon_id);

        if (mForceProtocol) {
            // ACTION
            buf.put((byte) 0x01); // id
            buf.put((byte) 0x04); // dismiss action
            buf.put((byte) 0x01); // number attributes
            buf.put((byte) 0x01); // attribute id (title)
            String actionstring = "dismiss all";
            buf.putShort((short) actionstring.length());
            buf.put(actionstring.getBytes());
        }

        return encodeBlobdb(UUID.randomUUID(), BLOBDB_INSERT, BLOBDB_NOTIFICATION, buf.array());
    }

    public byte[] encodeInstallMetadata(UUID uuid, String appName, short appVersion, short sdkVersion, int flags, int iconId) {
        final short METADATA_LENGTH = 126;

        byte[] name_buf = new byte[96];
        System.arraycopy(appName.getBytes(), 0, name_buf, 0, appName.length());
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
        return encodeSimpleMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETUUIDS);
    }

    @Override
    public byte[] encodeAppStart(UUID uuid) {
        if (isFw3x) {
            ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_APPRUNSTATE);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort(LENGTH_APPRUNSTATE);
            buf.putShort(ENDPOINT_APPRUNSTATE);
            buf.put(APPRUNSTATE_START);
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
            return buf.array();
        } else {
            ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
            pairs.add(new Pair<>(1, (Object) 1)); // launch
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
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 25);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 25);
        buf.putShort(ENDPOINT_PHONEVERSION);
        buf.put((byte) 0x01);
        buf.putInt(-1); //0xffffffff
        buf.putInt(0);

        buf.putInt(os);

        buf.put(PHONEVERSION_APPVERSION_MAGIC);
        buf.put((byte) 3); // major?
        buf.put((byte) 0); // minor?
        buf.put((byte) 1); // patch?
        buf.put((byte) 3); // ???
        buf.put((byte) 0); // ???
        buf.put((byte) 0); // ???
        buf.put((byte) 0); // ???
        buf.putInt(0); // ???

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
    public byte[] encodeUploadStart(byte type, int app_id, int size) {
        short length;
        if (isFw3x) {
            length = LENGTH_UPLOADSTART_3X;
            type |= 0b10000000;
        } else {
            length = LENGTH_UPLOADSTART_2X;
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(length);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_INIT);
        buf.putInt(size);
        buf.put(type);
        if (isFw3x) {
            buf.putInt(app_id);
        } else {
            // slot
            buf.put((byte) app_id);
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
        ArrayList<Pair<Integer, Object>> dict = new ArrayList<Pair<Integer, Object>>();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte dictSize = buf.get();
        while (dictSize-- > 0) {
            Integer key = buf.getInt();
            byte type = buf.get();
            short length = buf.getShort(); // length
            switch (type) {
                case TYPE_INT32:
                case TYPE_UINT32:
                    dict.add(new Pair<Integer, Object>(key, buf.getInt()));
                    break;
                case TYPE_CSTRING:
                case TYPE_BYTEARRAY:
                    byte[] bytes = new byte[length];
                    buf.get(bytes);
                    if (type == TYPE_BYTEARRAY) {
                        dict.add(new Pair<Integer, Object>(key, bytes));
                    } else {
                        dict.add(new Pair<Integer, Object>(key, Arrays.toString(bytes)));
                    }
                    break;
                default:
            }
        }
        return dict;
    }

    byte[] encodeApplicationMessagePush(short endpoint, UUID uuid, ArrayList<Pair<Integer, Object>> pairs) {
        int length = LENGTH_UUID + 3; // UUID + (PUSH + id + length of dict)
        for (Pair<Integer, Object> pair : pairs) {
            length += 7; // key + type + length
            if (pair.second instanceof Integer) {
                length += 4;
            } else if (pair.second instanceof String) {
                length += ((String) pair.second).length() + 1;
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

        buf.order(ByteOrder.LITTLE_ENDIAN); // Um, yes, really
        for (Pair<Integer, Object> pair : pairs) {
            buf.putInt(pair.first);
            if (pair.second instanceof Integer) {
                buf.put(TYPE_INT32);
                buf.putShort((short) 4); // length of int
                buf.putInt((int) pair.second);
            } else if (pair.second instanceof String) {
                buf.put(TYPE_CSTRING);
                buf.putShort((short) (((String) pair.second).length() + 1));
                buf.put(((String) pair.second).getBytes());
                buf.put((byte) 0);
            }
        }

        return buf.array();
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

    private GBDeviceEventDismissNotification decodeNotificationAction2x(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);

        byte command = buf.get();
        if (command == 0x02) { // dismiss notification ?
            int id = buf.getInt();
            short action = buf.getShort(); // at least the low byte should be the action - or not?
            if (action == 0x0001) {
                GBDeviceEventDismissNotification devEvtDismissNotification = new GBDeviceEventDismissNotification();
                devEvtDismissNotification.notificationID = id;
                return devEvtDismissNotification;
            }
            LOG.info("unexpected paramerter in dismiss action: " + action);
        }

        return null;
    }

    private GBDeviceEventDismissNotification decodeNotificationAction3x(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);

        byte command = buf.get();
        if (command == 0x02) { // dismiss notification ?
            buf.getLong(); // skip 8 bytes of UUID
            buf.getInt();  // skip 4 bytes of UUID
            int id = buf.getInt();
            short action = buf.getShort(); // at least the low byte should be the action - or not?
            if (action == 0x0001) {
                GBDeviceEventDismissNotification devEvtDismissNotification = new GBDeviceEventDismissNotification();
                devEvtDismissNotification.notificationID = id;
                return devEvtDismissNotification;
            }
            LOG.info("unexpected paramerter in dismiss action: " + action);
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
                Byte hwRev = buf.get();
                if (hwRev > 0 && hwRev < hwRevisions.length) {
                    versionCmd.hwVersion = hwRevisions[hwRev];
                } else if (hwRev == -3) { // basalt emulator
                    versionCmd.hwVersion = "dvt";
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
                pebbleCmd = buf.get();
                last_id = buf.get();
                long uuid_high = buf.getLong();
                long uuid_low = buf.getLong();

                switch (pebbleCmd) {
                    case APPLICATIONMESSAGE_PUSH:
                        UUID uuid = new UUID(uuid_high, uuid_low);
                        LOG.info("got APPLICATIONMESSAGE PUSH from UUID " + uuid);
                        if (WeatherNeatSupport.uuid.equals(uuid)) {
                            ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                            devEvts = mWeatherNeatSupport.handleMessage(dict);
                        } else if (MorpheuzSupport.uuid.equals(uuid)) {
                            ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                            devEvts = mMorpheuzSupport.handleMessage(dict);
                        } else if (GadgetbridgePblSupport.uuid.equals(uuid)) {
                            ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                            devEvts = mGadgetbridgePblSupport.handleMessage(dict);
                        }
                        break;
                    case APPLICATIONMESSAGE_ACK:
                        LOG.info("got APPLICATIONMESSAGE ACK");
                        break;
                    case APPLICATIONMESSAGE_NACK:
                        LOG.info("got APPLICATIONMESSAGE NACK");
                        break;
                    case APPLICATIONMESSAGE_REQUEST:
                        LOG.info("got APPLICATIONMESSAGE REQUEST");
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
                devEvts = new GBDeviceEvent[]{decodeNotificationAction2x(buf)};
                break;
            case ENDPOINT_NOTIFICATIONACTION:
                devEvts = new GBDeviceEvent[]{decodeNotificationAction3x(buf)};
                break;
            case ENDPOINT_PING:
                devEvts = new GBDeviceEvent[]{decodePing(buf)};
                break;
            case ENDPOINT_APPFETCH:
                devEvts = new GBDeviceEvent[]{decodeAppFetch(buf)};
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
