package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HPlusConstants {

    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("14702856-620a-3973-7c78-9cfff0876abd");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("14702853-620a-3973-7c78-9cfff0876abd");
    public static final UUID UUID_SERVICE_HP = UUID.fromString("14701820-620a-3973-7c78-9cfff0876abd");

    public static final byte ARG_WRIST_LEFT = 0; //Guess...
    public static final byte ARG_WRIST_RIGHT = 1; //Guess...

    public static final byte ARG_LANGUAGE_CN = 1;
    public static final byte ARG_LANGUAGE_EN = 2;

    public static final byte ARG_TIMEMODE_24H = 1;
    public static final byte ARG_TIMEMODE_12H = 0;

    public static final byte ARG_UNIT_METRIC = 0;
    public static final byte ARG_UNIT_IMPERIAL = 1;

    public static final byte ARG_GENDER_MALE = 0;
    public static final byte ARG_GENDER_FEMALE = 1;

    public static final byte ARG_HEARTRATE_MEASURE_ON = 11;
    public static final byte ARG_HEARTRATE_MEASURE_OFF = 22;

    public static final byte ARG_HEARTRATE_ALLDAY_ON = 0x0A;
    public static final byte ARG_HEARTRATE_ALLDAY_OFF = (byte) 0xff;

    public static final byte INCOMING_CALL_STATE_DISABLED_THRESHOLD = 0x7B;
    public static final byte INCOMING_CALL_STATE_ENABLED = (byte) 0xAA;

    public static final byte ARG_ALARM_DISABLE = (byte) -1;

    public static final byte[] CMD_SET_PREF_START = new byte[]{0x4f, 0x5a};
    public static final byte[] CMD_SET_PREF_START1 = new byte[]{0x4d};
    //public static final byte CMD_SET_ALARM = 0x4c; Unknown
    public static final byte CMD_SET_ALARM = 0x0c;
    public static final byte CMD_SET_LANGUAGE = 0x22;
    public static final byte CMD_SET_TIMEMODE = 0x47;
    public static final byte CMD_SET_UNITS = 0x48;
    public static final byte CMD_SET_GENDER = 0x2d;
    public static final byte CMD_SET_DATE = 0x08;
    public static final byte CMD_SET_TIME = 0x09;
    public static final byte CMD_SET_WEEK = 0x2a;
    public static final byte CMD_SET_PREF_SIT = 0x1e;
    public static final byte CMD_SET_WEIGHT = 0x05;
    public static final byte CMD_HEIGHT = 0x04;
    public static final byte CMD_SET_AGE = 0x2c;
    public static final byte CMD_SET_GOAL = 0x26;
    public static final byte CMD_SET_SCREENTIME = 0x0b;
    public static final byte CMD_SET_BLOOD = 0x4e; //??

    public static final byte CMD_SET_FINDME = 0x0a;
    public static final byte ARG_FINDME_ON = 0x01;
    public static final byte ARG_FINDME_OFF = 0x02;

    public static final byte CMD_GET_VERSION = 0x17;
    public static final byte CMD_SET_END = 0x4f;
    public static final byte CMD_SET_INCOMING_CALL_NUMBER = 0x23;
    public static final byte CMD_SET_ALLDAY_HRM = 0x35;
    public static final byte CMD_ACTION_INCOMING_CALL = 0x41;
    public static final byte CMD_SET_CONF_END = 0x4f;
    public static final byte CMD_SET_PREFS = 0x50;
    public static final byte CMD_SET_SIT_INTERVAL = 0x51;
    public static final byte CMD_SET_HEARTRATE_STATE = 0x32;

    //Actions to device
    public static final byte CMD_GET_ACTIVE_DAY = 0x27;
    public static final byte CMD_GET_DAY_DATA = 0x15;
    public static final byte CMD_GET_SLEEP = 0x19;
    public static final byte CMD_GET_CURR_DATA = 0x16;
    public static final byte CMD_GET_DEVICE_ID = 0x24;

    public static final byte CMD_ACTION_INCOMING_SOCIAL = 0x31;
    //public static final byte COMMAND_ACTION_INCOMING_SMS = 0x40; //Unknown
    public static final byte CMD_ACTION_DISPLAY_TEXT = 0x43;

    public static final byte CMD_ACTION_DISPLAY_TEXT_NAME = 0x3F;
    public static final byte CMD_ACTION_DISPLAY_TEXT_NAME_CN = 0x3E; //Text in GB2312?
    public static final byte[] CMD_ACTION_HELLO = new byte[]{0x01, 0x00};
    public static final byte CMD_SHUTDOWN = 0x5B;
    public static final byte ARG_SHUTDOWN_EN = 0x5A;

    public static final byte CMD_FACTORY_RESET = -74;
    public static final byte ARG_FACTORY_RESET_EN = 0x5A;

    public static final byte CMD_SET_INCOMING_MESSAGE = 0x07;
    public static final byte CMD_SET_INCOMING_CALL = 0x06;
    public static final byte ARG_INCOMING_CALL = (byte) -86;
    public static final byte ARG_INCOMING_MESSAGE = (byte) -86;

    //Incoming Messages
    public static final byte DATA_STATS = 0x33;
    public static final byte DATA_STEPS = 0x36;
    public static final byte DATA_DAY_SUMMARY = 0x38;
    public static final byte DATA_DAY_SUMMARY_ALT = 0x39;
    public static final byte DATA_SLEEP = 0x1A;
    public static final byte DATA_VERSION = 0x18;

    public static final String PREF_HPLUS_SCREENTIME = "hplus_screentime";
    public static final String PREF_HPLUS_ALLDAYHR = "hplus_alldayhr";
    public static final String PREF_HPLUS_UNIT = "hplus_unit";
    public static final String PREF_HPLUS_TIMEFORMAT = "hplus_timeformat";
    public static final String PREF_HPLUS_WRIST = "hplus_wrist";
    public static final String PREF_HPLUS_SIT_START_TIME = "hplus_sit_start_time";
    public static final String PREF_HPLUS_SIT_END_TIME = "hplus_sit_end_time";

    public static final Map<Character, Byte> transliterateMap = new HashMap<Character, Byte>(){
        {
            //These are missing
            put('ó', new Byte((byte) 111));
            put('Ó', new Byte((byte) 79));
            put('í', new Byte((byte) 105));
            put('Í', new Byte((byte) 73));
            put('ú', new Byte((byte) 117));
            put('Ú', new Byte((byte) 85));

            //These mostly belong to the extended ASCII table
            put('Ç', new Byte((byte) 128));
            put('ü', new Byte((byte) 129));
            put('é', new Byte((byte) 130));
            put('â', new Byte((byte) 131));
            put('ä', new Byte((byte) 132));
            put('à', new Byte((byte) 133));
            put('ã', new Byte((byte) 134));
            put('ç', new Byte((byte) 135));
            put('ê', new Byte((byte) 136));
            put('ë', new Byte((byte) 137));
            put('è', new Byte((byte) 138));
            put('Ï', new Byte((byte) 139));
            put('Î', new Byte((byte) 140));
            put('Ì', new Byte((byte) 141));
            put('Ã', new Byte((byte) 142));
            put('Ä', new Byte((byte) 143));
            put('É', new Byte((byte) 144));
            put('æ', new Byte((byte) 145));
            put('Æ', new Byte((byte) 146));
            put('ô', new Byte((byte) 147));
            put('ö', new Byte((byte) 148));
            put('ò', new Byte((byte) 149));
            put('û', new Byte((byte) 150));
            put('ù', new Byte((byte) 151));
            put('ÿ', new Byte((byte) 152));
            put('Ö', new Byte((byte) 153));
            put('Ü', new Byte((byte) 154));
            put('¢', new Byte((byte) 155));
            put('£', new Byte((byte) 156));
            put('¥', new Byte((byte) 157));
            put('ƒ', new Byte((byte) 159));
            put('á', new Byte((byte) 160));
            put('ñ', new Byte((byte) 164));
            put('Ñ', new Byte((byte) 165));
            put('ª', new Byte((byte) 166));
            put('º', new Byte((byte) 167));
            put('¿', new Byte((byte) 168));
            put('¬', new Byte((byte) 170));
            put('½', new Byte((byte) 171));
            put('¼', new Byte((byte) 172));
            put('¡', new Byte((byte) 173));
            put('«', new Byte((byte) 174));
            put('»', new Byte((byte) 175));
        }
    };
}
