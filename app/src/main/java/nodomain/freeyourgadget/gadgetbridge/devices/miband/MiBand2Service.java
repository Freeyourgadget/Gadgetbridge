package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public class MiBand2Service {


    public static final UUID UUID_SERVICE_MIBAND_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE0"));
    public static final UUID UUID_SERVICE_MIBAND2_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE1"));
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString(String.format(BASE_UUID, "180D"));
    public static final UUID UUID_SERVICE_FIRMWARE_SERVICE = UUID.fromString("00001530-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_FIRMWARE = UUID.fromString("00001531-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_DATA = UUID.fromString("00001532-0000-3512-2118-0009af100700");

    public static final UUID UUID_UNKNOWN_CHARACTERISTIC0 = UUID.fromString("00000000-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC1 = UUID.fromString("00000001-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC2 = UUID.fromString("00000002-0000-3512-2118-0009af100700");
    /**
     * Alarms, Display and other configuration.
     */
    public static final UUID UUID_CHARACTERISTIC_3_CONFIGURATION = UUID.fromString("00000003-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC4 = UUID.fromString("00000004-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_5_ACTIVITY_DATA = UUID.fromString("00000005-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_6_BATTERY_INFO = UUID.fromString("00000006-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC7 = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC8 = UUID.fromString("00000008-0000-3512-2118-0009af100700");
    // service uuid fee1
    public static final UUID UUID_CHARACTERISTIC_AUTH = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_10_BUTTON = UUID.fromString("00000010-0000-3512-2118-0009af100700");

    public static final int ALERT_LEVEL_NONE = 0;
    public static final int ALERT_LEVEL_MESSAGE = 1;
    public static final int ALERT_LEVEL_PHONE_CALL = 2;
    public static final int ALERT_LEVEL_VIBRATE_ONLY = 3;

    // set metric distance
    // set 12 hour time mode



    private static final Map<UUID, String> MIBAND_DEBUG;

    /**
     * Mi Band 2 authentication has three steps.
     * This is step 1: sending a "secret" key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_KEY = 0x01;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 2: requesting a random authentication key from the band.
     * This is byte 0, followed by {@link #AUTH_BYTE}.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_REQUEST_RANDOM_AUTH_NUMBER = 0x02;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 3: sending the encrypted random authentication key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the encrypted random authentication key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_ENCRYPTED_AUTH_NUMBER = 0x03;

    /**
     * Received in response to any authentication requests (byte 0 in the byte[] value.
     */
    public static final byte AUTH_RESPONSE = 0x10;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x01 means success.
     */
    public static final byte AUTH_SUCCESS = 0x01;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x04 means failure.
     */
    public static final byte AUTH_FAIL = 0x04;
    /**
     * In some logs it's 0x0...
     */
    public static final byte AUTH_BYTE = 0x8;

    // maybe not really activity data, but steps?
    public static final byte COMMAND_FETCH_ACTIVITY_DATA = 0x02;
    public static final byte COMMAND_XXXX_ACTIVITY_DATA = 0x03; // maybe delete/drop activity data?

    public static final byte[] COMMAND_SET_FITNESS_GOAL_START = new byte[] { 0x10, 0x0, 0x0 };
    public static final byte[] COMMAND_SET_FITNESS_GOAL_END = new byte[] { 0, 0 };


    public static byte COMMAND_DATEFORMAT = 0x06;

    public static final byte[] DATEFORMAT_DATE_TIME = new byte[] { COMMAND_DATEFORMAT, 0x0a, 0x0, 0x03 };
    public static final byte[] DATEFORMAT_TIME = new byte[] { COMMAND_DATEFORMAT, 0x0a, 0x0, 0x0 };

    public static final byte RESPONSE = 0x10;

    public static final byte SUCCESS = 0x01;
    public static final byte COMMAND_ACTIVITY_DATA_START_DATE = 0x01;
    public static final byte COMMAND_ACTIVITY_DATA_XXX_DATE = 0x02; // issued on first connect, followd by COMMAND_XXXX_ACTIVITY_DATA instead of COMMAND_FETCH_ACTIVITY_DATA

    public static final byte COMMAND_FIRMWARE_INIT = 0x01; // to UUID_CHARACTERISTIC_FIRMWARE, followed by fw file size in bytes
    public static final byte COMMAND_FIRMWARE_START_DATA = 0x03; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_UPDATE_SYNC = 0x00; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_CHECKSUM = 0x04; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_APPLY_REBOOT = 0x05; // or is it REBOOT? to UUID_CHARACTERISTIC_FIRMWARE

    public static final byte[] RESPONSE_FINISH_SUCCESS = new byte[] {RESPONSE, 2, SUCCESS };
    public static final byte[] RESPONSE_FIRMWARE_DATA_SUCCESS = new byte[] {RESPONSE, COMMAND_FIRMWARE_START_DATA, SUCCESS };
    /**
     * Received in response to any dateformat configuration request (byte 0 in the byte[] value.
     */
    public static final byte[] RESPONSE_DATEFORMAT_SUCCESS = new byte[] { RESPONSE, COMMAND_DATEFORMAT, 0x0a, 0x0, 0x01 };
    public static final byte[] RESPONSE_ACTIVITY_DATA_START_DATE_SUCCESS = new byte[] { RESPONSE, COMMAND_ACTIVITY_DATA_START_DATE, SUCCESS};

    public static final byte[] WEAR_LOCATION_LEFT_WRIST = new byte[] { 0x20, 0x00, 0x00, 0x02 };
    public static final byte[] WEAR_LOCATION_RIGHT_WRIST = new byte[] { 0x20, 0x00, 0x00, (byte) 0x82};

    public static final byte[] COMMAND_ENABLE_HR_SLEEP_MEASUREMENT = new byte[]{0x15, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_HR_SLEEP_MEASUREMENT = new byte[]{0x15, 0x00, 0x00};

    public static final byte[] COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{0x06, 0x05, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{0x06, 0x05, 0x00, 0x00};

    public static final byte[] COMMAND_TEXT_NOTIFICATION = new byte[] {0x05, 0x01};
    public static final byte[] COMMAND_TEXT_NOTIFICATION_CONTINUATION = new byte[] {(byte) 0xfa, 0x01, 0x00};

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
