package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class MiBand2Service {


    public static final UUID UUID_SERVICE_MIBAND_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE0"));
    public static final UUID UUID_SERVICE_MIBAND2_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE1"));
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString(String.format(BASE_UUID, "180D"));
    public static final UUID UUID_SERVICE_WEIGHT_SERVICE = UUID.fromString("00001530-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC0 = UUID.fromString("00000000-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC1 = UUID.fromString("00000001-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC2 = UUID.fromString("00000002-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC3 = UUID.fromString("00000003-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC4 = UUID.fromString("00000004-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC5 = UUID.fromString("00000005-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC6 = UUID.fromString("00000006-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC7 = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC8 = UUID.fromString("00000008-0000-3512-2118-0009af100700");
    // service uuid fee1
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC9 = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC10 = UUID.fromString("00000010-0000-3512-2118-0009af100700");

    // set metric distance
    // set 12 hour time mode


//    public static final UUID UUID_CHARACTERISTIC_DEVICE_INFO = UUID.fromString(String.format(BASE_UUID, "FF01"));
//
//    public static final UUID UUID_CHARACTERISTIC_DEVICE_NAME = UUID.fromString(String.format(BASE_UUID, "FF02"));
//
//    public static final UUID UUID_CHARACTERISTIC_NOTIFICATION = UUID.fromString(String.format(BASE_UUID, "FF03"));
//
//    public static final UUID UUID_CHARACTERISTIC_USER_INFO = UUID.fromString(String.format(BASE_UUID, "FF04"));
//
//    public static final UUID UUID_CHARACTERISTIC_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "FF05"));
//
//    public static final UUID UUID_CHARACTERISTIC_REALTIME_STEPS = UUID.fromString(String.format(BASE_UUID, "FF06"));
//
//    public static final UUID UUID_CHARACTERISTIC_ACTIVITY_DATA = UUID.fromString(String.format(BASE_UUID, "FF07"));
//
//    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_DATA = UUID.fromString(String.format(BASE_UUID, "FF08"));
//
//    public static final UUID UUID_CHARACTERISTIC_LE_PARAMS = UUID.fromString(String.format(BASE_UUID, "FF09"));
//
//    public static final UUID UUID_CHARACTERISTIC_DATE_TIME = UUID.fromString(String.format(BASE_UUID, "FF0A"));
//
//    public static final UUID UUID_CHARACTERISTIC_STATISTICS = UUID.fromString(String.format(BASE_UUID, "FF0B"));
//
//    public static final UUID UUID_CHARACTERISTIC_BATTERY = UUID.fromString(String.format(BASE_UUID, "FF0C"));
//
//    public static final UUID UUID_CHARACTERISTIC_TEST = UUID.fromString(String.format(BASE_UUID, "FF0D"));
//
//    public static final UUID UUID_CHARACTERISTIC_SENSOR_DATA = UUID.fromString(String.format(BASE_UUID, "FF0E"));
//
//    public static final UUID UUID_CHARACTERISTIC_PAIR = UUID.fromString(String.format(BASE_UUID, "FF0F"));
//
//    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "2A39"));
//    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString(String.format(BASE_UUID, "2A37"));
//
//
//
//    /* FURTHER UUIDS that were mixed with the other params below. The base UUID for these is unknown */
//
//    public static final byte ALIAS_LEN = 0xa;
//
//    /*NOTIFICATIONS: usually received on the UUID_CHARACTERISTIC_NOTIFICATION characteristic */
//
//    public static final byte NOTIFY_NORMAL = 0x0;
//
//    public static final byte NOTIFY_FIRMWARE_UPDATE_FAILED = 0x1;
//
//    public static final byte NOTIFY_FIRMWARE_UPDATE_SUCCESS = 0x2;
//
//    public static final byte NOTIFY_CONN_PARAM_UPDATE_FAILED = 0x3;
//
//    public static final byte NOTIFY_CONN_PARAM_UPDATE_SUCCESS = 0x4;
//
//    public static final byte NOTIFY_AUTHENTICATION_SUCCESS = 0x5;
//
//    public static final byte NOTIFY_AUTHENTICATION_FAILED = 0x6;
//
//    public static final byte NOTIFY_FITNESS_GOAL_ACHIEVED = 0x7;
//
//    public static final byte NOTIFY_SET_LATENCY_SUCCESS = 0x8;
//
//    public static final byte NOTIFY_RESET_AUTHENTICATION_FAILED = 0x9;
//
//    public static final byte NOTIFY_RESET_AUTHENTICATION_SUCCESS = 0xa;
//
//    public static final byte NOTIFY_FW_CHECK_FAILED = 0xb;
//
//    public static final byte NOTIFY_FW_CHECK_SUCCESS = 0xc;
//
//    public static final byte NOTIFY_STATUS_MOTOR_NOTIFY = 0xd;
//
//    public static final byte NOTIFY_STATUS_MOTOR_CALL = 0xe;
//
//    public static final byte NOTIFY_STATUS_MOTOR_DISCONNECT = 0xf;
//
//    public static final byte NOTIFY_STATUS_MOTOR_SMART_ALARM = 0x10;
//
//    public static final byte NOTIFY_STATUS_MOTOR_ALARM = 0x11;
//
//    public static final byte NOTIFY_STATUS_MOTOR_GOAL = 0x12;
//
//    public static final byte NOTIFY_STATUS_MOTOR_AUTH = 0x13;
//
//    public static final byte NOTIFY_STATUS_MOTOR_SHUTDOWN = 0x14;
//
//    public static final byte NOTIFY_STATUS_MOTOR_AUTH_SUCCESS = 0x15;
//
//    public static final byte NOTIFY_STATUS_MOTOR_TEST = 0x16;
//
//    // 0x18 is returned when we cancel data sync, perhaps is an ack for this message
//
//    public static final byte NOTIFY_UNKNOWN = -0x1;
//
//    public static final int NOTIFY_PAIR_CANCEL = 0xef;
//
//    public static final int NOTIFY_DEVICE_MALFUNCTION = 0xff;
//
//
//    /* MESSAGES: unknown */
//
//    public static final byte MSG_CONNECTED = 0x0;
//
//    public static final byte MSG_DISCONNECTED = 0x1;
//
//    public static final byte MSG_CONNECTION_FAILED = 0x2;
//
//    public static final byte MSG_INITIALIZATION_FAILED = 0x3;
//
//    public static final byte MSG_INITIALIZATION_SUCCESS = 0x4;
//
//    public static final byte MSG_STEPS_CHANGED = 0x5;
//
//    public static final byte MSG_DEVICE_STATUS_CHANGED = 0x6;
//
//    public static final byte MSG_BATTERY_STATUS_CHANGED = 0x7;
//
//    /* COMMANDS: usually sent to UUID_CHARACTERISTIC_CONTROL_POINT characteristic */
//
//    public static final byte COMMAND_SET_TIMER = 0x4;
//
//    public static final byte COMMAND_SET_FITNESS_GOAL = 0x5;
//
//    public static final byte COMMAND_FETCH_DATA = 0x6;
//
//    public static final byte COMMAND_SEND_FIRMWARE_INFO = 0x7;
//
//    public static final byte COMMAND_SEND_NOTIFICATION = 0x8;
//
//    public static final byte COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE = 0xa;
//
//    public static final byte COMMAND_SYNC = 0xb;
//
//    public static final byte COMMAND_REBOOT = 0xc;
//
//    public static final byte COMMAND_SET_WEAR_LOCATION = 0xf;
//
//    public static final byte COMMAND_STOP_SYNC_DATA = 0x11;
//
//    public static final byte COMMAND_STOP_MOTOR_VIBRATE = 0x13;
//
//    public static final byte COMMAND_SET_REALTIME_STEPS_NOTIFICATION = 0x3;
//
//    public static final byte COMMAND_SET_REALTIME_STEP = 0x10;
//
//    // Test HR
//    public static final byte COMMAND_SET_HR_SLEEP = 0x0;
//    public static final byte COMMAND_SET__HR_CONTINUOUS = 0x1;
//    public static final byte COMMAND_SET_HR_MANUAL = 0x2;
//
//
//    /* FURTHER COMMANDS: unchecked therefore left commented
//
//
//	public static final byte COMMAND_FACTORY_RESET = 0x9t;
//
//	public static final int COMMAND_SET_COLOR_THEME = et;
//
//	public static final byte COMMAND_GET_SENSOR_DATA = 0x12t
//
//	*/
//
//    /* CONNECTION: unknown
//
//   	public static final CONNECTION_LATENCY_LEVEL_LOW = 0x0t;
//
//	public static final CONNECTION_LATENCY_LEVEL_MEDIUM = 0x1t;
//
//	public static final CONNECTION_LATENCY_LEVEL_HIGH = 0x2t;
//
//    */
//
//    /* MODES: probably related to the sample data structure
//    */
//
//    public static final byte MODE_REGULAR_DATA_LEN_BYTE = 0x0;
//
//    // was MODE_REGULAR_DATA_LEN_MINITE
//    public static final byte MODE_REGULAR_DATA_LEN_MINUTE = 0x1;
//
//    /* PROFILE: unknown
//
//	public static final PROFILE_STATE_UNKNOWN:I = 0x0
//
//	public static final PROFILE_STATE_INITIALIZATION_SUCCESS:I = 0x1
//
//	public static final PROFILE_STATE_INITIALIZATION_FAILED:I = 0x2
//
//	public static final PROFILE_STATE_AUTHENTICATION_SUCCESS:I = 0x3
//
//	public static final PROFILE_STATE_AUTHENTICATION_FAILED:I = 0x4
//
//	*/
//
//    // TEST_*: sent to UUID_CHARACTERISTIC_TEST characteristic
//
//	public static final byte TEST_DISCONNECTED_REMINDER = 0x5;
//
//	public static final byte TEST_NOTIFICATION = 0x3;
//
//	public static final byte TEST_REMOTE_DISCONNECT = 0x1;
//
//	public static final byte TEST_SELFTEST = 0x2;

    private static final Map<UUID, String> MIBAND_DEBUG;

    static {
        MIBAND_DEBUG = new HashMap<>();
        MIBAND_DEBUG.put(UUID_SERVICE_MIBAND_SERVICE, "MiBand Service");
        MIBAND_DEBUG.put(UUID_SERVICE_HEART_RATE, "MiBand HR Service");

//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DEVICE_INFO, "Device Info");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DEVICE_NAME, "Device Name");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_NOTIFICATION, "Notification");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_USER_INFO, "User Info");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_CONTROL_POINT, "Control Point");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_REALTIME_STEPS, "Realtime Steps");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_ACTIVITY_DATA, "Activity Data");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_FIRMWARE_DATA, "Firmware Data");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_LE_PARAMS, "LE Params");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DATE_TIME, "Date/Time");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_STATISTICS, "Statistics");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_BATTERY, "Battery");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_TEST, "Test");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_SENSOR_DATA, "Sensor Data");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_PAIR, "Pair");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, "Heart Rate Control Point");
//        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, "Heart Rate Measure");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = MIBAND_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }
}
