package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

import java.util.UUID;

/**
 * Message constants reverse-engineered by João Paulo Barraca, jpbarraca@gmail.com.
 *
 * @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
 */
public final class HPlusConstants {

    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("14702856-620a-3973-7c78-9cfff0876abd");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("14702853-620a-3973-7c78-9cfff0876abd");
    public static final UUID UUID_SERVICE_HP = UUID.fromString("14701820-620a-3973-7c78-9cfff0876abd");


    public static final byte PREF_VALUE_COUNTRY_CN = 1;
    public static final byte PREF_VALUE_COUNTRY_OTHER = 2;

    public static final byte PREF_VALUE_CLOCK_24H = 0;
    public static final byte PREF_VALUE_CLOCK_12H = 1;

    public static final byte PREF_VALUE_UNIT_METRIC = 0;
    public static final byte PREF_VALUE_UNIT_IMPERIAL = 1;

    public static final byte PREF_VALUE_GENDER_MALE = 0;
    public static final byte PREF_VALUE_GENDER_FEMALE = 1;

    public static final byte PREF_VALUE_HEARTRATE_MEASURE_ON = 11;
    public static final byte PREF_VALUE_HEARTRATE_MEASURE_OFF = 22;

    public static final byte PREF_VALUE_HEARTRATE_ALLDAY_ON = 10;
    public static final byte PREF_VALUE_HEARTRATE_ALLDAY_OFF = -1;

    public static final byte INCOMING_CALL_STATE_DISABLED_THRESHOLD = 0x7B;
    public static final byte INCOMING_CALL_STATE_ENABLED = (byte) 0xAA;

    public static final byte[] COMMAND_SET_PREF_START = new byte[]{0x4f, 0x5a};
    public static final byte[] COMMAND_SET_PREF_START1 = new byte[]{0x4d};
    public static final byte COMMAND_SET_PREF_COUNTRY = 0x22;
    public static final byte COMMAND_SET_PREF_TIMEMODE = 0x47;
    public static final byte COMMAND_SET_PREF_UNIT = 0x48;
    public static final byte COMMAND_SET_PREF_SEX = 0x2d;
    public static final byte COMMAND_SET_PREF_DATE = 0x08;
    public static final byte COMMAND_SET_PREF_TIME = 0x09;
    public static final byte COMMAND_SET_PREF_WEEK = 0x2a;
    public static final byte COMMAND_SET_PREF_SIT = 0x1e;
    public static final byte COMMAND_SET_PREF_WEIGHT = 0x05;
    public static final byte COMMAND_SET_PREF_HEIGHT = 0x04;
    public static final byte COMMAND_SET_PREF_AGE = 0x2c;
    public static final byte COMMAND_SET_PREF_GOAL = 0x26;
    public static final byte COMMAND_SET_PREF_SCREENTIME = 0x0b;
    public static final byte COMMAND_SET_PREF_BLOOD = 0x4e; //??
    public static final byte COMMAND_SET_PREF_FINDME = 0x0a;
    public static final byte COMMAND_SET_PREF_SAVE = 0x17;
    public static final byte COMMAND_SET_PREF_END = 0x4f;
    public static final byte COMMAND_SET_DISPLAY_ALERT = 0x23;
    public static final byte COMMAND_SET_PREF_ALLDAYHR = 53;
    public static final byte COMMAND_SET_INCOMING_CALL = 0x41;
    public static final byte COMMAND_SET_CONF_SAVE = 0x17;
    public static final byte COMMAND_SET_CONF_END = 0x4f;
    public static final byte COMMAND_SET_PREFS = 0x50;
    public static final byte COMMAND_SET_SIT_INTERVAL = 0x51;

    public static final byte[] COMMAND_FACTORY_RESET = new byte[] {-74, 90};


    //Actions to device
    public static final byte COMMAND_ACTION_INCOMING_SOCIAL = 0x31;
    public static final byte COMMAND_ACTION_INCOMING_SMS = 0x40;
    public static final byte COMMAND_ACTION_DISPLAY_TEXT = 0x43;
    public static final byte[] COMMAND_ACTION_INCOMING_CALL = new byte[] {6, -86};
    public static final byte COMMAND_ACTION_DISPLAY_TEXT_CENTER = 0x23;
    public static final byte COMMAND_ACTION_DISPLAY_TEXT_NAME = 0x3F;
    public static final byte COMMAND_ACTION_DISPLAY_TEXT_NAME_CN = 0x3E; //Text in GB2312?


    //Incoming Messages
    public static final byte DATA_STATS = 0x33;
    public static final byte DATA_STEPS = 0x36;
    public static final byte DATA_DAY_SUMMARY = 0x38;
    public static final byte DATA_DAY_SUMMARY_ALT = 0x39;
    public static final byte DATA_SLEEP = 0x1A;
    public static final byte DATA_INCOMING_CALL_STATE = 0x18;



    public static final String PREF_HPLUS_SCREENTIME = "hplus_screentime";
    public static final String PREF_HPLUS_ALLDAYHR = "hplus_alldayhr";
    public static final String PREF_HPLUS_UNIT = "hplus_unit";
    public static final String PREF_HPLUS_TIMEMODE = "hplus_timemode";
    public static final String PREF_HPLUS_WRIST = "hplus_wrist";
    public static final String PREF_HPLUS_SWALERT = "hplus_sw_alert";
    public static final String PREF_HPLUS_ALERT_TIME = "hplus_alert_time";
    public static final String PREF_HPLUS_SIT_START_TIME = "hplus_sit_start_time";
    public static final String PREF_HPLUS_SIT_END_TIME = "hplus_sit_end_time";
    public static final String PREF_HPLUS_COUNTRY = "hplus_country";

}
