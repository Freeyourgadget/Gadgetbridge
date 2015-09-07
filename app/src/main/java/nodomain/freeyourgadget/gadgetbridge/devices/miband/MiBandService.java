package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class MiBandService {


    public static final String MAC_ADDRESS_FILTER = "88:0F:10";

    public static final UUID UUID_SERVICE_MIBAND_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE0"));

    public static final UUID UUID_CHARACTERISTIC_DEVICE_INFO = UUID.fromString(String.format(BASE_UUID, "FF01"));

    public static final UUID UUID_CHARACTERISTIC_DEVICE_NAME = UUID.fromString(String.format(BASE_UUID, "FF02"));

    public static final UUID UUID_CHARACTERISTIC_NOTIFICATION = UUID.fromString(String.format(BASE_UUID, "FF03"));

    public static final UUID UUID_CHARACTERISTIC_USER_INFO = UUID.fromString(String.format(BASE_UUID, "FF04"));

    public static final UUID UUID_CHARACTERISTIC_CONTROL_POINT = UUID.fromString(String.format(BASE_UUID, "FF05"));

    public static final UUID UUID_CHARACTERISTIC_REALTIME_STEPS = UUID.fromString(String.format(BASE_UUID, "FF06"));

    public static final UUID UUID_CHARACTERISTIC_ACTIVITY_DATA = UUID.fromString(String.format(BASE_UUID, "FF07"));

    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_DATA = UUID.fromString(String.format(BASE_UUID, "FF08"));

    public static final UUID UUID_CHARACTERISTIC_LE_PARAMS = UUID.fromString(String.format(BASE_UUID, "FF09"));

    public static final UUID UUID_CHARACTERISTIC_DATE_TIME = UUID.fromString(String.format(BASE_UUID, "FF0A"));

    public static final UUID UUID_CHARACTERISTIC_STATISTICS = UUID.fromString(String.format(BASE_UUID, "FF0B"));

    public static final UUID UUID_CHARACTERISTIC_BATTERY = UUID.fromString(String.format(BASE_UUID, "FF0C"));

    public static final UUID UUID_CHARACTERISTIC_TEST = UUID.fromString(String.format(BASE_UUID, "FF0D"));

    public static final UUID UUID_CHARACTERISTIC_SENSOR_DATA = UUID.fromString(String.format(BASE_UUID, "FF0E"));

    public static final UUID UUID_CHARACTERISTIC_PAIR = UUID.fromString(String.format(BASE_UUID, "FF0F"));

    /* FURTHER UUIDS that were mixed with the other params below. The base UUID for these is unknown */

    public static final String UUID_CHARACTERISTIC_FEATURE = "2A9E";

    public static final String UUID_CHARACTERISTIC_MEASUREMENT = "2A9D";

    public static final String UUID_SERVICE_WEIGHT_SCALE_SERVICE = "181D";

    public static final String UUID_SERVICE_WEIGHT_SERVICE = "00001530-0000-3512-2118-0009af100700";

    public static final byte ALIAS_LEN = 0xa;

    /*NOTIFICATIONS: usually received on the UUID_CHARACTERISTIC_NOTIFICATION characteristic */

    public static final byte NOTIFY_NORMAL = 0x0;

    public static final byte NOTIFY_FIRMWARE_UPDATE_FAILED = 0x1;

    public static final byte NOTIFY_FIRMWARE_UPDATE_SUCCESS = 0x2;

    public static final byte NOTIFY_CONN_PARAM_UPDATE_FAILED = 0x3;

    public static final byte NOTIFY_CONN_PARAM_UPDATE_SUCCESS = 0x4;

    public static final byte NOTIFY_AUTHENTICATION_SUCCESS = 0x5;

    public static final byte NOTIFY_AUTHENTICATION_FAILED = 0x6;

    public static final byte NOTIFY_FITNESS_GOAL_ACHIEVED = 0x7;

    public static final byte NOTIFY_SET_LATENCY_SUCCESS = 0x8;

    public static final byte NOTIFY_RESET_AUTHENTICATION_FAILED = 0x9;

    public static final byte NOTIFY_RESET_AUTHENTICATION_SUCCESS = 0xa;

    public static final byte NOTIFY_FW_CHECK_FAILED = 0xb;

    public static final byte NOTIFY_FW_CHECK_SUCCESS = 0xc;

    public static final byte NOTIFY_STATUS_MOTOR_NOTIFY = 0xd;

    public static final byte NOTIFY_STATUS_MOTOR_CALL = 0xe;

    public static final byte NOTIFY_STATUS_MOTOR_DISCONNECT = 0xf;

    public static final byte NOTIFY_STATUS_MOTOR_SMART_ALARM = 0x10;

    public static final byte NOTIFY_STATUS_MOTOR_ALARM = 0x11;

    public static final byte NOTIFY_STATUS_MOTOR_GOAL = 0x12;

    public static final byte NOTIFY_STATUS_MOTOR_AUTH = 0x13;

    public static final byte NOTIFY_STATUS_MOTOR_SHUTDOWN = 0x14;

    public static final byte NOTIFY_STATUS_MOTOR_AUTH_SUCCESS = 0x15;

    public static final byte NOTIFY_STATUS_MOTOR_TEST = 0x16;

    // 0x18 is returned when we cancel data sync, perhaps is an ack for this message

    public static final byte NOTIFY_UNKNOWN = -0x1;

    public static final int NOTIFY_PAIR_CANCEL = 0xef;

    public static final int NOTIFY_DEVICE_MALFUNCTION = 0xff;


    /* MESSAGES: unknown */

    public static final byte MSG_CONNECTED = 0x0;

    public static final byte MSG_DISCONNECTED = 0x1;

    public static final byte MSG_CONNECTION_FAILED = 0x2;

    public static final byte MSG_INITIALIZATION_FAILED = 0x3;

    public static final byte MSG_INITIALIZATION_SUCCESS = 0x4;

    public static final byte MSG_STEPS_CHANGED = 0x5;

    public static final byte MSG_DEVICE_STATUS_CHANGED = 0x6;

    public static final byte MSG_BATTERY_STATUS_CHANGED = 0x7;

    /* COMMANDS: usually sent to UUID_CHARACTERISTIC_CONTROL_POINT characteristic */

    public static final byte COMMAND_SET_TIMER = 0x4;

    public static final byte COMMAND_SET_FITNESS_GOAL = 0x5;

    public static final byte COMMAND_FETCH_DATA = 0x6;

    public static final byte COMMAND_SEND_FIRMWARE_INFO = 0x7;

    public static final byte COMMAND_SEND_NOTIFICATION = 0x8;

    public static final byte COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE = 0xa;

    public static final byte COMMAND_SYNC = 0xb;

    public static final byte COMMAND_REBOOT = 0xc;

    public static final byte COMMAND_SET_WEAR_LOCATION = 0xf;

    public static final byte COMMAND_STOP_SYNC_DATA = 0x11;

    public static final byte COMMAND_STOP_MOTOR_VIBRATE = 0x13;

    /* FURTHER COMMANDS: unchecked therefore left commented

	public static final COMMAND_SET_REALTIME_STEPS_NOTIFICATION = 0x3t

	public static final byte COMMAND_FACTORY_RESET = 0x9t;

	public static final int COMMAND_SET_COLOR_THEME = et;

	public static final COMMAND_SET_REALTIME_STEP = 0x10t

	public static final byte COMMAND_GET_SENSOR_DATA = 0x12t

	*/

    /* CONNECTION: unknown

   	public static final CONNECTION_LATENCY_LEVEL_LOW = 0x0t;

	public static final CONNECTION_LATENCY_LEVEL_MEDIUM = 0x1t;

	public static final CONNECTION_LATENCY_LEVEL_HIGH = 0x2t;

    */

    /* MODES: unknown

	public static final MODE_REGULAR_DATA_LEN_BYTE = 0x0t;

	public static final MODE_REGULAR_DATA_LEN_MINITE = 0x1t
	*/

    /* PROFILE: unknown

	public static final PROFILE_STATE_UNKNOWN:I = 0x0

	public static final PROFILE_STATE_INITIALIZATION_SUCCESS:I = 0x1

	public static final PROFILE_STATE_INITIALIZATION_FAILED:I = 0x2

	public static final PROFILE_STATE_AUTHENTICATION_SUCCESS:I = 0x3

	public static final PROFILE_STATE_AUTHENTICATION_FAILED:I = 0x4

	*/

    /* TEST: unkown (maybe sent to UUID_CHARACTERISTIC_TEST characteristic?

	public static final TEST_DISCONNECTED_REMINDER = 0x5t

	public static final TEST_NOTIFICATION = 0x3t

	public static final TEST_REMOTE_DISCONNECT = 0x1t

	public static final TEST_SELFTEST = 0x2t

	*/

    private static Map<UUID, String> MIBAND_DEBUG;

    static {
        MIBAND_DEBUG = new HashMap<>();
        MIBAND_DEBUG.put(UUID_SERVICE_MIBAND_SERVICE, "MiBand Service");

        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DEVICE_INFO, "Device Info");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DEVICE_NAME, "Device Name");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_NOTIFICATION, "Notification");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_USER_INFO, "User Info");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_CONTROL_POINT, "Control Point");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_REALTIME_STEPS, "Realtime Steps");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_ACTIVITY_DATA, "Activity Data");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_FIRMWARE_DATA, "Firmware Data");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_LE_PARAMS, "LE Params");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_DATE_TIME, "Date/Time");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_STATISTICS, "Statistics");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_BATTERY, "Battery");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_TEST, "Test");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_SENSOR_DATA, "Sensor Data");
        MIBAND_DEBUG.put(UUID_CHARACTERISTIC_PAIR, "Pair");

        // extra:
        MIBAND_DEBUG.put(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"), "Generic Access Service");
        MIBAND_DEBUG.put(UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"), "Generic Attribute Service");
        MIBAND_DEBUG.put(UUID.fromString("00002a43-0000-1000-8000-00805f9b34fb"), "Alert Category ID");
        MIBAND_DEBUG.put(UUID.fromString("00002a42-0000-1000-8000-00805f9b34fb"), "Alert Category ID Bit Mask");
        MIBAND_DEBUG.put(UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb"), "Alert Level");
        MIBAND_DEBUG.put(UUID.fromString("00002a44-0000-1000-8000-00805f9b34fb"), "Alert Notification Control Point");
        MIBAND_DEBUG.put(UUID.fromString("00002a3f-0000-1000-8000-00805f9b34fb"), "Alert Status");
        MIBAND_DEBUG.put(UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"), "Appearance");
        MIBAND_DEBUG.put(UUID.fromString("00002a49-0000-1000-8000-00805f9b34fb"), "Blood Pressure Feature");
        MIBAND_DEBUG.put(UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb"), "Blood Pressure Measurement");
        MIBAND_DEBUG.put(UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb"), "Body Sensor Location");
        MIBAND_DEBUG.put(UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb"), "Current Time");
        MIBAND_DEBUG.put(UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb"), "Date Time");
        MIBAND_DEBUG.put(UUID.fromString("00002a0a-0000-1000-8000-00805f9b34fb"), "Day Date Time");
        MIBAND_DEBUG.put(UUID.fromString("00002a09-0000-1000-8000-00805f9b34fb"), "Day of Week");
        MIBAND_DEBUG.put(UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"), "Device Name");
        MIBAND_DEBUG.put(UUID.fromString("00002a0d-0000-1000-8000-00805f9b34fb"), "DST Offset");
        MIBAND_DEBUG.put(UUID.fromString("00002a0c-0000-1000-8000-00805f9b34fb"), "Exact Time 256");
        MIBAND_DEBUG.put(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"), "Firmware Revision String");
        MIBAND_DEBUG.put(UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb"), "Hardware Revision String");
        MIBAND_DEBUG.put(UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb"), "Heart Rate Control Point");
        MIBAND_DEBUG.put(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), "Heart Rate Measurement");
        MIBAND_DEBUG.put(UUID.fromString("00002a2a-0000-1000-8000-00805f9b34fb"), "IEEE 11073-20601 Regulatory");
        MIBAND_DEBUG.put(UUID.fromString("00002a36-0000-1000-8000-00805f9b34fb"), "Intermediate Cuff Pressure");
        MIBAND_DEBUG.put(UUID.fromString("00002a1e-0000-1000-8000-00805f9b34fb"), "Intermediate Temperature");
        MIBAND_DEBUG.put(UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb"), "Local Time Information");
        MIBAND_DEBUG.put(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), "Manufacturer Name String");
        MIBAND_DEBUG.put(UUID.fromString("00002a21-0000-1000-8000-00805f9b34fb"), "Measurement Interval");
        MIBAND_DEBUG.put(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"), "Model Number String");
        MIBAND_DEBUG.put(UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb"), "New Alert");
        MIBAND_DEBUG.put(UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"), "Peripheral Preferred Connection Parameters");
        MIBAND_DEBUG.put(UUID.fromString("00002a02-0000-1000-8000-00805f9b34fb"), "Peripheral Privacy Flag");
        MIBAND_DEBUG.put(UUID.fromString("00002a03-0000-1000-8000-00805f9b34fb"), "Reconnection Address");
        MIBAND_DEBUG.put(UUID.fromString("00002a14-0000-1000-8000-00805f9b34fb"), "Reference Time Information");
        MIBAND_DEBUG.put(UUID.fromString("00002a40-0000-1000-8000-00805f9b34fb"), "Ringer Control Point");
        MIBAND_DEBUG.put(UUID.fromString("00002a41-0000-1000-8000-00805f9b34fb"), "Ringer Setting");
        MIBAND_DEBUG.put(UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"), "Serial Number String");
        MIBAND_DEBUG.put(UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb"), "Service Changed");
        MIBAND_DEBUG.put(UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"), "Software Revision String");
        MIBAND_DEBUG.put(UUID.fromString("00002a47-0000-1000-8000-00805f9b34fb"), "Supported New Alert Category");
        MIBAND_DEBUG.put(UUID.fromString("00002a48-0000-1000-8000-00805f9b34fb"), "Supported Unread Alert Category");
        MIBAND_DEBUG.put(UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb"), "System ID");
        MIBAND_DEBUG.put(UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb"), "Temperature Measurement");
        MIBAND_DEBUG.put(UUID.fromString("00002a1d-0000-1000-8000-00805f9b34fb"), "Temperature DeviceType");
        MIBAND_DEBUG.put(UUID.fromString("00002a12-0000-1000-8000-00805f9b34fb"), "Time Accuracy");
        MIBAND_DEBUG.put(UUID.fromString("00002a13-0000-1000-8000-00805f9b34fb"), "Time Source");
        MIBAND_DEBUG.put(UUID.fromString("00002a16-0000-1000-8000-00805f9b34fb"), "Time Update Control Point");
        MIBAND_DEBUG.put(UUID.fromString("00002a17-0000-1000-8000-00805f9b34fb"), "Time Update State");
        MIBAND_DEBUG.put(UUID.fromString("00002a11-0000-1000-8000-00805f9b34fb"), "Time with DST");
        MIBAND_DEBUG.put(UUID.fromString("00002a0e-0000-1000-8000-00805f9b34fb"), "Time Zone");
        MIBAND_DEBUG.put(UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb"), "Tx Power Level");
        MIBAND_DEBUG.put(UUID.fromString("00002a45-0000-1000-8000-00805f9b34fb"), "Unread Alert Status");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = MIBAND_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }
}
