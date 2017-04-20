/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Julien Pivotto, Kevin Richter, Steffen Liebergeld, Uwe Hermann

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
import java.util.List;
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
import nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble.GBDeviceEventDataLogging;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleIconID;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PebbleProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleProtocol.class);

    private static final short ENDPOINT_TIME = 11;
    private static final short ENDPOINT_FIRMWAREVERSION = 16;
    private static final short ENDPOINT_PHONEVERSION = 17;
    private static final short ENDPOINT_SYSTEMMESSAGE = 18;
    private static final short ENDPOINT_MUSICCONTROL = 32;
    private static final short ENDPOINT_PHONECONTROL = 33;
    static final short ENDPOINT_APPLICATIONMESSAGE = 48;
    private static final short ENDPOINT_LAUNCHER = 49;
    private static final short ENDPOINT_APPRUNSTATE = 52; // FW >=3.x
    private static final short ENDPOINT_LOGS = 2000;
    private static final short ENDPOINT_PING = 2001;
    private static final short ENDPOINT_LOGDUMP = 2002;
    private static final short ENDPOINT_RESET = 2003;
    private static final short ENDPOINT_APP = 2004;
    private static final short ENDPOINT_APPLOGS = 2006;
    private static final short ENDPOINT_NOTIFICATION = 3000; // FW 1.x-2-x
    private static final short ENDPOINT_EXTENSIBLENOTIFS = 3010; // FW 2.x
    private static final short ENDPOINT_RESOURCE = 4000;
    private static final short ENDPOINT_SYSREG = 5000;
    private static final short ENDPOINT_FCTREG = 5001;
    private static final short ENDPOINT_APPMANAGER = 6000;
    private static final short ENDPOINT_APPFETCH = 6001; // FW >=3.x
    private static final short ENDPOINT_DATALOG = 6778;
    private static final short ENDPOINT_RUNKEEPER = 7000;
    private static final short ENDPOINT_SCREENSHOT = 8000;
    private static final short ENDPOINT_AUDIOSTREAM = 10000;
    private static final short ENDPOINT_VOICECONTROL = 11000;
    private static final short ENDPOINT_NOTIFICATIONACTION = 11440; // FW >=3.x, TODO: find a better name
    private static final short ENDPOINT_APPREORDER = (short) 0xabcd; // FW >=3.x
    private static final short ENDPOINT_BLOBDB = (short) 0xb1db;  // FW >=3.x
    private static final short ENDPOINT_PUTBYTES = (short) 0xbeef;

    private static final byte APPRUNSTATE_START = 1;
    private static final byte APPRUNSTATE_STOP = 2;

    private static final byte BLOBDB_INSERT = 1;
    private static final byte BLOBDB_DELETE = 4;
    private static final byte BLOBDB_CLEAR = 5;

    private static final byte BLOBDB_PIN = 1;
    private static final byte BLOBDB_APP = 2;
    private static final byte BLOBDB_REMINDER = 3;
    private static final byte BLOBDB_NOTIFICATION = 4;
    private static final byte BLOBDB_WEATHER = 5;
    private static final byte BLOBDB_CANNED_MESSAGES = 6;
    private static final byte BLOBDB_PREFERENCES = 7;
    private static final byte BLOBDB_APPSETTINGS = 9;
    private static final byte BLOBDB_APPGLANCE = 11;

    private static final byte BLOBDB_SUCCESS = 1;
    private static final byte BLOBDB_GENERALFAILURE = 2;
    private static final byte BLOBDB_INVALIDOPERATION = 3;
    private static final byte BLOBDB_INVALIDDATABASEID = 4;
    private static final byte BLOBDB_INVALIDDATA = 5;
    private static final byte BLOBDB_KEYDOESNOTEXIST = 6;
    private static final byte BLOBDB_DATABASEFULL = 7;
    private static final byte BLOBDB_DATASTALE = 8;


    private static final byte NOTIFICATION_EMAIL = 0;
    private static final byte NOTIFICATION_SMS = 1;
    private static final byte NOTIFICATION_TWITTER = 2;
    private static final byte NOTIFICATION_FACEBOOK = 3;

    private static final byte PHONECONTROL_ANSWER = 1;
    private static final byte PHONECONTROL_HANGUP = 2;
    private static final byte PHONECONTROL_GETSTATE = 3;
    private static final byte PHONECONTROL_INCOMINGCALL = 4;
    private static final byte PHONECONTROL_OUTGOINGCALL = 5;
    private static final byte PHONECONTROL_MISSEDCALL = 6;
    private static final byte PHONECONTROL_RING = 7;
    private static final byte PHONECONTROL_START = 8;
    private static final byte PHONECONTROL_END = 9;

    private static final byte MUSICCONTROL_SETMUSICINFO = 0x10;
    private static final byte MUSICCONTROL_SETPLAYSTATE = 0x11;

    private static final byte MUSICCONTROL_PLAYPAUSE = 1;
    private static final byte MUSICCONTROL_PAUSE = 2;
    private static final byte MUSICCONTROL_PLAY = 3;
    private static final byte MUSICCONTROL_NEXT = 4;
    private static final byte MUSICCONTROL_PREVIOUS = 5;
    private static final byte MUSICCONTROL_VOLUMEUP = 6;
    private static final byte MUSICCONTROL_VOLUMEDOWN = 7;
    private static final byte MUSICCONTROL_GETNOWPLAYING = 8;

    private static final byte MUSICCONTROL_STATE_PAUSED = 0x00;
    private static final byte MUSICCONTROL_STATE_PLAYING = 0x01;
    private static final byte MUSICCONTROL_STATE_REWINDING = 0x02;
    private static final byte MUSICCONTROL_STATE_FASTWORWARDING = 0x03;
    private static final byte MUSICCONTROL_STATE_UNKNOWN = 0x04;

    private static final byte NOTIFICATIONACTION_ACK = 0;
    private static final byte NOTIFICATIONACTION_NACK = 1;
    private static final byte NOTIFICATIONACTION_INVOKE = 0x02;
    private static final byte NOTIFICATIONACTION_RESPONSE = 0x11;

    private static final byte TIME_GETTIME = 0;
    private static final byte TIME_SETTIME = 2;
    private static final byte TIME_SETTIME_UTC = 3;

    private static final byte FIRMWAREVERSION_GETVERSION = 0;

    private static final byte APPMANAGER_GETAPPBANKSTATUS = 1;
    private static final byte APPMANAGER_REMOVEAPP = 2;
    private static final byte APPMANAGER_REFRESHAPP = 3;
    private static final byte APPMANAGER_GETUUIDS = 5;

    private static final int APPMANAGER_RES_SUCCESS = 1;

    private static final byte APPLICATIONMESSAGE_PUSH = 1;
    private static final byte APPLICATIONMESSAGE_REQUEST = 2;
    private static final byte APPLICATIONMESSAGE_ACK = (byte) 0xff;
    private static final byte APPLICATIONMESSAGE_NACK = (byte) 0x7f;

    private static final byte DATALOG_OPENSESSION = 0x01;
    private static final byte DATALOG_SENDDATA = 0x02;
    private static final byte DATALOG_CLOSE = 0x03;
    private static final byte DATALOG_TIMEOUT = 0x07;
    private static final byte DATALOG_REPORTSESSIONS = (byte) 0x84;
    private static final byte DATALOG_ACK = (byte) 0x85;
    private static final byte DATALOG_NACK = (byte) 0x86;

    private static final byte PING_PING = 0;
    private static final byte PING_PONG = 1;

    private static final byte PUTBYTES_INIT = 1;
    private static final byte PUTBYTES_SEND = 2;
    private static final byte PUTBYTES_COMMIT = 3;
    private static final byte PUTBYTES_ABORT = 4;
    private static final byte PUTBYTES_COMPLETE = 5;

    public static final byte PUTBYTES_TYPE_FIRMWARE = 1;
    public static final byte PUTBYTES_TYPE_RECOVERY = 2;
    public static final byte PUTBYTES_TYPE_SYSRESOURCES = 3;
    public static final byte PUTBYTES_TYPE_RESOURCES = 4;
    public static final byte PUTBYTES_TYPE_BINARY = 5;
    public static final byte PUTBYTES_TYPE_FILE = 6;
    public static final byte PUTBYTES_TYPE_WORKER = 7;

    private static final byte RESET_REBOOT = 0;

    private static final byte SCREENSHOT_TAKE = 0;

    private static final byte SYSTEMMESSAGE_NEWFIRMWAREAVAILABLE = 0;
    private static final byte SYSTEMMESSAGE_FIRMWARESTART = 1;
    private static final byte SYSTEMMESSAGE_FIRMWARECOMPLETE = 2;
    private static final byte SYSTEMMESSAGE_FIRMWAREFAIL = 3;
    private static final byte SYSTEMMESSAGE_FIRMWARE_UPTODATE = 4;
    private static final byte SYSTEMMESSAGE_FIRMWARE_OUTOFDATE = 5;
    private static final byte SYSTEMMESSAGE_STOPRECONNECTING = 6;
    private static final byte SYSTEMMESSAGE_STARTRECONNECTING = 7;

    private static final byte PHONEVERSION_REQUEST = 0;
    private static final byte PHONEVERSION_APPVERSION_MAGIC = 2; // increase this if pebble complains
    private static final byte PHONEVERSION_APPVERSION_MAJOR = 2;
    private static final byte PHONEVERSION_APPVERSION_MINOR = 3;
    private static final byte PHONEVERSION_APPVERSION_PATCH = 0;


    private static final int PHONEVERSION_SESSION_CAPS_GAMMARAY = 0x80000000;

    private static final int PHONEVERSION_REMOTE_CAPS_TELEPHONY = 0x00000010;
    private static final int PHONEVERSION_REMOTE_CAPS_SMS = 0x00000020;
    private static final int PHONEVERSION_REMOTE_CAPS_GPS = 0x00000040;
    private static final int PHONEVERSION_REMOTE_CAPS_BTLE = 0x00000080;
    private static final int PHONEVERSION_REMOTE_CAPS_REARCAMERA = 0x00000100;
    private static final int PHONEVERSION_REMOTE_CAPS_ACCEL = 0x00000200;
    private static final int PHONEVERSION_REMOTE_CAPS_GYRO = 0x00000400;
    private static final int PHONEVERSION_REMOTE_CAPS_COMPASS = 0x00000800;

    private static final byte PHONEVERSION_REMOTE_OS_UNKNOWN = 0;
    private static final byte PHONEVERSION_REMOTE_OS_IOS = 1;
    private static final byte PHONEVERSION_REMOTE_OS_ANDROID = 2;
    private static final byte PHONEVERSION_REMOTE_OS_OSX = 3;
    private static final byte PHONEVERSION_REMOTE_OS_LINUX = 4;
    private static final byte PHONEVERSION_REMOTE_OS_WINDOWS = 5;

    static final byte TYPE_BYTEARRAY = 0;
    private static final byte TYPE_CSTRING = 1;
    static final byte TYPE_UINT = 2;
    static final byte TYPE_INT = 3;

    private final short LENGTH_PREFIX = 4;

    private static final byte LENGTH_UUID = 16;

    private static final long GB_UUID_MASK = 0x4767744272646700L;

    // base is -8
    private static final String[] hwRevisions = {
            // Emulator
            "silk_bb2", "robert_bb", "silk_bb",
            "spalding_bb2", "snowy_bb2", "snowy_bb",
            "bb2", "bb",
            "unknown",
            // Pebble Classic Series
            "ev1", "ev2", "ev2_3", "ev2_4", "v1_5", "v2_0",
            // Pebble Time Series
            "snowy_evt2", "snowy_dvt", "spalding_dvt", "snowy_s3", "spalding",
            // Pebble 2 Series
            "silk_evt", "robert_evt", "silk"
    };

    private static final Random mRandom = new Random();

    int mFwMajor = 3;
    boolean mEnablePebbleKit = false;
    boolean mAlwaysACKPebbleKit = false;
    private boolean mForceProtocol = false;
    private GBDeviceEventScreenshot mDevEventScreenshot = null;
    private int mScreenshotRemaining = -1;

    //monochrome black + white
    private static final byte[] clut_pebble = {
            0x00, 0x00, 0x00, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00
    };

    // linear BGR222 (6 bit, 64 entries)
    private static final byte[] clut_pebbletime = new byte[]{
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

    public static final UUID UUID_PEBBLE_HEALTH = UUID.fromString("36d8c6ed-4c83-4fa1-a9e2-8f12dc941f8c"); // FIXME: store somewhere else, this is also accessed by other code
    public static final UUID UUID_WORKOUT = UUID.fromString("fef82c82-7176-4e22-88de-35a3fc18d43f"); // FIXME: store somewhere else, this is also accessed by other code
    public static final UUID UUID_WEATHER = UUID.fromString("61b22bc8-1e29-460d-a236-3fe409a439ff"); // FIXME: store somewhere else, this is also accessed by other code
    public static final UUID UUID_NOTIFICATIONS = UUID.fromString("b2cae818-10f8-46df-ad2b-98ad2254a3c1");

    private static final UUID UUID_GBPEBBLE = UUID.fromString("61476764-7465-7262-6469-656775527a6c");
    private static final UUID UUID_MORPHEUZ = UUID.fromString("5be44f1d-d262-4ea6-aa30-ddbec1e3cab2");
    private static final UUID UUID_MISFIT = UUID.fromString("0b73b76a-cd65-4dc2-9585-aaa213320858");
    private static final UUID UUID_PEBBLE_TIMESTYLE = UUID.fromString("4368ffa4-f0fb-4823-90be-f754b076bdaa");
    private static final UUID UUID_PEBSTYLE = UUID.fromString("da05e84d-e2a2-4020-a2dc-9cdcf265fcdd");
    private static final UUID UUID_MARIOTIME = UUID.fromString("43caa750-2896-4f46-94dc-1adbd4bc1ff3");
    private static final UUID UUID_HELTHIFY = UUID.fromString("7ee97b2c-95e8-4720-b94e-70fccd905d98");
    private static final UUID UUID_TREKVOLLE = UUID.fromString("2da02267-7a19-4e49-9ed1-439d25db14e4");
    private static final UUID UUID_SQUARE = UUID.fromString("cb332373-4ee5-4c5c-8912-4f62af2d756c");
    private static final UUID UUID_ZALEWSZCZAK_CROWEX = UUID.fromString("a88b3151-2426-43c6-b1d0-9b288b3ec47e");
    private static final UUID UUID_ZALEWSZCZAK_FANCY = UUID.fromString("014e17bf-5878-4781-8be1-8ef998cee1ba");
    private static final UUID UUID_ZALEWSZCZAK_TALLY = UUID.fromString("abb51965-52e2-440a-b93c-843eeacb697d");
    private static final UUID UUID_OBSIDIAN = UUID.fromString("ef42caba-0c65-4879-ab23-edd2bde68824");

    private static final UUID UUID_ZERO = new UUID(0, 0);

    private static final UUID UUID_LOCATION = UUID.fromString("2c7e6a86-51e5-4ddd-b606-db43d1e4ad28"); // might be the location of "Berlin" or "Auto"

    private final Map<UUID, AppMessageHandler> mAppMessageHandlers = new HashMap<>();

    private UUID currentRunningApp = UUID_ZERO;

    public PebbleProtocol(GBDevice device) {
        super(device);
        mAppMessageHandlers.put(UUID_MORPHEUZ, new AppMessageHandlerMorpheuz(UUID_MORPHEUZ, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_MISFIT, new AppMessageHandlerMisfit(UUID_MISFIT, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_PEBBLE_TIMESTYLE, new AppMessageHandlerTimeStylePebble(UUID_PEBBLE_TIMESTYLE, PebbleProtocol.this));
        //mAppMessageHandlers.put(UUID_PEBSTYLE, new AppMessageHandlerPebStyle(UUID_PEBSTYLE, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_MARIOTIME, new AppMessageHandlerMarioTime(UUID_MARIOTIME, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_HELTHIFY, new AppMessageHandlerHealthify(UUID_HELTHIFY, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_TREKVOLLE, new AppMessageHandlerTrekVolle(UUID_TREKVOLLE, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_SQUARE, new AppMessageHandlerSquare(UUID_SQUARE, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_ZALEWSZCZAK_CROWEX, new AppMessageHandlerZalewszczak(UUID_ZALEWSZCZAK_CROWEX, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_ZALEWSZCZAK_FANCY, new AppMessageHandlerZalewszczak(UUID_ZALEWSZCZAK_FANCY, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_ZALEWSZCZAK_TALLY, new AppMessageHandlerZalewszczak(UUID_ZALEWSZCZAK_TALLY, PebbleProtocol.this));
        mAppMessageHandlers.put(UUID_OBSIDIAN, new AppMessageHandlerObsidian(UUID_OBSIDIAN, PebbleProtocol.this));
    }

    private final HashMap<Byte, DatalogSession> mDatalogSessions = new HashMap<>();

    private byte[] encodeSimpleMessage(short endpoint, byte command) {
        final short LENGTH_SIMPLEMESSAGE = 1;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SIMPLEMESSAGE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SIMPLEMESSAGE);
        buf.putShort(endpoint);
        buf.put(command);

        return buf.array();
    }

    private byte[] encodeMessage(short endpoint, byte type, int cookie, String[] parts) {
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

        // for SMS that came in though the SMS receiver
        if (notificationSpec.sender != null) {
            title = notificationSpec.sender;
            subtitle = notificationSpec.subject;
        } else {
            title = notificationSpec.title;
        }

        Long ts = System.currentTimeMillis();
        if (mFwMajor < 3) {
            ts += (SimpleTimeZone.getDefault().getOffset(ts));
        }
        ts /= 1000;

        if (mFwMajor >= 3) {
            // 3.x notification
            return encodeBlobdbNotification(id, (int) (ts & 0xffffffffL), title, subtitle, notificationSpec.body, notificationSpec.sourceName, hasHandle, notificationSpec.type, notificationSpec.cannedReplies);
        } else if (mForceProtocol || notificationSpec.type != NotificationType.GENERIC_EMAIL) {
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
    public byte[] encodeDeleteNotification(int id) {
        return encodeBlobdb(new UUID(GB_UUID_MASK, id), BLOBDB_DELETE, BLOBDB_NOTIFICATION, null);
    }

    @Override
    public byte[] encodeAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        long id = calendarEventSpec.id != -1 ? calendarEventSpec.id : mRandom.nextLong();
        int iconId;
        ArrayList<Pair<Integer, Object>> attributes = new ArrayList<>();
        attributes.add(new Pair<>(1, (Object) calendarEventSpec.title));
        switch (calendarEventSpec.type) {
            case CalendarEventSpec.TYPE_SUNRISE:
                iconId = PebbleIconID.SUNRISE;
                break;
            case CalendarEventSpec.TYPE_SUNSET:
                iconId = PebbleIconID.SUNSET;
                break;
            default:
                iconId = PebbleIconID.TIMELINE_CALENDAR;
                attributes.add(new Pair<>(3, (Object) calendarEventSpec.description));
        }

        return encodeTimelinePin(new UUID(GB_UUID_MASK | calendarEventSpec.type, id), calendarEventSpec.timestamp, (short) (calendarEventSpec.durationInSeconds / 60), iconId, attributes);
    }

    @Override
    public byte[] encodeDeleteCalendarEvent(byte type, long id) {
        return encodeBlobdb(new UUID(GB_UUID_MASK | type, id), BLOBDB_DELETE, BLOBDB_PIN, null);
    }

    @Override
    public byte[] encodeSetTime() {
        final short LENGTH_SETTIME = 5;
        long ts = System.currentTimeMillis();
        long ts_offset = (SimpleTimeZone.getDefault().getOffset(ts));
        ByteBuffer buf;
        if (mFwMajor >= 3) {
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
        return encodeSetCallState("Where are you?", "Gadgetbridge", start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END);
        /*
        int ts = (int) (System.currentTimeMillis() / 1000);

        if (start) {
            //return encodeWeatherPin(ts, "Weather", "1°/-1°", "Gadgetbridge is Sunny", "Berlin", 37);
        }
        */
    }

    private byte[] encodeExtensibleNotification(int id, int timestamp, String title, String subtitle, String body, String sourceName, boolean hasHandle, String[] cannedReplies) {
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

        if (hasHandle && !"ALARMCLOCKRECEIVER".equals(sourceName)) {
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
        if (hasHandle && !"ALARMCLOCKRECEIVER".equals(sourceName)) {
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

    private byte[] encodeBlobdb(Object key, byte command, byte db, byte[] blob) {

        int length = 5;

        int key_length;
        if (key instanceof UUID) {
            key_length = LENGTH_UUID;
        } else if (key instanceof String) {
            key_length = ((String) key).getBytes().length;
        } else {
            LOG.warn("unknown key type");
            return null;
        }
        if (key_length > 255) {
            LOG.warn("key is too long");
            return null;
        }
        length += key_length;

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

        buf.put((byte) key_length);
        if (key instanceof UUID) {
            UUID uuid = (UUID) key;
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
            buf.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            buf.put(((String) key).getBytes());
        }

        if (blob != null) {
            buf.putShort((short) blob.length);
            buf.put(blob);
        }

        return buf.array();
    }

    byte[] encodeActivateHealth(boolean activate) {
        byte[] blob;
        if (activate) {

            ByteBuffer buf = ByteBuffer.allocate(9);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            ActivityUser activityUser = new ActivityUser();
            Integer heightMm = activityUser.getHeightCm() * 10;
            buf.putShort(heightMm.shortValue());
            Integer weigthDag = activityUser.getWeightKg() * 100;
            buf.putShort(weigthDag.shortValue());
            buf.put((byte) 0x01); //activate tracking
            buf.put((byte) 0x00); //activity Insights
            buf.put((byte) 0x00); //sleep Insights
            buf.put((byte) activityUser.getAge());
            buf.put((byte) activityUser.getGender());
            blob = buf.array();
        } else {
            blob = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }
        return encodeBlobdb("activityPreferences", BLOBDB_INSERT, BLOBDB_PREFERENCES, blob);
    }

    byte[] encodeSetSaneDistanceUnit(boolean sane) {
        byte value;
        if (sane) {
            value = 0x00;
        } else {
            value = 0x01;
        }
        return encodeBlobdb("unitsDistance", BLOBDB_INSERT, BLOBDB_PREFERENCES, new byte[]{value});
    }


    byte[] encodeActivateHRM(boolean activate) {
        return encodeBlobdb("hrmPreferences", BLOBDB_INSERT, BLOBDB_PREFERENCES,
                activate ? new byte[]{0x01} : new byte[]{0x00});
    }

    byte[] encodeActivateWeather(boolean activate) {
        if (activate) {
            ByteBuffer buf = ByteBuffer.allocate(0x61);
            buf.put((byte) 1);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putLong(UUID_LOCATION.getMostSignificantBits());
            buf.putLong(UUID_LOCATION.getLeastSignificantBits());
            // disable remaining 5 possible location
            buf.put(new byte[60 - LENGTH_UUID]);
            return encodeBlobdb("weatherApp", BLOBDB_INSERT, BLOBDB_APPSETTINGS, buf.array());
        } else {
            return encodeBlobdb("weatherApp", BLOBDB_DELETE, BLOBDB_APPSETTINGS, null);
        }
    }

    byte[] encodeReportDataLogSessions() {
        return encodeSimpleMessage(ENDPOINT_DATALOG, DATALOG_REPORTSESSIONS);
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

    private byte[] encodeTimelinePin(UUID uuid, int timestamp, short duration, int icon_id, List<Pair<Integer, Object>> attributes) {
        final short TIMELINE_PIN_LENGTH = 46;

        //FIXME: dont depend layout on icon :P
        byte layout_id = 0x01;
        if (icon_id == PebbleIconID.TIMELINE_CALENDAR) {
            layout_id = 0x02;
        }
        icon_id |= 0x80000000;
        byte attributes_count = 2;
        byte actions_count = 0;

        int attributes_length = 10;
        for (Pair<Integer, Object> pair : attributes) {
            if (pair.first == null || pair.second == null)
                continue;
            if (pair.second instanceof Integer) {
                attributes_length += 7;
            } else if (pair.second instanceof Byte) {
                attributes_length += 4;
            } else if (pair.second instanceof String) {
                attributes_length += ((String) pair.second).getBytes().length + 3;
            } else if (pair.second instanceof byte[]) {
                attributes_length += ((byte[]) pair.second).length + 3;
            } else {
                LOG.warn("unsupported type for timeline attributes: " + pair.second.getClass().toString());
            }
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
        buf.put(layout_id); // layout was (0x02 = pin?), 0x01 needed for subtitle but seems to do no harm if there isn't one
        buf.putShort((short) attributes_length); // total length of all attributes and actions in bytes
        buf.put(attributes_count);
        buf.put(actions_count);

        buf.put((byte) 4); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(icon_id);

        for (Pair<Integer, Object> pair : attributes) {
            if (pair.first == null || pair.second == null)
                continue;
            buf.put(pair.first.byteValue());
            if (pair.second instanceof Integer) {
                buf.putShort((short) 4);
                buf.putInt(((Integer) pair.second));
            } else if (pair.second instanceof Byte) {
                buf.putShort((short) 1);
                buf.put((Byte) pair.second);
            } else if (pair.second instanceof String) {
                buf.putShort((short) ((String) pair.second).getBytes().length);
                buf.put(((String) pair.second).getBytes());
            } else if (pair.second instanceof byte[]) {
                buf.putShort((short) ((byte[]) pair.second).length);
                buf.put((byte[]) pair.second);
            }
        }

        return encodeBlobdb(uuid, BLOBDB_INSERT, BLOBDB_PIN, buf.array());
    }

    private byte[] encodeBlobdbNotification(int id, int timestamp, String title, String subtitle, String body, String sourceName, boolean hasHandle, NotificationType notificationType, String[] cannedReplies) {
        final short NOTIFICATION_PIN_LENGTH = 46;
        final short ACTION_LENGTH_MIN = 10;

        String[] parts = {title, subtitle, body};

        if(notificationType == null) {
            notificationType = NotificationType.UNKNOWN;
        }

        int icon_id = notificationType.icon;
        byte color_id = notificationType.color;

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
        if (hasHandle && !"ALARMCLOCKRECEIVER".equals(sourceName)) {
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

        short pin_length = (short) (NOTIFICATION_PIN_LENGTH + attributes_length);

        ByteBuffer buf = ByteBuffer.allocate(pin_length);

        // pin - 46 bytes
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(GB_UUID_MASK);
        buf.putLong(id);
        buf.putLong(UUID_NOTIFICATIONS.getMostSignificantBits());
        buf.putLong(UUID_NOTIFICATIONS.getLeastSignificantBits());
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
        if (hasHandle && !"ALARMCLOCKRECEIVER".equals(sourceName)) {
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

    private byte[] encodeActionResponse2x(int id, byte actionId, int iconId, String caption) {
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

    private byte[] encodeWeatherPin(int timestamp, String title, String subtitle, String body, String location, int iconId) {
        final short NOTIFICATION_PIN_LENGTH = 46;
        final short ACTION_LENGTH_MIN = 10;

        String[] parts = {title, subtitle, body, location, "test", "test"};

        // Calculate length first
        byte actions_count = 1;
        short actions_length;
        String remove_string = "Remove";
        actions_length = (short) (ACTION_LENGTH_MIN * actions_count + remove_string.getBytes().length);

        byte attributes_count = 3;
        short attributes_length = (short) (21 + actions_length);
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_count++;
                attributes_length += (3 + s.getBytes().length);
            }
        }

        UUID uuid = UUID.fromString("61b22bc8-1e29-460d-a236-3fe409a43901");

        short pin_length = (short) (NOTIFICATION_PIN_LENGTH + attributes_length);

        ByteBuffer buf = ByteBuffer.allocate(pin_length);

        // pin (46 bytes)
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits() | 0xff);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(timestamp); // 32-bit timestamp
        buf.putShort((short) 0); // duration
        buf.put((byte) 0x02); // type (0x02 = pin)
        buf.putShort((short) 0x0001); // flags 0x0001 = ?
        buf.put((byte) 0x06); // layout (0x06 = weather)
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
                if (attribute_id == 4) {
                    buf.put((byte) 11);
                } else if (attribute_id == 5) {
                    buf.put((byte) 25);
                } else if (attribute_id == 6) {
                    buf.put((byte) 26);
                } else {
                    buf.put(attribute_id);
                }
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }

        buf.put((byte) 4); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(0x80000000 | iconId);

        buf.put((byte) 6); // icon
        buf.putShort((short) 4); // length of int
        buf.putInt(0x80000000 | iconId);

        buf.put((byte) 14); // last updated
        buf.putShort((short) 4); // length of int
        buf.putInt(timestamp);

        // remove action
        buf.put((byte) 123); // action id
        buf.put((byte) 0x09); // remove
        buf.put((byte) 0x01); // number attributes
        buf.put((byte) 0x01); // attribute id (title)
        buf.putShort((short) remove_string.getBytes().length);
        buf.put(remove_string.getBytes());

        return encodeBlobdb(uuid, BLOBDB_INSERT, BLOBDB_PIN, buf.array());
    }


    @Override
    public byte[] encodeSendWeather(WeatherSpec weatherSpec) {
        byte[] forecastProtocol = null;
        byte[] watchfaceProtocol = null;
        int length = 0;
        if (mFwMajor >= 4) {
            forecastProtocol = encodeWeatherForecast(weatherSpec);
            length += forecastProtocol.length;
        }
        AppMessageHandler handler = mAppMessageHandlers.get(currentRunningApp);
        if (handler != null) {
            watchfaceProtocol = handler.encodeUpdateWeather(weatherSpec);
            if (watchfaceProtocol != null) {
                length += watchfaceProtocol.length;
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(length);

        if (forecastProtocol != null) {
            buf.put(forecastProtocol);
        }
        if (watchfaceProtocol != null) {
            buf.put(watchfaceProtocol);
        }

        return buf.array();
    }

    private byte[] encodeWeatherForecast(WeatherSpec weatherSpec) {
        final short WEATHER_FORECAST_LENGTH = 20;

        String[] parts = {weatherSpec.location, weatherSpec.currentCondition};

        // Calculate length first
        short attributes_length = 0;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_length += (2 + s.getBytes().length);
            }
        }

        short pin_length = (short) (WEATHER_FORECAST_LENGTH + attributes_length);

        ByteBuffer buf = ByteBuffer.allocate(pin_length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 3); // unknown, always 3?
        buf.putShort((short) (weatherSpec.currentTemp - 273));
        buf.put(Weather.mapToPebbleCondition(weatherSpec.currentConditionCode));
        buf.putShort((short) (weatherSpec.todayMaxTemp - 273));
        buf.putShort((short) (weatherSpec.todayMinTemp - 273));
        buf.put(Weather.mapToPebbleCondition(weatherSpec.tomorrowConditionCode));
        buf.putShort((short) (weatherSpec.tomorrowMaxTemp - 273));
        buf.putShort((short) (weatherSpec.tomorrowMinTemp - 273));
        buf.putInt(weatherSpec.timestamp);
        buf.put((byte) 0); // automatic location 0=manual 1=auto
        buf.putShort(attributes_length);

        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 512) partlength = 512;
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }

        return encodeBlobdb(UUID_LOCATION, BLOBDB_INSERT, BLOBDB_WEATHER, buf.array());
    }

    private byte[] encodeActionResponse(UUID uuid, int iconId, String caption) {
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

    byte[] encodeInstallMetadata(UUID uuid, String appName, short appVersion, short sdkVersion, int flags, int iconId) {
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

    byte[] encodeAppFetchAck() {
        final short LENGTH_APPFETCH = 2;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_APPFETCH);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_APPFETCH);
        buf.putShort(ENDPOINT_APPFETCH);
        buf.put((byte) 0x01);
        buf.put((byte) 0x01);

        return buf.array();
    }

    byte[] encodeGetTime() {
        return encodeSimpleMessage(ENDPOINT_TIME, TIME_GETTIME);
    }

    @Override
    public byte[] encodeSetCallState(String number, String name, int command) {
        String[] parts = {number, name};
        byte pebbleCmd;
        switch (command) {
            case CallSpec.CALL_START:
                pebbleCmd = PHONECONTROL_START;
                break;
            case CallSpec.CALL_END:
                pebbleCmd = PHONECONTROL_END;
                break;
            case CallSpec.CALL_INCOMING:
                pebbleCmd = PHONECONTROL_INCOMINGCALL;
                break;
            case CallSpec.CALL_OUTGOING:
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

    public byte[] encodeSetMusicState(byte state, int position, int playRate, byte shuffle, byte repeat) {
        if (mFwMajor < 3) {
            return null;
        }

        byte playState;

        switch (state) {
            case MusicStateSpec.STATE_PLAYING:
                playState = MUSICCONTROL_STATE_PLAYING;
                break;
            case MusicStateSpec.STATE_PAUSED:
                playState = MUSICCONTROL_STATE_PAUSED;
                break;
            default:
                playState = MUSICCONTROL_STATE_UNKNOWN;
                break;
        }

        int length = LENGTH_PREFIX + 12;
        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length - LENGTH_PREFIX));
        buf.putShort(ENDPOINT_MUSICCONTROL);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(MUSICCONTROL_SETPLAYSTATE);
        buf.put(playState);
        buf.putInt(position * 1000);
        buf.putInt(playRate);
        buf.put(shuffle);
        buf.put(repeat);

        return buf.array();
    }

    @Override
    public byte[] encodeSetMusicInfo(String artist, String album, String track, int duration, int trackCount, int trackNr) {
        String[] parts = {artist, album, track};
        if (duration == 0 || mFwMajor < 3) {
            return encodeMessage(ENDPOINT_MUSICCONTROL, MUSICCONTROL_SETMUSICINFO, 0, parts);
        } else {
            // Calculate length first
            int length = LENGTH_PREFIX + 9;
            if (parts != null) {
                for (String s : parts) {
                    if (s == null || s.equals("")) {
                        length++; // encode null or empty strings as 0x00 later
                        continue;
                    }
                    length += (1 + s.getBytes().length);
                }
            }

            // Encode Prefix
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort((short) (length - LENGTH_PREFIX));
            buf.putShort(ENDPOINT_MUSICCONTROL);
            buf.put(MUSICCONTROL_SETMUSICINFO);

            // Encode Pascal-Style Strings
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    buf.put((byte) 0x00);
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put((byte) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }

            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(duration * 1000);
            buf.putShort((short) (trackCount & 0xffff));
            buf.putShort((short) (trackNr & 0xffff));

            return buf.array();
        }
    }

    @Override
    public byte[] encodeFirmwareVersionReq() {
        return encodeSimpleMessage(ENDPOINT_FIRMWAREVERSION, FIRMWAREVERSION_GETVERSION);
    }

    @Override
    public byte[] encodeAppInfoReq() {
        if (mFwMajor >= 3) {
            return null; // can't do this on 3.x :(
        }
        return encodeSimpleMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETUUIDS);
    }

    @Override
    public byte[] encodeAppStart(UUID uuid, boolean start) {
        if (mFwMajor >= 3) {
            final short LENGTH_APPRUNSTATE = 17;
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
        if (mFwMajor >= 3) {
            if (UUID_PEBBLE_HEALTH.equals(uuid)) {
                return encodeActivateHealth(false);
            }
            if (UUID_WORKOUT.equals(uuid)) {
                return encodeActivateHRM(false);
            }
            if (UUID_WEATHER.equals(uuid)) { //TODO: probably it wasn't present in firmware 3
                return encodeActivateWeather(false);
            }
            return encodeBlobdb(uuid, BLOBDB_DELETE, BLOBDB_APP, null);
        } else {
            final short LENGTH_REMOVEAPP_2X = 17;
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
        final short LENGTH_PHONEVERSION = 17;
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
        buf.put((byte) 4); // major
        buf.put((byte) 1); // minor
        buf.put((byte) 1); // patch
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(0x00000000000029af); //flags

        return buf.array();
    }

    private byte[] encodePhoneVersion(byte os) {
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

    @Override
    public byte[] encodeAppReorder(UUID[] uuids) {
        int length = 2 + uuids.length * LENGTH_UUID;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) length);
        buf.putShort(ENDPOINT_APPREORDER);
        buf.put((byte) 0x01);
        buf.put((byte) uuids.length);
        for (UUID uuid : uuids) {
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
        }

        return buf.array();
    }

    @Override
    public byte[] encodeSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

        if (cannedMessagesSpec.cannedMessages == null || cannedMessagesSpec.cannedMessages.length == 0) {
            return null;
        }

        String blobDBKey;
        switch (cannedMessagesSpec.type) {
            case CannedMessagesSpec.TYPE_MISSEDCALLS:
                blobDBKey = "com.pebble.android.phone";
                break;
            case CannedMessagesSpec.TYPE_NEWSMS:
                blobDBKey = "com.pebble.sendText";
                break;
            default:
                return null;
        }

        int replies_length = -1;

        for (String reply : cannedMessagesSpec.cannedMessages) {
            replies_length += reply.getBytes().length + 1;
        }

        ByteBuffer buf = ByteBuffer.allocate(12 + replies_length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x00000000); // unknown
        buf.put((byte) 0x00); // attributes count?
        buf.put((byte) 0x01); // actions count?

        // action
        buf.put((byte) 0x00); // action id
        buf.put((byte) 0x03); // action type = reply
        buf.put((byte) 0x01); // attributes count
        buf.put((byte) 0x08); // canned messages
        buf.putShort((short) replies_length);
        for (int i = 0; i < cannedMessagesSpec.cannedMessages.length - 1; i++) {
            buf.put(cannedMessagesSpec.cannedMessages[i].getBytes());
            buf.put((byte) 0x00);
        }
        // last one must not be zero terminated, else we get an additional empty reply
        buf.put(cannedMessagesSpec.cannedMessages[cannedMessagesSpec.cannedMessages.length - 1].getBytes());

        return encodeBlobdb(blobDBKey, BLOBDB_INSERT, BLOBDB_CANNED_MESSAGES, buf.array());
    }

    /* pebble specific install methods */
    byte[] encodeUploadStart(byte type, int app_id, int size, String filename) {
        short length;
        if (mFwMajor >= 3 && (type != PUTBYTES_TYPE_FILE)) {
            length = (short) 10;
            type |= 0b10000000;
        } else {
            length = (short) 7;
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

        if (mFwMajor >= 3 && (type != PUTBYTES_TYPE_FILE)) {
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

    byte[] encodeUploadChunk(int token, byte[] buffer, int size) {
        final short LENGTH_UPLOADCHUNK = 9;
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

    byte[] encodeUploadCommit(int token, int crc) {
        final short LENGTH_UPLOADCOMMIT = 9;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMMIT);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMMIT);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMMIT);
        buf.putInt(token);
        buf.putInt(crc);
        return buf.array();
    }

    byte[] encodeUploadComplete(int token) {
        final short LENGTH_UPLOADCOMPLETE = 5;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMPLETE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMPLETE);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMPLETE);
        buf.putInt(token);
        return buf.array();
    }

    byte[] encodeUploadCancel(int token) {
        final short LENGTH_UPLOADCANCEL = 5;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCANCEL);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCANCEL);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_ABORT);
        buf.putInt(token);
        return buf.array();
    }

    private byte[] encodeSystemMessage(byte systemMessage) {
        final short LENGTH_SYSTEMMESSAGE = 2;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SYSTEMMESSAGE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SYSTEMMESSAGE);
        buf.putShort(ENDPOINT_SYSTEMMESSAGE);
        buf.put((byte) 0);
        buf.put(systemMessage);
        return buf.array();

    }

    byte[] encodeInstallFirmwareStart() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARESTART);
    }

    byte[] encodeInstallFirmwareComplete() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARECOMPLETE);
    }

    public byte[] encodeInstallFirmwareError() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWAREFAIL);
    }


    byte[] encodeAppRefresh(int index) {
        final short LENGTH_REFRESHAPP = 5;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REFRESHAPP);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_REFRESHAPP);
        buf.putShort(ENDPOINT_APPMANAGER);
        buf.put(APPMANAGER_REFRESHAPP);
        buf.putInt(index);

        return buf.array();
    }

    private byte[] encodeDatalog(byte handle, byte reply) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 2);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 2);
        buf.putShort(ENDPOINT_DATALOG);
        buf.put(reply);
        buf.put(handle);

        return buf.array();
    }

    byte[] encodeApplicationMessageAck(UUID uuid, byte id) {
        if (uuid == null) {
            uuid = currentRunningApp;
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 18); // +ACK

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 18);
        buf.putShort(ENDPOINT_APPLICATIONMESSAGE);
        buf.put(APPLICATIONMESSAGE_ACK);
        buf.put(id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());

        return buf.array();
    }

    private byte[] encodePing(byte command, int cookie) {
        final short LENGTH_PING = 5;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PING);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PING);
        buf.putShort(ENDPOINT_PING);
        buf.put(command);
        buf.putInt(cookie);

        return buf.array();
    }

    byte[] encodeEnableAppLogs(boolean enable) {
        final short LENGTH_APPLOGS = 1;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_APPLOGS);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_APPLOGS);
        buf.putShort(ENDPOINT_APPLOGS);
        buf.put((byte) (enable ? 1 : 0));

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
                    if (length == 1) {
                        dict.add(new Pair<Integer, Object>(key, buf.get()));
                    } else if (length == 2) {
                        dict.add(new Pair<Integer, Object>(key, buf.getShort()));
                    } else {
                        dict.add(new Pair<Integer, Object>(key, buf.getInt()));
                    }
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
            if (type == TYPE_CSTRING) {
                length--;
            }
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
                        jsonObject.put("value", new String(Base64.encode(bytes, Base64.NO_WRAP)));
                    } else {
                        jsonObject.put("type", "string");
                        jsonObject.put("value", new String(bytes));
                        buf.get(); // skip null-termination;
                    }
                    break;
                default:
                    LOG.info("unknown type in appmessage, ignoring");
                    return null;
            }
            jsonArray.put(jsonObject);
        }

        GBDeviceEventSendBytes sendBytesAck = null;
        if (mAlwaysACKPebbleKit) {
            // this is a hack we send an ack to the Pebble immediately because somebody said it helps some PebbleKit apps :P
             sendBytesAck = new GBDeviceEventSendBytes();
             sendBytesAck.encodedBytes = encodeApplicationMessageAck(uuid, last_id);
        }
        GBDeviceEventAppMessage appMessage = new GBDeviceEventAppMessage();
        appMessage.appUUID = uuid;
        appMessage.id = last_id & 0xff;
        appMessage.message = jsonArray.toString();
        return new GBDeviceEvent[]{appMessage, sendBytesAck};
    }

    byte[] encodeApplicationMessagePush(short endpoint, UUID uuid, ArrayList<Pair<Integer, Object>> pairs) {
        int length = LENGTH_UUID + 3; // UUID + (PUSH + id + length of dict)
        for (Pair<Integer, Object> pair : pairs) {
            if (pair.first == null || pair.second == null)
                continue;
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
            } else {
                LOG.warn("unknown type: " + pair.second.getClass().toString());
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
            if (pair.first == null || pair.second == null)
                continue;
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

    byte[] encodeApplicationMessageFromJSON(UUID uuid, JSONArray jsonArray) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                String type = (String) jsonObject.get("type");
                int key = jsonObject.getInt("key");
                int length = jsonObject.getInt("length");
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

    private byte reverseBits(byte in) {
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
            UUID uuid = new UUID(0,0);
            if (mFwMajor >= 3) {
                uuid = getUUID(buf);
                id = (int) (uuid.getLeastSignificantBits() & 0xffffffffL);
            } else {
                id = buf.getInt();
            }
            byte action = buf.get();
            if (action >= 0x00 && action <= 0x05) {
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
                    case 0x00:
                        boolean failed = true;
                        byte attribute_count = buf.get();
                        if (attribute_count > 0) {
                            byte attribute = buf.get();
                            if (attribute == 0x01) { // reply string is in attribute 0x01
                                short length = buf.getShort();
                                if (length > 64) length = 64;
                                byte[] reply = new byte[length];
                                buf.get(reply);
                                devEvtNotificationControl.phoneNumber = null;
                                if (buf.remaining() > 1 && buf.get() == 0x0c) {
                                    short phoneNumberLength = buf.getShort();
                                    byte[] phoneNumberBytes = new byte[phoneNumberLength];
                                    buf.get(phoneNumberBytes);
                                    devEvtNotificationControl.phoneNumber = new String(phoneNumberBytes);
                                }
                                devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                                devEvtNotificationControl.reply = new String(reply);
                                caption = "SENT";
                                icon_id = PebbleIconID.RESULT_SENT;
                                failed = false;
                            }
                        }
                        if (failed) {
                            caption = "FAILED";
                            icon_id = PebbleIconID.RESULT_FAILED;
                            devEvtNotificationControl = null; // error
                        }
                        break;
                }
                GBDeviceEventSendBytes sendBytesAck = null;
                if (mFwMajor >= 3 || needsAck2x) {
                    sendBytesAck = new GBDeviceEventSendBytes();
                    if (mFwMajor >= 3) {
                        sendBytesAck.encodedBytes = encodeActionResponse(uuid, icon_id, caption);
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

    private void decodeAppLogs(ByteBuffer buf) {
        UUID uuid = getUUID(buf);
        int timestamp = buf.getInt();
        int logLevel = buf.get() & 0xff;
        int messageLength = buf.get() & 0xff;
        int lineNumber = buf.getShort() & 0xffff;
        String fileName = getFixedString(buf, 16);
        String message = getFixedString(buf, messageLength);
        LOG.debug("APP_LOGS (" + logLevel +") from uuid " + uuid.toString() + " in " + fileName + ":" + lineNumber + " " + message);
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

    private GBDeviceEvent[] decodeAppRunState(ByteBuffer buf) {
        byte command = buf.get();
        UUID uuid = getUUID(buf);
        final String ENDPOINT_NAME = "APPRUNSTATE";
        switch (command) {
            case APPRUNSTATE_START:
                LOG.info(ENDPOINT_NAME + ": started " + uuid);
                currentRunningApp = uuid;
                AppMessageHandler handler = mAppMessageHandlers.get(uuid);
                if (handler != null) {
                    return handler.onAppStart();
                }
                else {
                    GBDeviceEventAppManagement gbDeviceEventAppManagement = new GBDeviceEventAppManagement();
                    gbDeviceEventAppManagement.uuid = uuid;
                    gbDeviceEventAppManagement.type = GBDeviceEventAppManagement.EventType.START;
                    gbDeviceEventAppManagement.event = GBDeviceEventAppManagement.Event.SUCCESS;
                    return new GBDeviceEvent[] {gbDeviceEventAppManagement};
                }
            case APPRUNSTATE_STOP:
                LOG.info(ENDPOINT_NAME + ": stopped " + uuid);
                break;
            default:
                LOG.info(ENDPOINT_NAME + ": (cmd:" + command + ")" + uuid);
                break;
        }
        return new GBDeviceEvent[]{null};
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
            UUID uuid = getUUID(buf);
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

    private GBDeviceEvent[] decodeDatalog(ByteBuffer buf, short length) {
        byte command = buf.get();
        byte id = buf.get();
        GBDeviceEvent[] devEvtsDataLogging = null;
        switch (command) {
            case DATALOG_TIMEOUT:
                LOG.info("DATALOG TIMEOUT. id=" + (id & 0xff) + " - ignoring");
                return null;
            case DATALOG_SENDDATA:
                buf.order(ByteOrder.LITTLE_ENDIAN);
                int items_left = buf.getInt();
                int crc = buf.getInt();
                DatalogSession datalogSession = mDatalogSessions.get(id);
                LOG.info("DATALOG SENDDATA. id=" + (id & 0xff) + ", items_left=" + items_left + ", total length=" + (length - 10));
                if (datalogSession != null) {
                    LOG.info("DATALOG UUID=" + datalogSession.uuid + ", tag=" + datalogSession.tag + datalogSession.getTaginfo() + ", itemSize=" + datalogSession.itemSize + ", itemType=" + datalogSession.itemType);
                    if (!datalogSession.uuid.equals(UUID_ZERO) && datalogSession.getClass().equals(DatalogSession.class) && mEnablePebbleKit) {
                        devEvtsDataLogging = datalogSession.handleMessageForPebbleKit(buf, length - 10);
                    } else {
                        devEvtsDataLogging = datalogSession.handleMessage(buf, length - 10);
                    }
                }
                break;
            case DATALOG_OPENSESSION:
                UUID uuid = getUUID(buf);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                int timestamp = buf.getInt();
                int log_tag = buf.getInt();
                byte item_type = buf.get();
                short item_size = buf.getShort();
                LOG.info("DATALOG OPENSESSION. id=" + (id & 0xff) + ", App UUID=" + uuid.toString() + ", log_tag=" + log_tag + ", item_type=" + item_type + ", itemSize=" + item_size);
                if (!mDatalogSessions.containsKey(id)) {
                    if (uuid.equals(UUID_ZERO) && log_tag == 78) {
                        mDatalogSessions.put(id, new DatalogSessionAnalytics(id, uuid, timestamp, log_tag, item_type, item_size, getDevice()));
                    } else if (uuid.equals(UUID_ZERO) && log_tag == 81) {
                        mDatalogSessions.put(id, new DatalogSessionHealthSteps(id, uuid, timestamp, log_tag, item_type, item_size, getDevice()));
                    } else if (uuid.equals(UUID_ZERO) && log_tag == 83) {
                        mDatalogSessions.put(id, new DatalogSessionHealthSleep(id, uuid, timestamp, log_tag, item_type, item_size, getDevice()));
                    } else if (uuid.equals(UUID_ZERO) && log_tag == 84) {
                        mDatalogSessions.put(id, new DatalogSessionHealthOverlayData(id, uuid, timestamp, log_tag, item_type, item_size, getDevice()));
                    } else if (uuid.equals(UUID_ZERO) && log_tag == 85) {
                        mDatalogSessions.put(id, new DatalogSessionHealthHR(id, uuid, timestamp, log_tag, item_type, item_size, getDevice()));
                    } else {
                        mDatalogSessions.put(id, new DatalogSession(id, uuid, timestamp, log_tag, item_type, item_size));
                    }
                }
                devEvtsDataLogging = new GBDeviceEvent[]{null};
                break;
            case DATALOG_CLOSE:
                LOG.info("DATALOG_CLOSE. id=" + (id & 0xff));
                datalogSession = mDatalogSessions.get(id);
                if (datalogSession != null) {
                    if (!datalogSession.uuid.equals(UUID_ZERO) && datalogSession.getClass().equals(DatalogSession.class) && mEnablePebbleKit) {
                        GBDeviceEventDataLogging dataLogging = new GBDeviceEventDataLogging();
                        dataLogging.command = GBDeviceEventDataLogging.COMMAND_FINISH_SESSION;
                        dataLogging.appUUID = datalogSession.uuid;
                        dataLogging.tag = datalogSession.tag;
                        devEvtsDataLogging = new GBDeviceEvent[]{dataLogging, null};
                    }
                    mDatalogSessions.remove(id);
                }
                break;
            default:
                LOG.info("unknown DATALOG command: " + (command & 0xff));
                break;
        }
        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();

        if (devEvtsDataLogging != null) {
            // append ack
            LOG.info("sending ACK (0x85)");
            sendBytes.encodedBytes = encodeDatalog(id, DATALOG_ACK);
            devEvtsDataLogging[devEvtsDataLogging.length - 1] = sendBytes;
        } else {
            LOG.info("sending NACK (0x86)");
            sendBytes.encodedBytes = encodeDatalog(id, DATALOG_NACK);
            devEvtsDataLogging = new GBDeviceEvent[]{sendBytes};
        }
        return devEvtsDataLogging;
    }

    private GBDeviceEvent decodeAppReorder(ByteBuffer buf) {
        byte status = buf.get();
        if (status == 1) {
            LOG.info("app reordering successful");
        } else {
            LOG.info("app reordering returned status " + status);
        }
        return null;
    }

    private GBDeviceEvent decodeVoiceControl(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte command = buf.get();
        int flags = buf.getInt();
        byte session_type = buf.get(); //0x01 dictation 0x02 command
        short session_id = buf.getShort();
        //attributes
        byte count = buf.get();
        byte type = buf.get();
        short length = buf.getShort();
        byte[] version = new byte[20];
        buf.get(version); //it's a string like "1.2rc1"
        int sample_rate = buf.getInt();
        short bit_rate = buf.getShort();
        byte bitstream_version = buf.get();
        short frame_size = buf.getShort();

        GBDeviceEventSendBytes sendBytes = new GBDeviceEventSendBytes();
        if (command == 0x01) { //session setup
            sendBytes.encodedBytes = null;
        } else if (command == 0x02) { //dictation result
            sendBytes.encodedBytes = null;
        }
        return sendBytes;
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        buf.order(ByteOrder.BIG_ENDIAN);
        short length = buf.getShort();
        short endpoint = buf.getShort();
        GBDeviceEvent devEvts[] = null;
        byte pebbleCmd;
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
                versionCmd.fwVersion = getFixedString(buf, 32);

                mFwMajor = versionCmd.fwVersion.charAt(1) - 48;
                LOG.info("Pebble firmware major detected as " + mFwMajor);

                byte[] tmp = new byte[9];
                buf.get(tmp, 0, 9);
                int hwRev = buf.get() + 8;
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
                        appInfoCmd.apps = new GBDeviceApp[slotsUsed];
                        boolean[] slotInUse = new boolean[slotCount];

                        for (int i = 0; i < slotsUsed; i++) {
                            int id = buf.getInt();
                            int index = buf.getInt();
                            slotInUse[index] = true;
                            String appName = getFixedString(buf, 32);
                            String appCreator = getFixedString(buf, 32);

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
                            appInfoCmd.apps[i] = new GBDeviceApp(tmpUUIDS.get(i), appName, appCreator, appVersion.toString(), appType);
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
                            UUID uuid = getUUID(buf);
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
                UUID uuid = getUUID(buf);

                switch (pebbleCmd) {
                    case APPLICATIONMESSAGE_PUSH:
                        LOG.info((endpoint == ENDPOINT_LAUNCHER ? "got LAUNCHER PUSH from UUID : " : "got APPLICATIONMESSAGE PUSH from UUID : ")  + uuid);
                        AppMessageHandler handler = mAppMessageHandlers.get(uuid);
                        if (handler != null) {
                            if (handler.isEnabled()) {
                                if (endpoint == ENDPOINT_APPLICATIONMESSAGE) {
                                    ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                                    devEvts = handler.handleMessage(dict);
                                }
                                else {
                                    currentRunningApp = uuid;
                                    devEvts = handler.onAppStart();
                                }
                            } else {
                                devEvts = new GBDeviceEvent[]{null};
                            }
                        } else {
                            try {
                                if (endpoint == ENDPOINT_APPLICATIONMESSAGE) {
                                    devEvts = decodeDictToJSONAppMessage(uuid, buf);
                                }
                                else {
                                    currentRunningApp = uuid;
                                    GBDeviceEventAppManagement gbDeviceEventAppManagement = new GBDeviceEventAppManagement();
                                    gbDeviceEventAppManagement.uuid = uuid;
                                    gbDeviceEventAppManagement.type = GBDeviceEventAppManagement.EventType.START;
                                    gbDeviceEventAppManagement.event = GBDeviceEventAppManagement.Event.SUCCESS;
                                    devEvts = new GBDeviceEvent[] {gbDeviceEventAppManagement};
                                }
                            } catch (JSONException e) {
                                LOG.error(e.getMessage());
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
                devEvts = decodeDatalog(buf, length);
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
                devEvts = decodeAppRunState(buf);
                break;
            case ENDPOINT_BLOBDB:
                devEvts = new GBDeviceEvent[]{decodeBlobDb(buf)};
                break;
            case ENDPOINT_APPREORDER:
                devEvts = new GBDeviceEvent[]{decodeAppReorder(buf)};
                break;
            case ENDPOINT_APPLOGS:
                decodeAppLogs(buf);
                break;
//            case ENDPOINT_VOICECONTROL:
//                devEvts = new GBDeviceEvent[]{decodeVoiceControl(buf)};
//            case ENDPOINT_AUDIOSTREAM:
//                LOG.debug(GB.hexdump(responseData, 0, responseData.length));
//                break;
            default:
                break;
        }

        return devEvts;
    }

    void setForceProtocol(boolean force) {
        LOG.info("setting force protocol to " + force);
        mForceProtocol = force;
    }

    void setAlwaysACKPebbleKit(boolean alwaysACKPebbleKit) {
        LOG.info("setting always ACK PebbleKit to " + alwaysACKPebbleKit);
        mAlwaysACKPebbleKit = alwaysACKPebbleKit;
    }

    void setEnablePebbleKit(boolean enablePebbleKit) {
        LOG.info("setting enable PebbleKit support to " + enablePebbleKit);
        mEnablePebbleKit = enablePebbleKit;
    }

    private String getFixedString(ByteBuffer buf, int length) {
        byte[] tmp = new byte[length];
        buf.get(tmp, 0, length);

        return new String(tmp).trim();
    }

    private UUID getUUID(ByteBuffer buf) {
        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.BIG_ENDIAN);
        long uuid_high = buf.getLong();
        long uuid_low = buf.getLong();
        buf.order(byteOrder);
        return new UUID(uuid_high, uuid_low);
    }
}
