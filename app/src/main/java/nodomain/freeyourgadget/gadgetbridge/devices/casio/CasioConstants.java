/*  Copyright (C) 2018-2021 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.devices.casio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CasioConstants {
    public static final UUID CASIO_VIRTUAL_SERVER_SERVICE = UUID.fromString("26eb0007-b012-49a8-b1f8-394fb2032b0f");

    public static final UUID CASIO_VIRTUAL_SERVER_FEATURES = UUID.fromString("26eb0008-b012-49a8-b1f8-394fb2032b0f");

    public static final UUID CASIO_A_NOT_W_REQ_NOT = UUID.fromString( "26eb0009-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_A_NOT_COM_SET_NOT = UUID.fromString( "26eb000a-b012-49a8-b1f8-394fb2032b0f");

    public static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Immediate Alert

    public static final UUID IMMEDIATE_ALERT_SERVICE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");

    // Alert

    public static final UUID ALERT_SERVICE_UUID = UUID.fromString("26eb0000-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID ALERT_CHARACTERISTIC_UUID = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERT_NOTIFICATION_CONTROL_POINT = UUID.fromString("00002a44-0000-1000-8000-00805f9b34fb");

    // More Alert

    public static final UUID MORE_ALERT_SERVICE_UUID = UUID.fromString("26eb001a-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID MORE_ALERT_UUID = UUID.fromString("26eb001b-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID MORE_ALERT_FOR_LONG_UUID = UUID.fromString("26eb001c-b012-49a8-b1f8-394fb2032b0f");

    // Phone Alert
    public static final UUID CASIO_PHONE_ALERT_STATUS_SERVICE = UUID.fromString("26eb0001-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID RINGER_CONTROL_POINT = UUID.fromString("00002a40-0000-1000-8000-00805f9b34fb");

    // Phone Finder

    public static final UUID CASIO_IMMEDIATE_ALERT_SERVICE_UUID = UUID.fromString("26eb0005-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID ALERT_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    // Current Time

    public static final UUID CURRENT_TIME_SERVICE_UUID = UUID.fromString("26eb0002-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    public static final UUID LOCAL_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");

    // Control Mode
    public static final UUID WATCH_FEATURES_SERVICE_UUID = UUID.fromString("26eb000d-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID WATCH_CTRL_SERVICE_UUID = UUID.fromString("26eb0018-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID KEY_CONTAINER_CHARACTERISTIC_UUID = UUID.fromString("26eb0019-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID NAME_OF_APP_CHARACTERISTIC_UUID = UUID.fromString("26eb001d-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID FUNCTION_SWITCH_CHARACTERISTIC = UUID.fromString("26eb001e-b012-49a8-b1f8-394fb2032b0f");
    public static final String MUSIC_MESSAGE = "Music";

    // Modern Watches - All Features
    public static final UUID CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID = UUID.fromString("26eb002c-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_ALL_FEATURES_CHARACTERISTIC_UUID = UUID.fromString("26eb002d-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID = UUID.fromString("26eb0023-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_CONVOY_CHARACTERISTIC_UUID = UUID.fromString("26eb0024-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_NOTIFICATION_CHARACTERISTIC_UUID = UUID.fromString("26eb0030-b012-49a8-b1f8-394fb2032b0f");

    // Link Loss

    public static final UUID LINK_LOSS_SERVICE = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

    // TxPower

    public static final UUID TX_POWER_SERVICE_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    // Settings
    public static final UUID CASIO_SETTING_FOR_BLE_CHARACTERISTIC_UUID = UUID.fromString("26eb000f-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID = UUID.fromString("26eb0013-b012-49a8-b1f8-394fb2032b0f");

    // Notification Types - GB6900

    public static final byte CALL_NOTIFICATION_ID = 3;
    public static final byte MAIL_NOTIFICATION_ID = 1;
    public static final byte CALENDAR_NOTIFICATION_ID = 7;
    public static final byte SNS_NOTIFICATION_ID = 13;
    public static final byte SMS_NOTIFICATION_ID = 5;

    // Notification Types - GBX100
    public static final byte CATEGORY_ADVERTISEMENT = 13;
    public static final byte CATEGORY_BUSINESS = 9;
    public static final byte CATEGORY_CONDITION = 12;
    public static final byte CATEGORY_EMAIL = 6;
    public static final byte CATEGORY_ENTERTAINMENT = 11;
    public static final byte CATEGORY_HEALTH_AND_FITNESS = 8;
    public static final byte CATEGORY_INCOMING_CALL = 1;
    public static final byte CATEGORY_LOCATION = 10;
    public static final byte CATEGORY_MISSED_CALL = 2;
    public static final byte CATEGORY_NEWS = 7;
    public static final byte CATEGORY_OTHER = 0;
    public static final byte CATEGORY_SCHEDULE_AND_ALARM = 5;
    public static final byte CATEGORY_SNS = 4;
    public static final byte CATEGORY_VOICEMAIL = 3;

    public enum Model {
        MODEL_CASIO_GENERIC,
        MODEL_CASIO_6900B,
        MODEL_CASIO_5600B,
        MODEL_CASIO_GBX100
    }

    public enum ConfigurationOption {
        OPTION_GENDER,
        OPTION_WEIGHT,
        OPTION_HEIGHT,
        OPTION_WRIST,
        OPTION_BIRTHDAY,
        OPTION_STEP_GOAL,
        OPTION_DISTANCE_GOAL,
        OPTION_ACTIVITY_GOAL,
        OPTION_AUTOLIGHT,
        OPTION_TIMEFORMAT,
        OPTION_KEY_VIBRATION,
        OPTION_OPERATING_SOUNDS,
        OPTION_ALL
    }

    public static final int CASIO_CONVOY_DATATYPE_STEPS = 0x04;
    public static final int CASIO_CONVOY_DATATYPE_CALORIES = 0x05;

    public static final int CASIO_FAKE_RING_SLEEP_DURATION = 3000;
    public static final int CASIO_FAKE_RING_RETRIES = 10;

    public static final int CASIO_AUTOREMOVE_MESSAGE_DELAY = 10000;

    public static Map<String, Byte> characteristicToByte = new HashMap<String, Byte>() {
        {
            put("CASIO_WATCH_NAME", (byte) 0x23);
            put("CASIO_APP_INFORMATION", (byte) 0x22);
            put("CASIO_BLE_FEATURES", (byte) 0x10);
            put("CASIO_SETTING_FOR_BLE", (byte) 0x11);
            put("CASIO_ADVERTISE_PARAMETER_MANAGER", (byte) 0x3b);
            put("CASIO_CONNECTION_PARAMETER_MANAGER", (byte) 0x3a);
            put("CASIO_MODULE_ID", (byte) 0x26);
            put("CASIO_WATCH_CONDITION", (byte) 0x28);
            put("CASIO_VERSION_INFORMATION", (byte) 0x20);
            put("CASIO_DST_WATCH_STATE", (byte) 0x1d);
            put("CASIO_DST_SETTING", (byte) 0x1e);
            put("CASIO_SERVICE_DISCOVERY_MANAGER", (byte) 0x47);
            put("CASIO_CURRENT_TIME", (byte) 0x09);
            put("CASIO_SETTING_FOR_USER_PROFILE", (byte) 0x45);
            put("CASIO_SETTING_FOR_TARGET_VALUE", (byte) 0x43);
            put("ALERT_LEVEL", (byte) 0x0a);
            put("CASIO_SETTING_FOR_ALM", (byte) 0x15);
            put("CASIO_SETTING_FOR_ALM2", (byte) 0x16);
            put("CASIO_SETTING_FOR_BASIC", (byte) 0x13);
            put("CASIO_CURRENT_TIME_MANAGER", (byte) 0x39);
        }
    };
}
