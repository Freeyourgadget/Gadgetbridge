package nodomain.freeyourgadget.gadgetbridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.SimpleTimeZone;

public class PebbleProtocol {
    static final short ENDPOINT_FIRMWARE = 1;
    static final short ENDPOINT_TIME = 11;
    static final short ENDPOINT_FIRMWAREVERSION = 16;
    static final short ENDPOINT_PHONEVERSION = 17;
    static final short ENDPOINT_SYSTEMMESSAGE = 18;
    static final short ENDPOINT_MUSICCONTROL = 32;
    static final short ENDPOINT_PHONECONTROL = 33;
    static final short ENDPOINT_APPLICATIONMESSAGE = 48;
    static final short ENDPOINT_LAUNCHER = 49;
    static final short ENDPOINT_LOGS = 2000;
    static final short ENDPOINT_PING = 2001;
    static final short ENDPOINT_LOGDUMP = 2002;
    static final short ENDPOINT_RESET = 2003;
    static final short ENDPOINT_APP = 2004;
    static final short ENDPOINT_APPLOGS = 2006;
    static final short ENDPOINT_NOTIFICATION = 3000;
    static final short ENDPOINT_RESOURCE = 4000;
    static final short ENDPOINT_SYSREG = 5000;
    static final short ENDPOINT_FCTREG = 5001;
    static final short ENDPOINT_APPMANAGER = 6000;
    static final short ENDPOINT_RUNKEEPER = 7000;
    static final short ENDPOINT_SCREENSHOT = 8000;
    static final short ENDPOINT_PUTBYTES = (short) 48879;

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

    static final byte TIME_GETTIME = 0;
    static final byte TIME_SETTIME = 2;

    static final byte LENGTH_PREFIX = 4;
    static final byte LENGTH_SETTIME = 9;

    static byte[] encodeMessage(short endpoint, byte type, String[] parts) {
        // Calculate length first
        int length = LENGTH_PREFIX + 1;
        for (String s : parts) {
            length += (1 + s.getBytes().length);
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length - LENGTH_PREFIX));
        buf.putShort(endpoint);
        buf.put(type);

        // Encode Pascal-Style Strings
        for (String s : parts) {

            int partlength = s.getBytes().length;
            if (partlength > 255) partlength = 255;
            buf.put((byte) partlength);
            buf.put(s.getBytes(), 0, partlength);
        }

        return buf.array();
    }

    public static byte[] encodeSMS(String from, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        String tsstring = ts.toString();  // SIC
        String[] parts = {from, body, tsstring};

        return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_SMS, parts);
    }

    public static byte[] encodeEmail(String from, String subject, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        String tsstring = ts.toString(); // SIC
        String[] parts = {from, body, tsstring, subject};

        return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_EMAIL, parts);
    }

    public static byte[] encodeSetTime() {
        long ts = System.currentTimeMillis() / 1000;
        ts += SimpleTimeZone.getDefault().getOffset(ts) / 1000;
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_SETTIME);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (LENGTH_SETTIME - LENGTH_PREFIX));
        buf.putShort(ENDPOINT_TIME);
        buf.put(TIME_SETTIME);
        buf.putInt((int) ts);

        return buf.array();
    }

    public static byte[] encodeIncomingCall(String number, String name) {
        String cookie = "000"; // That's a dirty trick to make the cookie part 4 bytes long :P
        String[] parts = {cookie, number, name};
        return encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_INCOMINGCALL, parts);
    }

    // FIXME: that should return data into some unified struct/Class
    public static String decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        buf.order(ByteOrder.BIG_ENDIAN);
        short length = buf.getShort();
        short endpoint = buf.getShort();
        byte extra = 0;

        switch (endpoint) {
            case ENDPOINT_PHONECONTROL:
                extra = buf.get();
                break;
            default:
                break;
        }
        String ret = Short.toString(length) + "/" + Short.toString(endpoint) + "/" + Byte.toString(extra);

        return ret;
    }
}
