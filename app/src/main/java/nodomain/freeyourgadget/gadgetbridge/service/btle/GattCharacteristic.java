/*  Copyright (C) 2015-2021 Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GattCharacteristic {

    //part of the generic BLE specs see https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
    public static final byte NO_ALERT = 0x0;
    public static final byte MILD_ALERT = 0x1;
    public static final byte HIGH_ALERT = 0x2;

    // this list of GATT characteristics is complete as of 2020-01-18, retrieved from https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf
    public static final UUID UUID_CHARACTERISTIC_DEVICE_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A00")));
    public static final UUID UUID_CHARACTERISTIC_APPEARANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A01")));
    public static final UUID UUID_CHARACTERISTIC_PERIPHERAL_PRIVACY_FLAG = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A02")));
    public static final UUID UUID_CHARACTERISTIC_RECONNECTION_ADDRESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A03")));
    public static final UUID UUID_CHARACTERISTIC_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A04")));
    public static final UUID UUID_CHARACTERISTIC_SERVICE_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A05")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A06")));
    public static final UUID UUID_CHARACTERISTIC_TX_POWER_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A07")));
    public static final UUID UUID_CHARACTERISTIC_DATE_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A08")));
    public static final UUID UUID_CHARACTERISTIC_DAY_OF_WEEK = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A09")));
    public static final UUID UUID_CHARACTERISTIC_DAY_DATE_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0A")));
    public static final UUID UUID_CHARACTERISTIC_EXACT_TIME_256 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0C")));
    public static final UUID UUID_CHARACTERISTIC_DST_OFFSET = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0D")));
    public static final UUID UUID_CHARACTERISTIC_TIME_ZONE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0E")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_TIME_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A0F")));
    public static final UUID UUID_CHARACTERISTIC_TIME_WITH_DST = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A11")));
    public static final UUID UUID_CHARACTERISTIC_TIME_ACCURACY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A12")));
    public static final UUID UUID_CHARACTERISTIC_TIME_SOURCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A13")));
    public static final UUID UUID_CHARACTERISTIC_REFERENCE_TIME_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A14")));
    public static final UUID UUID_CHARACTERISTIC_TIME_UPDATE_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A16")));
    public static final UUID UUID_CHARACTERISTIC_TIME_UPDATE_STATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A17")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A18")));
    public static final UUID UUID_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A19")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1C")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_TYPE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1D")));
    public static final UUID UUID_CHARACTERISTIC_INTERMEDIATE_TEMPERATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A1E")));
    public static final UUID UUID_CHARACTERISTIC_MEASUREMENT_INTERVAL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A21")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_KEYBOARD_INPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A22")));
    public static final UUID UUID_CHARACTERISTIC_SYSTEM_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A23")));
    public static final UUID UUID_CHARACTERISTIC_MODEL_NUMBER_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A24")));
    public static final UUID UUID_CHARACTERISTIC_SERIAL_NUMBER_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A25")));
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A26")));
    public static final UUID UUID_CHARACTERISTIC_HARDWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A27")));
    public static final UUID UUID_CHARACTERISTIC_SOFTWARE_REVISION_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A28")));
    public static final UUID UUID_CHARACTERISTIC_MANUFACTURER_NAME_STRING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A29")));
    public static final UUID UUID_CHARACTERISTIC_IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A2A")));
    public static final UUID UUID_CHARACTERISTIC_CURRENT_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A2B")));
    public static final UUID UUID_CHARACTERISTIC_SCAN_REFRESH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A31")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_KEYBOARD_OUTPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A32")));
    public static final UUID UUID_CHARACTERISTIC_BOOT_MOUSE_INPUT_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A33")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_MEASUREMENT_CONTEXT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A34")));
    public static final UUID UUID_CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A35")));
    public static final UUID UUID_CHARACTERISTIC_INTERMEDIATE_CUFF_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A36")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A37")));
    public static final UUID UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A38")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A39")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A3F")));
    public static final UUID UUID_CHARACTERISTIC_RINGER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A40")));
    public static final UUID UUID_CHARACTERISTIC_RINGER_SETTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A41")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_CATEGORY_ID_BIT_MASK = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A42")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_CATEGORY_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A43")));
    public static final UUID UUID_CHARACTERISTIC_ALERT_NOTIFICATION_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A44")));
    public static final UUID UUID_CHARACTERISTIC_UNREAD_ALERT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A45")));
    public static final UUID UUID_CHARACTERISTIC_NEW_ALERT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A46")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_NEW_ALERT_CATEGORY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A47")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_UNREAD_ALERT_CATEGORY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A48")));
    public static final UUID UUID_CHARACTERISTIC_BLOOD_PRESSURE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A49")));
    public static final UUID UUID_CHARACTERISTIC_HID_INFORMATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4A")));
    public static final UUID UUID_CHARACTERISTIC_REPORT_MAP = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4B")));
    public static final UUID UUID_CHARACTERISTIC_HID_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4C")));
    public static final UUID UUID_CHARACTERISTIC_REPORT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4D")));
    public static final UUID UUID_CHARACTERISTIC_PROTOCOL_MODE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4E")));
    public static final UUID UUID_CHARACTERISTIC_SCAN_INTERVAL_WINDOW = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A4F")));
    public static final UUID UUID_CHARACTERISTIC_PNP_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A50")));
    public static final UUID UUID_CHARACTERISTIC_GLUCOSE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A51")));
    public static final UUID UUID_CHARACTERISTIC_RECORD_ACCESS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A52")));
    public static final UUID UUID_CHARACTERISTIC_RSC_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A53")));
    public static final UUID UUID_CHARACTERISTIC_RSC_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A54")));
    public static final UUID UUID_CHARACTERISTIC_SC_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A55")));
    public static final UUID UUID_CHARACTERISTIC_AGGREGATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5A")));
    public static final UUID UUID_CHARACTERISTIC_CSC_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5B")));
    public static final UUID UUID_CHARACTERISTIC_CSC_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5C")));
    public static final UUID UUID_CHARACTERISTIC_SENSOR_LOCATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5D")));
    public static final UUID UUID_CHARACTERISTIC_PLX_SPOT_CHECK_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5E")));
    public static final UUID UUID_CHARACTERISTIC_PLX_CONTINUOUS_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A5F")));
    public static final UUID UUID_CHARACTERISTIC_PLX_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A60")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A63")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_VECTOR = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A64")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A65")));
    public static final UUID UUID_CHARACTERISTIC_CYCLING_POWER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A66")));
    public static final UUID UUID_CHARACTERISTIC_LOCATION_AND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A67")));
    public static final UUID UUID_CHARACTERISTIC_NAVIGATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A68")));
    public static final UUID UUID_CHARACTERISTIC_POSITION_QUALITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A69")));
    public static final UUID UUID_CHARACTERISTIC_LN_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6A")));
    public static final UUID UUID_CHARACTERISTIC_LN_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6B")));
    public static final UUID UUID_CHARACTERISTIC_ELEVATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6C")));
    public static final UUID UUID_CHARACTERISTIC_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6D")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6E")));
    public static final UUID UUID_CHARACTERISTIC_HUMIDITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A6F")));
    public static final UUID UUID_CHARACTERISTIC_TRUE_WIND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A70")));
    public static final UUID UUID_CHARACTERISTIC_TRUE_WIND_DIRECTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A71")));
    public static final UUID UUID_CHARACTERISTIC_APPARENT_WIND_SPEED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A72")));
    public static final UUID UUID_CHARACTERISTIC_APPARENT_WIND_DIRECTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A73")));
    public static final UUID UUID_CHARACTERISTIC_GUST_FACTOR = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A74")));
    public static final UUID UUID_CHARACTERISTIC_POLLEN_CONCENTRATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A75")));
    public static final UUID UUID_CHARACTERISTIC_UV_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A76")));
    public static final UUID UUID_CHARACTERISTIC_IRRADIANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A77")));
    public static final UUID UUID_CHARACTERISTIC_RAINFALL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A78")));
    public static final UUID UUID_CHARACTERISTIC_WIND_CHILL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A79")));
    public static final UUID UUID_CHARACTERISTIC_HEAT_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7A")));
    public static final UUID UUID_CHARACTERISTIC_DEW_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7B")));
    public static final UUID UUID_CHARACTERISTIC_DESCRIPTOR_VALUE_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7D")));
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7E")));
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_THRESHOLD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A7F")));
    public static final UUID UUID_CHARACTERISTIC_AGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A80")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A81")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A82")));
    public static final UUID UUID_CHARACTERISTIC_ANAEROBIC_THRESHOLD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A83")));
    public static final UUID UUID_CHARACTERISTIC_AEROBIC_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A84")));
    public static final UUID UUID_CHARACTERISTIC_DATE_OF_BIRTH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A85")));
    public static final UUID UUID_CHARACTERISTIC_DATE_OF_THRESHOLD_ASSESSMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A86")));
    public static final UUID UUID_CHARACTERISTIC_EMAIL_ADDRESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A87")));
    public static final UUID UUID_CHARACTERISTIC_FAT_BURN_HEART_RATE_LOWER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A88")));
    public static final UUID UUID_CHARACTERISTIC_FAT_BURN_HEART_RATE_UPPER_LIMIT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A89")));
    public static final UUID UUID_CHARACTERISTIC_FIRST_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8A")));
    public static final UUID UUID_CHARACTERISTIC_FIVE_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8B")));
    public static final UUID UUID_CHARACTERISTIC_GENDER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8C")));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MAX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8D")));
    public static final UUID UUID_CHARACTERISTIC_HEIGHT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8E")));
    public static final UUID UUID_CHARACTERISTIC_HIP_CIRCUMFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A8F")));
    public static final UUID UUID_CHARACTERISTIC_LAST_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A90")));
    public static final UUID UUID_CHARACTERISTIC_MAXIMUM_RECOMMENDED_HEART_RATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A91")));
    public static final UUID UUID_CHARACTERISTIC_RESTING_HEART_RATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A92")));
    public static final UUID UUID_CHARACTERISTIC_SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A93")));
    public static final UUID UUID_CHARACTERISTIC_THREE_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A94")));
    public static final UUID UUID_CHARACTERISTIC_TWO_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A95")));
    public static final UUID UUID_CHARACTERISTIC_VO2_MAX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A96")));
    public static final UUID UUID_CHARACTERISTIC_WAIST_CIRCUMFERENCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A97")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A98")));
    public static final UUID UUID_CHARACTERISTIC_DATABASE_CHANGE_INCREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A99")));
    public static final UUID UUID_CHARACTERISTIC_USER_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9A")));
    public static final UUID UUID_CHARACTERISTIC_BODY_COMPOSITION_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9B")));
    public static final UUID UUID_CHARACTERISTIC_BODY_COMPOSITION_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9C")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9D")));
    public static final UUID UUID_CHARACTERISTIC_WEIGHT_SCALE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9E")));
    public static final UUID UUID_CHARACTERISTIC_USER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2A9F")));
    public static final UUID UUID_CHARACTERISTIC_MAGNETIC_FLUX_DENSITY___2D = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA0")));
    public static final UUID UUID_CHARACTERISTIC_MAGNETIC_FLUX_DENSITY___3D = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA1")));
    public static final UUID UUID_CHARACTERISTIC_LANGUAGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA2")));
    public static final UUID UUID_CHARACTERISTIC_BAROMETRIC_PRESSURE_TREND = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA3")));
    public static final UUID UUID_CHARACTERISTIC_BOND_MANAGEMENT_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA4")));
    public static final UUID UUID_CHARACTERISTIC_BOND_MANAGEMENT_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA5")));
    public static final UUID UUID_CHARACTERISTIC_CENTRAL_ADDRESS_RESOLUTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA6")));
    public static final UUID UUID_CHARACTERISTIC_CGM_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA7")));
    public static final UUID UUID_CHARACTERISTIC_CGM_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA8")));
    public static final UUID UUID_CHARACTERISTIC_CGM_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AA9")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SESSION_START_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAA")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SESSION_RUN_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAB")));
    public static final UUID UUID_CHARACTERISTIC_CGM_SPECIFIC_OPS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAC")));
    public static final UUID UUID_CHARACTERISTIC_INDOOR_POSITIONING_CONFIGURATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAD")));
    public static final UUID UUID_CHARACTERISTIC_LATITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAE")));
    public static final UUID UUID_CHARACTERISTIC_LONGITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AAF")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_NORTH_COORDINATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB0")));
    public static final UUID UUID_CHARACTERISTIC_LOCAL_EAST_COORDINATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB1")));
    public static final UUID UUID_CHARACTERISTIC_FLOOR_NUMBER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB2")));
    public static final UUID UUID_CHARACTERISTIC_ALTITUDE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB3")));
    public static final UUID UUID_CHARACTERISTIC_UNCERTAINTY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB4")));
    public static final UUID UUID_CHARACTERISTIC_LOCATION_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB5")));
    public static final UUID UUID_CHARACTERISTIC_URI = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB6")));
    public static final UUID UUID_CHARACTERISTIC_HTTP_HEADERS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB7")));
    public static final UUID UUID_CHARACTERISTIC_HTTP_STATUS_CODE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB8")));
    public static final UUID UUID_CHARACTERISTIC_HTTP_ENTITY_BODY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AB9")));
    public static final UUID UUID_CHARACTERISTIC_HTTP_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABA")));
    public static final UUID UUID_CHARACTERISTIC_HTTPS_SECURITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABB")));
    public static final UUID UUID_CHARACTERISTIC_TDS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABC")));
    public static final UUID UUID_CHARACTERISTIC_OTS_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABD")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABE")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_TYPE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ABF")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_SIZE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC0")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_FIRST_CREATED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC1")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_LAST_MODIFIED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC2")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC3")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_PROPERTIES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC4")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_ACTIONCONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC5")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_LIST_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC6")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_LIST_FILTER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC7")));
    public static final UUID UUID_CHARACTERISTIC_OBJECT_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC8")));
    public static final UUID UUID_CHARACTERISTIC_RESOLVABLE_PRIVATE_ADDRESS_ONLY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AC9")));
    public static final UUID UUID_CHARACTERISTIC_UNSPECIFIED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACA")));
    public static final UUID UUID_CHARACTERISTIC_DIRECTORY_LISTING = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACB")));
    public static final UUID UUID_CHARACTERISTIC_FITNESS_MACHINE_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACC")));
    public static final UUID UUID_CHARACTERISTIC_TREADMILL_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACD")));
    public static final UUID UUID_CHARACTERISTIC_CROSS_TRAINER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACE")));
    public static final UUID UUID_CHARACTERISTIC_STEP_CLIMBER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ACF")));
    public static final UUID UUID_CHARACTERISTIC_STAIR_CLIMBER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD0")));
    public static final UUID UUID_CHARACTERISTIC_ROWER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD1")));
    public static final UUID UUID_CHARACTERISTIC_INDOOR_BIKE_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD2")));
    public static final UUID UUID_CHARACTERISTIC_TRAINING_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD3")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_SPEED_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD4")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_INCLINATION_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD5")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_RESISTANCE_LEVEL_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD6")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_HEART_RATE_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD7")));
    public static final UUID UUID_CHARACTERISTIC_SUPPORTED_POWER_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD8")));
    public static final UUID UUID_CHARACTERISTIC_FITNESS_MACHINE_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AD9")));
    public static final UUID UUID_CHARACTERISTIC_FITNESS_MACHINE_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ADA")));
    public static final UUID UUID_CHARACTERISTIC_MESH_PROVISIONING_DATA_IN = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ADB")));
    public static final UUID UUID_CHARACTERISTIC_MESH_PROVISIONING_DATA_OUT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ADC")));
    public static final UUID UUID_CHARACTERISTIC_MESH_PROXY_DATA_IN = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ADD")));
    public static final UUID UUID_CHARACTERISTIC_MESH_PROXY_DATA_OUT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2ADE")));
    public static final UUID UUID_CHARACTERISTIC_AVERAGE_CURRENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE0")));
    public static final UUID UUID_CHARACTERISTIC_AVERAGE_VOLTAGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE1")));
    public static final UUID UUID_CHARACTERISTIC_BOOLEAN = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE2")));
    public static final UUID UUID_CHARACTERISTIC_CHROMATIC_DISTANCE_FROM_PLANCKIAN = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE3")));
    public static final UUID UUID_CHARACTERISTIC_CHROMATICITY_COORDINATES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE4")));
    public static final UUID UUID_CHARACTERISTIC_CHROMATICITY_IN_CCT_AND_DUV_VALUES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE5")));
    public static final UUID UUID_CHARACTERISTIC_CHROMATICITY_TOLERANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE6")));
    public static final UUID UUID_CHARACTERISTIC_CIE_13_3_1995_COLOR_RENDERING_INDEX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE7")));
    public static final UUID UUID_CHARACTERISTIC_COEFFICIENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE8")));
    public static final UUID UUID_CHARACTERISTIC_CORRELATED_COLOR_TEMPERATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AE9")));
    public static final UUID UUID_CHARACTERISTIC_COUNT_16 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AEA")));
    public static final UUID UUID_CHARACTERISTIC_COUNT_24 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AEB")));
    public static final UUID UUID_CHARACTERISTIC_COUNTRY_CODE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AEC")));
    public static final UUID UUID_CHARACTERISTIC_DATE_UTC = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AED")));
    public static final UUID UUID_CHARACTERISTIC_ELECTRIC_CURRENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AEE")));
    public static final UUID UUID_CHARACTERISTIC_ELECTRIC_CURRENT_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AEF")));
    public static final UUID UUID_CHARACTERISTIC_ELECTRIC_CURRENT_SPECIFICATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF0")));
    public static final UUID UUID_CHARACTERISTIC_ELECTRIC_CURRENT_STATISTICS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF1")));
    public static final UUID UUID_CHARACTERISTIC_ENERGY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF2")));
    public static final UUID UUID_CHARACTERISTIC_ENERGY_IN_A_PERIOD_OF_DAY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF3")));
    public static final UUID UUID_CHARACTERISTIC_EVENT_STATISTICS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF4")));
    public static final UUID UUID_CHARACTERISTIC_FIXED_STRING_16 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF5")));
    public static final UUID UUID_CHARACTERISTIC_FIXED_STRING_24 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF6")));
    public static final UUID UUID_CHARACTERISTIC_FIXED_STRING_36 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF7")));
    public static final UUID UUID_CHARACTERISTIC_FIXED_STRING_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF8")));
    public static final UUID UUID_CHARACTERISTIC_GENERIC_LEVEL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AF9")));
    public static final UUID UUID_CHARACTERISTIC_GLOBAL_TRADE_ITEM_NUMBER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFA")));
    public static final UUID UUID_CHARACTERISTIC_ILLUMINANCE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFB")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_EFFICACY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFC")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_ENERGY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFD")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_EXPOSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFE")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_FLUX = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2AFF")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_FLUX_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B00")));
    public static final UUID UUID_CHARACTERISTIC_LUMINOUS_INTENSITY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B01")));
    public static final UUID UUID_CHARACTERISTIC_MASS_FLOW = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B02")));
    public static final UUID UUID_CHARACTERISTIC_PERCEIVED_LIGHTNESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B03")));
    public static final UUID UUID_CHARACTERISTIC_PERCENTAGE_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B04")));
    public static final UUID UUID_CHARACTERISTIC_POWER = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B05")));
    public static final UUID UUID_CHARACTERISTIC_POWER_SPECIFICATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B06")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_RUNTIME_IN_A_CURRENT_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B07")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_RUNTIME_IN_A_GENERIC_LEVEL_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B08")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_VALUE_IN_A_VOLTAGE_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B09")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_VALUE_IN_AN_ILLUMINANCE_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0A")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_VALUE_IN_A_PERIOD_OF_DAY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0B")));
    public static final UUID UUID_CHARACTERISTIC_RELATIVE_VALUE_IN_A_TEMPERATURE_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0C")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0D")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_8_IN_A_PERIOD_OF_DAY = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0E")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_8_STATISTICS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B0F")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_RANGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B10")));
    public static final UUID UUID_CHARACTERISTIC_TEMPERATURE_STATISTICS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B11")));
    public static final UUID UUID_CHARACTERISTIC_TIME_DECIHOUR_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B12")));
    public static final UUID UUID_CHARACTERISTIC_TIME_EXPONENTIAL_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B13")));
    public static final UUID UUID_CHARACTERISTIC_TIME_HOUR_24 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B14")));
    public static final UUID UUID_CHARACTERISTIC_TIME_MILLISECOND_24 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B15")));
    public static final UUID UUID_CHARACTERISTIC_TIME_SECOND_16 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B16")));
    public static final UUID UUID_CHARACTERISTIC_TIME_SECOND_8 = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B17")));
    public static final UUID UUID_CHARACTERISTIC_VOLTAGE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B18")));
    public static final UUID UUID_CHARACTERISTIC_VOLTAGE_SPECIFICATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B19")));
    public static final UUID UUID_CHARACTERISTIC_VOLTAGE_STATISTICS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1A")));
    public static final UUID UUID_CHARACTERISTIC_VOLUME_FLOW = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1B")));
    public static final UUID UUID_CHARACTERISTIC_CHROMATICITY_COORDINATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1C")));
    public static final UUID UUID_CHARACTERISTIC_RC_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1D")));
    public static final UUID UUID_CHARACTERISTIC_RC_SETTINGS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1E")));
    public static final UUID UUID_CHARACTERISTIC_RECONNECTION_CONFIGURATION_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B1F")));
    public static final UUID UUID_CHARACTERISTIC_IDD_STATUS_CHANGED = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B20")));
    public static final UUID UUID_CHARACTERISTIC_IDD_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B21")));
    public static final UUID UUID_CHARACTERISTIC_IDD_ANNUNCIATION_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B22")));
    public static final UUID UUID_CHARACTERISTIC_IDD_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B23")));
    public static final UUID UUID_CHARACTERISTIC_IDD_STATUS_READER_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B24")));
    public static final UUID UUID_CHARACTERISTIC_IDD_COMMAND_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B25")));
    public static final UUID UUID_CHARACTERISTIC_IDD_COMMAND_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B26")));
    public static final UUID UUID_CHARACTERISTIC_IDD_RECORD_ACCESS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B27")));
    public static final UUID UUID_CHARACTERISTIC_IDD_HISTORY_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B28")));
    public static final UUID UUID_CHARACTERISTIC_CLIENT_SUPPORTED_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B29")));
    public static final UUID UUID_CHARACTERISTIC_DATABASE_HASH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B2A")));
    public static final UUID UUID_CHARACTERISTIC_BSS_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B2B")));
    public static final UUID UUID_CHARACTERISTIC_BSS_RESPONSE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B2C")));
    public static final UUID UUID_CHARACTERISTIC_EMERGENCY_ID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B2D")));
    public static final UUID UUID_CHARACTERISTIC_EMERGENCY_TEXT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B2E")));
    public static final UUID UUID_CHARACTERISTIC_ENHANCED_BLOOD_PRESSURE_MEASUREMENT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B34")));
    public static final UUID UUID_CHARACTERISTIC_ENHANCED_INTERMEDIATE_CUFF_PRESSURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B35")));
    public static final UUID UUID_CHARACTERISTIC_BLOOD_PRESSURE_RECORD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B36")));
    public static final UUID UUID_CHARACTERISTIC_BR_EDR_HANDOVER_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B38")));
    public static final UUID UUID_CHARACTERISTIC_BLUETOOTH_SIG_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B39")));
    public static final UUID UUID_CHARACTERISTIC_SERVER_SUPPORTED_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3A")));
    public static final UUID UUID_CHARACTERISTIC_PHYSICAL_ACTIVITY_MONITOR_FEATURES = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3B")));
    public static final UUID UUID_CHARACTERISTIC_GENERAL_ACTIVITY_INSTANTANEOUS_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3C")));
    public static final UUID UUID_CHARACTERISTIC_GENERAL_ACTIVITY_SUMMARY_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3D")));
    public static final UUID UUID_CHARACTERISTIC_CARDIORESPIRATORY_ACTIVITY_INSTANTANEOUS_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3E")));
    public static final UUID UUID_CHARACTERISTIC_CARDIORESPIRATORY_ACTIVITY_SUMMARY_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B3F")));
    public static final UUID UUID_CHARACTERISTIC_STEP_COUNTER_ACTIVITY_SUMMARY_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B40")));
    public static final UUID UUID_CHARACTERISTIC_SLEEP_ACTIVITY_INSTANTANEOUS_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B41")));
    public static final UUID UUID_CHARACTERISTIC_SLEEP_ACTIVITY_SUMMARY_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B42")));
    public static final UUID UUID_CHARACTERISTIC_PHYSICAL_ACTIVITY_MONITOR_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B43")));
    public static final UUID UUID_CHARACTERISTIC_CURRENT_SESSION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B44")));
    public static final UUID UUID_CHARACTERISTIC_SESSION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B45")));
    public static final UUID UUID_CHARACTERISTIC_PREFERRED_UNITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B46")));
    public static final UUID UUID_CHARACTERISTIC_HIGH_RESOLUTION_HEIGHT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B47")));
    public static final UUID UUID_CHARACTERISTIC_MIDDLE_NAME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B48")));
    public static final UUID UUID_CHARACTERISTIC_STRIDE_LENGTH = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B49")));
    public static final UUID UUID_CHARACTERISTIC_HANDEDNESS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4A")));
    public static final UUID UUID_CHARACTERISTIC_DEVICE_WEARING_POSITION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4B")));
    public static final UUID UUID_CHARACTERISTIC_FOUR_ZONE_HEART_RATE_LIMITS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4C")));
    public static final UUID UUID_CHARACTERISTIC_HIGH_INTENSITY_EXERCISE_THRESHOLD = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4D")));
    public static final UUID UUID_CHARACTERISTIC_ACTIVITY_GOAL = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4E")));
    public static final UUID UUID_CHARACTERISTIC_SEDENTARY_INTERVAL_NOTIFICATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B4F")));
    public static final UUID UUID_CHARACTERISTIC_CALORIC_INTAKE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B50")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_INPUT_STATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B77")));
    public static final UUID UUID_CHARACTERISTIC_GAIN_SETTINGS_ATTRIBUTE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B78")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_INPUT_TYPE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B79")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_INPUT_STATUS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7A")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_INPUT_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7B")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_INPUT_DESCRIPTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7C")));
    public static final UUID UUID_CHARACTERISTIC_VOLUME_STATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7D")));
    public static final UUID UUID_CHARACTERISTIC_VOLUME_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7E")));
    public static final UUID UUID_CHARACTERISTIC_VOLUME_FLAGS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B7F")));
    public static final UUID UUID_CHARACTERISTIC_OFFSET_STATE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B80")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_LOCATION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B81")));
    public static final UUID UUID_CHARACTERISTIC_VOLUME_OFFSET_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B82")));
    public static final UUID UUID_CHARACTERISTIC_AUDIO_OUTPUT_DESCRIPTION = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B83")));
    public static final UUID UUID_CHARACTERISTIC_DEVICE_TIME_FEATURE = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B8E")));
    public static final UUID UUID_CHARACTERISTIC_DEVICE_TIME_PARAMETERS = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B8F")));
    public static final UUID UUID_CHARACTERISTIC_DEVICE_TIME = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B90")));
    public static final UUID UUID_CHARACTERISTIC_DEVICE_TIME_CONTROL_POINT = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B91")));
    public static final UUID UUID_CHARACTERISTIC_TIME_CHANGE_LOG_DATA = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "2B92")));

    private static Map<UUID, String> GATTCHARACTERISTIC_DEBUG;

    public static synchronized String lookup(UUID uuid, String fallback) {
        if (GATTCHARACTERISTIC_DEBUG == null) {
            GATTCHARACTERISTIC_DEBUG = initDebugMap();
        }
        String name = GATTCHARACTERISTIC_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }

    private static Map<UUID, String> initDebugMap() {
        Map<UUID,String> map = new HashMap<>();

        try {
            for (Field field : GattCharacteristic.class.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0 && field.getType() == UUID.class) {
                    UUID uuid = (UUID) field.get(null);
                    if (uuid != null) {
                        map.put(uuid, toPrettyName(field.getName()));
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(GattCharacteristic.class.getName(), "Error reading UUID fields by reflection: " + ex.getMessage(), ex);
        }
        return map;
    }

    private static String toPrettyName(String fieldName) {
        String[] words = fieldName.split("_");
        if (words.length <= 1) {
            return fieldName.toLowerCase();
        }
        StringBuilder builder = new StringBuilder(fieldName.length());
        for (String word : words) {
            if (word.length() == 0 || "UUID".equals(word) || "CHARACTERISTIC".equals(word)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(word.toLowerCase());
        }
        return builder.toString();
    }

    public static String toString(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid() + " (" + lookup(characteristic.getUuid(), "unknown") + ")";
    }
}
