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


    public static final byte COUNTRY_CN = 1;
    public static final byte COUNTRY_OTHER = 2;

    public static final byte CLOCK_24H = 0;
    public static final byte CLOCK_12H = 1;

    public static final byte UNIT_METRIC = 0;
    public static final byte UNIT_IMPERIAL = 1;

    public static final byte SEX_MALE = 0;
    public static final byte SEX_FEMALE = 1;

    public static final byte HEARTRATE_MEASURE_ON = 11;
    public static final byte HEARTRATE_MEASURE_OFF = 22;

    public static final byte HEARTRATE_ALLDAY_ON = 10;
    public static final byte HEARTRATE_ALLDAY_OFF = -1;

    public static final byte[] COMMAND_SET_INIT1 = new byte[]{0x50,0x00,0x25,(byte) 0xb1,0x4a,0x00,0x00,0x27,0x10,0x05,0x02,0x00,(byte) 0xff,0x0a,(byte) 0xff,0x00,(byte) 0xff,(byte) 0xff,0x00,0x01};
    public static final byte[] COMMAND_SET_INIT2 = new byte[]{0x51,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x07,(byte) 0xe0,0x0c,0x12,0x16,0x0a,0x10,0x00,0x00,0x00,0x00};

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
    public static final byte COMMAND_SET_INCOMMING_SOCIAL = 0x31;
    public static final byte COMMAND_SET_INCOMMING_SMS = 0x40;
    public static final byte COMMAND_SET_DISPLAY_TEXT = 0x43;
    public static final byte COMMAND_SET_DISPLAY_ALERT = 0x23;
    public static final byte COMMAND_SET_PREF_ALLDAYHR = 53;

    public static final byte COMMAND_SET_INCOMMING_CALL = 65;
    public static final byte[] COMMAND_FACTORY_RESET = new byte[] {-74, 90};

    public static final byte COMMAND_SET_CONF_SAVE = 0x17;
    public static final byte COMMAND_SET_CONF_END = 0x4f;


    public static final byte DATA_STATS = 0x33;
    public static final byte DATA_SLEEP = 0x1A;


    public static final String PREF_HPLUS_USER_ALIAS = "hplus_user_alias";
    public static final String PREF_HPLUS_FITNESS_GOAL = "hplus_fitness_goal";
    public static final String PREF_HPLUS_SCREENTIME = "hplus_screentime";
    public static final String PREF_HPLUS_ALLDAYHR = "hplus_alldayhr";
    public static final String PREF_HPLUS_UNIT = "hplus_unit";
    public static final String PREF_HPLUS_TIMEMODE = "hplus_timemode";
}
