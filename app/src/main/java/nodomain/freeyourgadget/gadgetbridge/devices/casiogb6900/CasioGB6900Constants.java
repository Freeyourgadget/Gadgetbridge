/*      Copyright (C) 2018-2019 Andreas Böhler
        based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.devices.casiogb6900;

import java.util.UUID;

public final class CasioGB6900Constants {
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

    // Link Loss

    public static final UUID LINK_LOSS_SERVICE = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

    // TxPower

    public static final UUID TX_POWER_SERVICE_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    // Settings
    public static final UUID CASIO_SETTING_FOR_BLE_CHARACTERISTIC_UUID = UUID.fromString("26eb000f-b012-49a8-b1f8-394fb2032b0f");
    public static final UUID CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID = UUID.fromString("26eb0013-b012-49a8-b1f8-394fb2032b0f");

    // Notification Types

    public static final byte CALL_NOTIFICATION_ID = 3;
    public static final byte MAIL_NOTIFICATION_ID = 1;
    public static final byte CALENDAR_NOTIFICATION_ID = 7;
    public static final byte SNS_NOTIFICATION_ID = 13;
    public static final byte SMS_NOTIFICATION_ID = 5;

    public enum Model {
        MODEL_CASIO_GENERIC,
        MODEL_CASIO_6900B,
        MODEL_CASIO_5600B
    }
}
