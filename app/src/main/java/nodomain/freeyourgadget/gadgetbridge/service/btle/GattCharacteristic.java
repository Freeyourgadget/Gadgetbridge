package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GattCharacteristic {

    //part of the generic BLE specs see https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
    //the list is complete as of 2015-09-28
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7E")));
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A84")));
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_THRESHOLD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7F")));
    public static final UUID UUID_CHARACTERISTIC_AGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A80")));
    public static final UUID UUID_CHARACTERISTIC_AGGREGATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5A")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_CATEGORY_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A43")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_CATEGORY_ID_BIT_MASK = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A42")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A06")));

    public static final byte NO_ALERT = 0x0;
    public static final byte MILD_ALERT = 0x1;
    public static final byte HIGH_ALERT = 0x2;

    public static final UUID UUID_CHARACTERISTIC_ALERT_NOTIFICATION_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A44")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A3F")));
    public static final UUID UUID_CHARACTERISTIC_ALTITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB3")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A81")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A82")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_THRESHOLD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A83")));
    public static final UUID UUID_CHARACTERISTIC_ANALOG = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A58")));
    public static final UUID UUID_CHARACTERISTIC_APPARENT_WIND_DIRECTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A73")));
    public static final UUID UUID_CHARACTERISTIC_APPARENT_WIND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A72")));
    public static final UUID UUID_CHARACTERISTIC_GAP_APPEARANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A01")));
    public static final UUID UUID_CHARACTERISTIC_BAROMETRIC_PRESSURE_TREND = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA3")));
    public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A19")));
    public static final UUID UUID_CHARACTERISTIC_BLOOD_PRESSURE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A49")));
    public static final UUID UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A35")));
    public static final UUID UUID_CHARACTERISTIC_BODY_COMPOSITION_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9B")));
    public static final UUID UUID_CHARACTERISTIC_BODY_COMPOSITION_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9C")));
    public static final UUID UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A38")));
    public static final UUID UUID_CHARACTERISTIC_BOND_MANAGEMENT_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA4")));
    public static final UUID UUID_CHARACTERISTIC_BOND_MANAGEMENT_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA5")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_KEYBOARD_INPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A22")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_KEYBOARD_OUTPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A32")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_MOUSE_INPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A33")));
    public static final UUID UUID_CHARACTERISTIC_GAP_CENTRAL_ADDRESS_RESOLUTION_SUPPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA6")));
    public static final UUID UUID_CHARACTERISTIC_CGM_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA8")));
    public static final UUID UUID_CHARACTERISTIC_CGM_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA7")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SESSION_RUN_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAB")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SESSION_START_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAA")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SPECIFIC_OPS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAC")));
    public static final UUID UUID_CHARACTERISTIC_CGM_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA9")));
    public static final UUID UUID_CHARACTERISTIC_CSC_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5C")));
    public static final UUID UUID_CHARACTERISTIC_CSC_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5B")));
    public static final UUID UUID_CHARACTERISTIC_CURRENT_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A2B")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A66")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A65")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A63")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_VECTOR = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A64")));
    public static final UUID UUID_CHARACTERISTIC_DATABASE_CHANGE_INCREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A99")));
    public static final UUID UUID_CHARACTERISTIC_DATE_OF_BIRTH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A85")));
    public static final UUID UUID_CHARACTERISTIC_DATE_OF_THRESHOLD_ASSESSMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A86")));
    public static final UUID UUID_CHARACTERISTIC_DATE_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A08")));
    public static final UUID UUID_CHARACTERISTIC_DAY_DATE_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0A")));
    public static final UUID UUID_CHARACTERISTIC_DAY_OF_WEEK = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A09")));
    public static final UUID UUID_CHARACTERISTIC_DESCRIPTOR_VALUE_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7D")));
    public static final UUID UUID_CHARACTERISTIC_GAP_DEVICE_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A00")));
    public static final UUID UUID_CHARACTERISTIC_DEW_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7B")));
    public static final UUID UUID_CHARACTERISTIC_DIGITAL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A56")));
    public static final UUID UUID_CHARACTERISTIC_DST_OFFSET = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0D")));
    public static final UUID UUID_CHARACTERISTIC_ELEVATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6C")));
    public static final UUID UUID_CHARACTERISTIC_EMAIL_ADDRESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A87")));
    public static final UUID UUID_CHARACTERISTIC_EXACT_TIME_256 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0C")));
    public static final UUID UUID_CHARACTERISTIC_FAT_BURN_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A88")));
    public static final UUID UUID_CHARACTERISTIC_FAT_BURN_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A89")));
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A26")));
    public static final UUID UUID_CHARACTERISTIC_FIRST_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8A")));
    public static final UUID UUID_CHARACTERISTIC_FIVE_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8B")));
    public static final UUID UUID_CHARACTERISTIC_FLOOR_NUMBER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB2")));
    public static final UUID UUID_CHARACTERISTIC_GENDER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8C")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A51")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A18")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_MEASUREMENT_CONTEXT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A34")));
    public static final UUID UUID_CHARACTERISTIC_GUST_FACTOR = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A74")));
    public static final UUID UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A27")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A39")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MAX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8D")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A37")));
    public static final UUID UUID_CHARACTERISTIC_HEAT_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7A")));
    public static final UUID UUID_CHARACTERISTIC_HEIGHT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8E")));
    public static final UUID UUID_CHARACTERISTIC_HID_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4C")));
    public static final UUID UUID_CHARACTERISTIC_HID_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4A")));
    public static final UUID UUID_CHARACTERISTIC_HIP_CIRCUMFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8F")));
    public static final UUID UUID_CHARACTERISTIC_HUMIDITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6F")));
    public static final UUID UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A2A")));
    public static final UUID UUID_CHARACTERISTIC_INDOOR_POSITIONING_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAD")));
    public static final UUID UUID_CHARACTERISTIC_INTERMEDIATE_BLOOD_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A36")));
    public static final UUID UUID_CHARACTERISTIC_INTERMEDIATE_TEMPERATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1E")));
    public static final UUID UUID_CHARACTERISTIC_IRRADIANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A77")));
    public static final UUID UUID_CHARACTERISTIC_LANGUAGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA2")));
    public static final UUID UUID_CHARACTERISTIC_LAST_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A90")));
    public static final UUID UUID_CHARACTERISTIC_LATITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAE")));
    public static final UUID UUID_CHARACTERISTIC_LN_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6B")));
    public static final UUID UUID_CHARACTERISTIC_LN_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6A")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_EAST_COORDINATE_XML = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB1")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_NORTH_COORDINATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB0")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_TIME_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0F")));
    public static final UUID UUID_CHARACTERISTIC_LOCATION_AND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A67")));
    public static final UUID UUID_CHARACTERISTIC_LOCATION_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB5")));
    public static final UUID UUID_CHARACTERISTIC_LONGITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAF")));
    public static final UUID UUID_CHARACTERISTIC_MAGNETIC_DECLINATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A2C")));
    public static final UUID UUID_CHARACTERISTIC_MAGNETIC_FLUX_DENSITY_2D = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA0")));
    public static final UUID UUID_CHARACTERISTIC_MAGNETIC_FLUX_DENSITY_3D = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA1")));
    public static final UUID UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A29")));
    public static final UUID UUID_CHARACTERISTIC_MAXIMUM_RECOMMENDED_HEART_RATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A91")));
    public static final UUID UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A21")));
    public static final UUID UUID_CHARACTERISTIC_MODEL_NUMBER_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A24")));
    public static final UUID UUID_CHARACTERISTIC_NAVIGATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A68")));
    public static final UUID UUID_CHARACTERISTIC_NEW_ALERT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A46")));
    public static final UUID UUID_CHARACTERISTIC_GAP_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A04")));
    public static final UUID UUID_CHARACTERISTIC_GAP_PERIPHERAL_PRIVACY_FLAG = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A02")));
    public static final UUID UUID_CHARACTERISTIC_PLX_CONTINUOUS_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5F")));
    public static final UUID UUID_CHARACTERISTIC_PLX_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A60")));
    public static final UUID UUID_CHARACTERISTIC_PLX_SPOT_CHECK_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5E")));
    public static final UUID UUID_CHARACTERISTIC_PNP_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A50")));
    public static final UUID UUID_CHARACTERISTIC_POLLEN_CONCENTRATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A75")));
    public static final UUID UUID_CHARACTERISTIC_POSITION_QUALITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A69")));
    public static final UUID UUID_CHARACTERISTIC_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6D")));
    public static final UUID UUID_CHARACTERISTIC_PROTOCOL_MODE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4E")));
    public static final UUID UUID_CHARACTERISTIC_RAINFALL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A78")));
    public static final UUID UUID_CHARACTERISTIC_GAP_RECONNECTION_ADDRESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A03")));
    public static final UUID UUID_CHARACTERISTIC_RECORD_ACCESS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A52")));
    public static final UUID UUID_CHARACTERISTIC_REFERENCE_TIME_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A14")));
    public static final UUID UUID_CHARACTERISTIC_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4D")));
    public static final UUID UUID_CHARACTERISTIC_REPORT_MAP = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4B")));
    public static final UUID UUID_CHARACTERISTIC_RESTING_HEART_RATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A92")));
    public static final UUID UUID_CHARACTERISTIC_RINGER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A40")));
    public static final UUID UUID_CHARACTERISTIC_RINGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A41")));
    public static final UUID UUID_CHARACTERISTIC_RSC_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A54")));
    public static final UUID UUID_CHARACTERISTIC_RSC_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A53")));
    public static final UUID UUID_CHARACTERISTIC_SC_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A55")));
    public static final UUID UUID_CHARACTERISTIC_SCAN_INTERVAL_WINDOW = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4F")));
    public static final UUID UUID_CHARACTERISTIC_SCAN_REFRESH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A31")));
    public static final UUID UUID_CHARACTERISTIC_SENSOR_LOCATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5D")));
    public static final UUID UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A25")));
    public static final UUID UUID_CHARACTERISTIC_GATT_SERVICE_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A05")));
    public static final UUID UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A28")));
    public static final UUID UUID_CHARACTERISTIC_SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A93")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_NEW_ALERT_CATEGORY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A47")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_UNREAD_ALERT_CATEGORY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A48")));
    public static final UUID UUID_CHARACTERISTIC_SYSTEM_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A23")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6E")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1C")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_TYPE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1D")));
    public static final UUID UUID_CHARACTERISTIC_THREE_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A94")));
    public static final UUID UUID_CHARACTERISTIC_TIME_ACCURACY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A12")));
    public static final UUID UUID_CHARACTERISTIC_TIME_SOURCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A13")));
    public static final UUID UUID_CHARACTERISTIC_TIME_UPDATE_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A16")));
    public static final UUID UUID_CHARACTERISTIC_TIME_UPDATE_STATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A17")));
    public static final UUID UUID_CHARACTERISTIC_TIME_WITH_DST = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A11")));
    public static final UUID UUID_CHARACTERISTIC_TIME_ZONE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0E")));
    public static final UUID UUID_CHARACTERISTIC_TRUE_WIND_DIRECTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A71")));
    public static final UUID UUID_CHARACTERISTIC_TRUE_WIND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A70")));
    public static final UUID UUID_CHARACTERISTIC_TWO_ZONE_HEART_RATE_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A95")));
    public static final UUID UUID_CHARACTERISTIC_TX_POWER_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A07")));
    public static final UUID UUID_CHARACTERISTIC_UNCERTAINTY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB4")));
    public static final UUID UUID_CHARACTERISTIC_UNREAD_ALERT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A45")));
    public static final UUID UUID_CHARACTERISTIC_USER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9F")));
    public static final UUID UUID_CHARACTERISTIC_USER_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9A")));
    public static final UUID UUID_CHARACTERISTIC_UV_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A76")));
    public static final UUID UUID_CHARACTERISTIC_VO2_MAX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A96")));
    public static final UUID UUID_CHARACTERISTIC_WAIST_CIRCUMFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A97")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A98")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9D")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT_SCALE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9E")));
    public static final UUID UUID_CHARACTERISTIC_WIND_CHILL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A79")));


    //do we need this?

    private static final Map<UUID, String> GATTCHARACTERISTIC_DEBUG;

    static {
        GATTCHARACTERISTIC_DEBUG = new HashMap<>();
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_ALERT_CATEGORY_ID, "Alert AlertCategory ID");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_ALERT_CATEGORY_ID_BIT_MASK, "Alert AlertCategory ID Bit Mask");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_ALERT_LEVEL, "Alert Level");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_ALERT_NOTIFICATION_CONTROL_POINT, "Alert Notification Control Point");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_ALERT_STATUS, "Alert Status");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GAP_APPEARANCE, "Appearance");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_BLOOD_PRESSURE_FEATURE, "Blood Pressure Feature");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT, "Blood Pressure Measurement");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION, "Body Sensor Location");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_CURRENT_TIME, "Current Time");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_DATE_TIME, "Date Time");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_DAY_DATE_TIME, "Day Date Time");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_DAY_OF_WEEK, "Day of Week");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GAP_DEVICE_NAME, "Device Name");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_DST_OFFSET, "DST Offset");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_EXACT_TIME_256, "Exact Time 256");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING, "Firmware Revision String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING, "Hardware Revision String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, "Heart Rate Control Point");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST, "IEEE 11073-20601 Regulatory");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_INTERMEDIATE_BLOOD_PRESSURE, "Intermediate Cuff Pressure");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_INTERMEDIATE_TEMPERATURE, "Intermediate Temperature");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_LOCAL_TIME_INFORMATION, "Local Time Information");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL, "Measurement Interval");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_MODEL_NUMBER_STRING, "Model Number String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_NEW_ALERT, "New Alert");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GAP_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "Peripheral Preferred Connection Parameters");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GAP_PERIPHERAL_PRIVACY_FLAG, "Peripheral Privacy Flag");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GAP_RECONNECTION_ADDRESS, "Reconnection Address");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_REFERENCE_TIME_INFORMATION, "Reference Time Information");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_RINGER_CONTROL_POINT, "Ringer Control Point");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_RINGER_SETTING, "Ringer Setting");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING, "Serial Number String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_GATT_SERVICE_CHANGED, "Service Changed");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING, "Software Revision String");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_SUPPORTED_NEW_ALERT_CATEGORY, "Supported New Alert AlertCategory");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_SUPPORTED_UNREAD_ALERT_CATEGORY, "Supported Unread Alert AlertCategory");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_SYSTEM_ID, "System ID");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT, "Temperature Measurement");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TEMPERATURE_TYPE, "Temperature DeviceType");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_ACCURACY, "Time Accuracy");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_SOURCE, "Time Source");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_UPDATE_CONTROL_POINT, "Time Update Control Point");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_UPDATE_STATE, "Time Update State");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_WITH_DST, "Time with DST");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TIME_ZONE, "Time Zone");
        GATTCHARACTERISTIC_DEBUG.put(UUID_CHARACTERISTIC_TX_POWER_LEVEL, "Tx Power Level");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = GATTCHARACTERISTIC_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }

    public static String toString(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid() + " (" + lookup(characteristic.getUuid(), "unknown") + ")";
    }
}
